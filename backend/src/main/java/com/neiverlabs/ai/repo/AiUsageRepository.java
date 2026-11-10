package com.neiverlabs.ai.repo;

import com.neiverlabs.ai.domain.AiUsage;
import com.neiverlabs.ai.domain.AiUsageStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiUsageRepository extends JpaRepository<AiUsage, UUID> {

  Page<AiUsage> findAllByOrderByOccurredAtDesc(Pageable pageable);

  Page<AiUsage> findByCompanyIdOrderByOccurredAtDesc(UUID companyId, Pageable pageable);

  @Query("""
      select coalesce(sum(u.estimatedCostEur), 0)
      from AiUsage u
      where u.companyId = :companyId
        and u.status = :status
        and u.occurredAt >= :from
        and u.occurredAt < :to
      """)
  BigDecimal sumSuccessfulCostEur(
      @Param("companyId") UUID companyId,
      @Param("status") AiUsageStatus status,
      @Param("from") Instant from,
      @Param("to") Instant to
  );

  @Query("""
      select coalesce(sum(u.estimatedCostEur), 0)
      from AiUsage u
      where u.status = :status
        and u.occurredAt >= :from
        and u.occurredAt < :to
      """)
  BigDecimal sumAllSuccessfulCostEur(
      @Param("status") AiUsageStatus status,
      @Param("from") Instant from,
      @Param("to") Instant to
  );

  @Query("""
      select u.companyId, coalesce(sum(u.estimatedCostEur), 0)
      from AiUsage u
      where u.status = com.neiverlabs.ai.domain.AiUsageStatus.SUCCESS
        and u.occurredAt >= :from
        and u.occurredAt < :to
      group by u.companyId
      order by coalesce(sum(u.estimatedCostEur), 0) desc
      """)
  List<Object[]> topConsumers(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
