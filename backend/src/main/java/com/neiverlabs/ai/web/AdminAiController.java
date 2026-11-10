package com.neiverlabs.ai.web;

import com.neiverlabs.ai.domain.AiModelPricing;
import com.neiverlabs.ai.repo.AiModelPricingRepository;
import com.neiverlabs.ai.service.AiService;
import com.neiverlabs.identity.security.AccessGuard;
import com.neiverlabs.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ai")
public class AdminAiController {

  private final AiService ai;
  private final AiModelPricingRepository pricing;
  private final Clock clock;

  public AdminAiController(AiService ai, AiModelPricingRepository pricing, Clock clock) {
    this.ai = ai;
    this.pricing = pricing;
    this.clock = clock;
  }

  @GetMapping("/usage")
  public PageResponse<AiUsageResponse> usage(Pageable pageable) {
    return ai.adminUsage(pageable);
  }

  @GetMapping("/usage/summary")
  public AiUsageSummaryResponse summary() {
    return ai.adminSummary();
  }

  @GetMapping("/pricing")
  @Transactional(readOnly = true)
  public List<AiPricingResponse> pricing() {
    AccessGuard.requireSuperAdmin();
    return pricing.findAllByOrderByProviderCodeAscModelCodeAscEffectiveFromDesc()
        .stream()
        .map(AiPricingResponse::from)
        .toList();
  }

  @PostMapping("/pricing")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public AiPricingResponse createPricing(@Valid @RequestBody CreateAiPricingRequest request) {
    AccessGuard.requireSuperAdmin();
    Instant from = request.effectiveFrom() == null ? clock.instant() : request.effectiveFrom();
    pricing.findCurrent(request.provider().trim(), request.model().trim(), from)
        .ifPresent(current -> current.close(from));
    AiModelPricing created = pricing.save(new AiModelPricing(
        request.provider().trim().toLowerCase(),
        request.model().trim(),
        request.inputUsdPerMillion(),
        request.outputUsdPerMillion(),
        from,
        request.sourceNote().trim()));
    return AiPricingResponse.from(created);
  }
}
