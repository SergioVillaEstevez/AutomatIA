package com.neiverlabs.hospitality.repo;

import com.neiverlabs.hospitality.domain.HospitalityWhatsAppTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalityWhatsAppTemplateRepository extends JpaRepository<HospitalityWhatsAppTemplate, UUID> {

  List<HospitalityWhatsAppTemplate> findByCompanyIdOrderByTitleAsc(UUID companyId);

  Optional<HospitalityWhatsAppTemplate> findByIdAndCompanyId(UUID id, UUID companyId);

  Optional<HospitalityWhatsAppTemplate> findByCompanyIdAndCode(UUID companyId, String code);

  boolean existsByCompanyIdAndCodeAndIdNot(UUID companyId, String code, UUID id);
}
