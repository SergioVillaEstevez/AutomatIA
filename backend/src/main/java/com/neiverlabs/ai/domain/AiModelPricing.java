package com.neiverlabs.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_model_pricing")
public class AiModelPricing {

  @Id
  private UUID id;

  @Column(name = "provider_code", nullable = false, length = 40)
  private String providerCode;

  @Column(name = "model_code", nullable = false, length = 80)
  private String modelCode;

  @Column(name = "input_usd_per_million", nullable = false, precision = 12, scale = 6)
  private BigDecimal inputUsdPerMillion;

  @Column(name = "output_usd_per_million", nullable = false, precision = 12, scale = 6)
  private BigDecimal outputUsdPerMillion;

  @Column(nullable = false, length = 3)
  private String currency = "USD";

  @Column(name = "effective_from", nullable = false)
  private Instant effectiveFrom;

  @Column(name = "effective_to")
  private Instant effectiveTo;

  @Column(name = "source_note", nullable = false, length = 300)
  private String sourceNote;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected AiModelPricing() {
  }

  public AiModelPricing(
      String providerCode,
      String modelCode,
      BigDecimal inputUsdPerMillion,
      BigDecimal outputUsdPerMillion,
      Instant effectiveFrom,
      String sourceNote
  ) {
    this.id = UUID.randomUUID();
    this.providerCode = providerCode;
    this.modelCode = modelCode;
    this.inputUsdPerMillion = inputUsdPerMillion;
    this.outputUsdPerMillion = outputUsdPerMillion;
    this.effectiveFrom = effectiveFrom;
    this.sourceNote = sourceNote;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public String getProviderCode() {
    return providerCode;
  }

  public String getModelCode() {
    return modelCode;
  }

  public BigDecimal getInputUsdPerMillion() {
    return inputUsdPerMillion;
  }

  public BigDecimal getOutputUsdPerMillion() {
    return outputUsdPerMillion;
  }

  public String getCurrency() {
    return currency;
  }

  public Instant getEffectiveFrom() {
    return effectiveFrom;
  }

  public Instant getEffectiveTo() {
    return effectiveTo;
  }

  public String getSourceNote() {
    return sourceNote;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void close(Instant at) {
    this.effectiveTo = at;
  }
}
