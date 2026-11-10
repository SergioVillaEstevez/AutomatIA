package com.neiverlabs.ai.repo;

import com.neiverlabs.ai.domain.AiModelPricing;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiModelPricingRepository extends JpaRepository<AiModelPricing, UUID> {

  @Query("""
      select p from AiModelPricing p
      where p.providerCode = :provider
        and p.modelCode = :model
        and p.effectiveFrom <= :at
        and (p.effectiveTo is null or p.effectiveTo > :at)
      order by p.effectiveFrom desc
      """)
  List<AiModelPricing> findActive(
      @Param("provider") String provider,
      @Param("model") String model,
      @Param("at") Instant at
  );

  default Optional<AiModelPricing> findCurrent(String provider, String model, Instant at) {
    List<AiModelPricing> rows = findActive(provider, model, at);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }

  List<AiModelPricing> findAllByOrderByProviderCodeAscModelCodeAscEffectiveFromDesc();
}
