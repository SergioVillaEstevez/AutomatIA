package com.neiverlabs.ai.service;

import com.neiverlabs.ai.domain.AiModelPricing;
import com.neiverlabs.ai.domain.AiProviderConfiguration;
import com.neiverlabs.ai.domain.AiUsage;
import com.neiverlabs.ai.domain.AiUsageStatus;
import com.neiverlabs.ai.provider.AiCompletionRequest;
import com.neiverlabs.ai.provider.AiCompletionResult;
import com.neiverlabs.ai.provider.AiProvider;
import com.neiverlabs.ai.provider.AiProviderException;
import com.neiverlabs.ai.repo.AiModelPricingRepository;
import com.neiverlabs.ai.repo.AiProviderConfigurationRepository;
import com.neiverlabs.ai.repo.AiUsageRepository;
import com.neiverlabs.ai.web.AiCompletionResponse;
import com.neiverlabs.ai.web.AiUsageResponse;
import com.neiverlabs.ai.web.AiUsageSummaryResponse;
import com.neiverlabs.ai.web.CompleteAiRequest;
import com.neiverlabs.company.domain.Company;
import com.neiverlabs.company.repo.CompanyRepository;
import com.neiverlabs.config.AiProperties;
import com.neiverlabs.identity.security.AccessGuard;
import com.neiverlabs.identity.security.NeiverPrincipal;
import com.neiverlabs.shared.error.ApiException;
import com.neiverlabs.shared.error.ErrorCode;
import com.neiverlabs.shared.web.PageResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiService {

  private final Map<String, AiProvider> providers;
  private final AiProviderConfigurationRepository configurations;
  private final AiModelPricingRepository pricing;
  private final AiUsageRepository usage;
  private final CompanyRepository companies;
  private final AiCostCalculator costs;
  private final AiProperties properties;
  private final Clock clock;

  public AiService(
      List<AiProvider> providers,
      AiProviderConfigurationRepository configurations,
      AiModelPricingRepository pricing,
      AiUsageRepository usage,
      CompanyRepository companies,
      AiCostCalculator costs,
      AiProperties properties,
      Clock clock
  ) {
    this.providers = new HashMap<>();
    for (AiProvider provider : providers) {
      this.providers.put(provider.code(), provider);
    }
    this.configurations = configurations;
    this.pricing = pricing;
    this.usage = usage;
    this.companies = companies;
    this.costs = costs;
    this.properties = properties;
    this.clock = clock;
  }

  @Transactional
  public AiCompletionResponse complete(CompleteAiRequest request) {
    NeiverPrincipal principal = AccessGuard.current();
    UUID companyId = AccessGuard.requireTenant();
    Company company = companies.findById(companyId)
        .orElseThrow(() -> ApiException.notFound("Empresa no encontrada."));

    String taskType = blankToDefault(request.taskType(), "DEFAULT");
    AiProviderConfiguration config = configurations.resolve(companyId, taskType)
        .orElseThrow(() -> ApiException.badRequest(
            "No hay configuración de IA para esta empresa. El administrador general debe definir el proveedor."));

    Instant now = clock.instant();
    AiModelPricing price = pricing.findCurrent(config.getProviderCode(), config.getModelCode(), now)
        .orElseThrow(() -> new ApiException(
            HttpStatus.CONFLICT,
            ErrorCode.AI_PRICING_MISSING,
            "Precio no configurado",
            "No hay tarifa vigente para " + config.getProviderCode() + "/" + config.getModelCode()
                + ". Actualiza ai_model_pricing antes de usarlo."));

    // Budget first: a zero/exhausted limit must block even when OPENAI_API_KEY is missing.
    YearMonth month = YearMonth.from(LocalDate.ofInstant(now, ZoneOffset.UTC));
    BigDecimal spent = monthlySpend(companyId, month);
    if (properties.isBudgetEnforced()
        && company.getMonthlyAiBudgetEur() != null
        && spent.compareTo(company.getMonthlyAiBudgetEur()) >= 0) {
      usage.save(AiUsage.blocked(
          companyId,
          principal.userId(),
          config.getProviderCode(),
          config.getModelCode(),
          taskType,
          ErrorCode.AI_BUDGET_EXCEEDED.name(),
          "La empresa ha agotado su límite mensual de IA."));
      throw new ApiException(
          HttpStatus.PAYMENT_REQUIRED,
          ErrorCode.AI_BUDGET_EXCEEDED,
          "Límite de IA agotado",
          "Esta empresa ha alcanzado su límite mensual de IA ("
              + company.getMonthlyAiBudgetEur() + " €).");
    }

    AiProvider provider = providers.get(config.getProviderCode());
    if (provider == null) {
      throw ApiException.badRequest("Proveedor de IA no soportado: " + config.getProviderCode());
    }
    if (!provider.available()) {
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          ErrorCode.AI_NOT_CONFIGURED,
          "IA no configurada",
          "Falta la clave del proveedor en el servidor. Define OPENAI_API_KEY.");
    }

    try {
      AiCompletionResult result = provider.complete(new AiCompletionRequest(
          config.getModelCode(),
          List.of(new AiCompletionRequest.Message("user", request.prompt().trim())),
          request.maxOutputTokens()));
      AiCostCalculator.Cost cost = costs.estimate(price, result.inputTokens(), result.outputTokens());
      if (properties.isBudgetEnforced()
          && company.getMonthlyAiBudgetEur() != null
          && spent.add(cost.eur()).compareTo(company.getMonthlyAiBudgetEur()) > 0) {
        usage.save(AiUsage.blocked(
            companyId,
            principal.userId(),
            result.providerCode(),
            result.model(),
            taskType,
            ErrorCode.AI_BUDGET_EXCEEDED.name(),
            "La petición superaría el límite mensual de IA."));
        throw new ApiException(
            HttpStatus.PAYMENT_REQUIRED,
            ErrorCode.AI_BUDGET_EXCEEDED,
            "Límite de IA agotado",
            "Esta petición superaría el límite mensual de IA de la empresa.");
      }
      AiUsage saved = usage.save(AiUsage.success(
          companyId,
          principal.userId(),
          result.providerCode(),
          result.model(),
          taskType,
          result.inputTokens(),
          result.outputTokens(),
          cost.usd(),
          cost.eur()));
      return new AiCompletionResponse(
          result.content(),
          result.providerCode(),
          result.model(),
          result.inputTokens(),
          result.outputTokens(),
          cost.usd(),
          cost.eur(),
          saved.getId());
    } catch (AiProviderException ex) {
      usage.save(AiUsage.failed(
          companyId,
          principal.userId(),
          config.getProviderCode(),
          config.getModelCode(),
          taskType,
          ex.getErrorCode(),
          ex.getMessage()));
      throw new ApiException(
          HttpStatus.BAD_GATEWAY,
          ErrorCode.AI_PROVIDER_ERROR,
          "Error del proveedor de IA",
          ex.getMessage());
    }
  }

  @Transactional(readOnly = true)
  public PageResponse<AiUsageResponse> companyUsage(Pageable pageable) {
    UUID companyId = AccessGuard.requireTenant();
    Page<AiUsage> page = usage.findByCompanyIdOrderByOccurredAtDesc(companyId, pageable);
    return PageResponse.from(page.map(AiUsageResponse::from));
  }

  @Transactional(readOnly = true)
  public PageResponse<AiUsageResponse> adminUsage(Pageable pageable) {
    AccessGuard.requireSuperAdmin();
    return PageResponse.from(usage.findAllByOrderByOccurredAtDesc(pageable).map(AiUsageResponse::from));
  }

  @Transactional(readOnly = true)
  public AiUsageSummaryResponse companySummary() {
    UUID companyId = AccessGuard.requireTenant();
    Company company = companies.findById(companyId)
        .orElseThrow(() -> ApiException.notFound("Empresa no encontrada."));
    YearMonth month = YearMonth.now(clock);
    BigDecimal spent = monthlySpend(companyId, month);
    return new AiUsageSummaryResponse(
        month.toString(),
        spent,
        company.getMonthlyAiBudgetEur(),
        properties.isBudgetEnforced(),
        remaining(company.getMonthlyAiBudgetEur(), spent));
  }

  @Transactional(readOnly = true)
  public AiUsageSummaryResponse adminSummary() {
    AccessGuard.requireSuperAdmin();
    YearMonth month = YearMonth.now(clock);
    Instant from = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant to = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    BigDecimal spent = usage.sumAllSuccessfulCostEur(AiUsageStatus.SUCCESS, from, to);
    return new AiUsageSummaryResponse(month.toString(), spent, null, properties.isBudgetEnforced(), null);
  }

  @Transactional(readOnly = true)
  public BigDecimal monthlySpend(UUID companyId, YearMonth month) {
    Instant from = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant to = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    return usage.sumSuccessfulCostEur(companyId, AiUsageStatus.SUCCESS, from, to);
  }

  private static BigDecimal remaining(BigDecimal budget, BigDecimal spent) {
    if (budget == null) {
      return null;
    }
    return budget.subtract(spent);
  }

  private static String blankToDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }
}
