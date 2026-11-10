package com.neiverlabs.hospitality.repo;

import com.neiverlabs.hospitality.domain.HospitalityResource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HospitalityResourceRepository extends JpaRepository<HospitalityResource, UUID> {

  List<HospitalityResource> findByCompanyIdOrderBySortOrderAscNameAsc(UUID companyId);

  Optional<HospitalityResource> findByIdAndCompanyId(UUID id, UUID companyId);

  boolean existsByCompanyIdAndNameIgnoreCase(UUID companyId, String name);

  boolean existsByCompanyIdAndNameIgnoreCaseAndIdNot(UUID companyId, String name, UUID id);

  @Query("""
      select coalesce(sum(r.seats), 0) from HospitalityResource r
      where r.companyId = :companyId and r.active = true
      """)
  long sumActiveSeats(@Param("companyId") UUID companyId);
}
