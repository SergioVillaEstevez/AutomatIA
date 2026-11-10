package com.neiverlabs.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "neiver.ai")
public class AiProperties {

  /**
   * Clave de OpenAI. Solo en el backend. Vacía = proveedor no disponible.
   */
  private String openaiApiKey = "";

  private String openaiBaseUrl = "https://api.openai.com";

  /** Modelo por defecto si la configuración de BD no dice otra cosa. */
  private String defaultModel = "gpt-4.1-mini";

  private int timeoutSeconds = 60;

  /** Tipo de cambio USD→EUR para estimar el coste en euros. Actualizable sin redeploy de lógica. */
  private BigDecimal usdToEur = new BigDecimal("0.92");

  private boolean budgetEnforced = true;

  public String getOpenaiApiKey() {
    return openaiApiKey;
  }

  public void setOpenaiApiKey(String openaiApiKey) {
    this.openaiApiKey = openaiApiKey;
  }

  public String getOpenaiBaseUrl() {
    return openaiBaseUrl;
  }

  public void setOpenaiBaseUrl(String openaiBaseUrl) {
    this.openaiBaseUrl = openaiBaseUrl;
  }

  public String getDefaultModel() {
    return defaultModel;
  }

  public void setDefaultModel(String defaultModel) {
    this.defaultModel = defaultModel;
  }

  public int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  public void setTimeoutSeconds(int timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
  }

  public BigDecimal getUsdToEur() {
    return usdToEur;
  }

  public void setUsdToEur(BigDecimal usdToEur) {
    this.usdToEur = usdToEur;
  }

  public boolean isBudgetEnforced() {
    return budgetEnforced;
  }

  public void setBudgetEnforced(boolean budgetEnforced) {
    this.budgetEnforced = budgetEnforced;
  }
}
