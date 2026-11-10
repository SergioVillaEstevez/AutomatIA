package com.neiverlabs.ai.repo;

import com.neiverlabs.ai.domain.AiProviderConfiguration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiProviderConfigurationRepository extends JpaRepository<AiProviderConfiguration, UUID> {

  @Query("""
      select c from AiProviderConfiguration c
      where c.enabled = true
        and c.taskType = :taskType
        and (c.companyId = :companyId or c.companyId is null)
      order by case when c.companyId is null then 1 else 0 end
      """)
  List<AiProviderConfiguration> findEnabled(@Param("companyId") UUID companyId, @Param("taskType") String taskType);

  default Optional<AiProviderConfiguration> resolve(UUID companyId, String taskType) {
    List<AiProviderConfiguration> rows = findEnabled(companyId, taskType);
    if (!rows.isEmpty()) {
      return Optional.of(rows.getFirst());
    }
    if (!"DEFAULT".equals(taskType)) {
      List<AiProviderConfiguration> fallback = findEnabled(companyId, "DEFAULT");
      if (!fallback.isEmpty()) {
        return Optional.of(fallback.getFirst());
      }
    }
    return Optional.empty();
  }
}
