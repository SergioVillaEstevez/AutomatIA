package com.neiverlabs.hospitality.repo;

import com.neiverlabs.hospitality.domain.HospitalityVenueSettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalityVenueSettingsRepository extends JpaRepository<HospitalityVenueSettings, UUID> {

  Optional<HospitalityVenueSettings> findByPublicSlug(String publicSlug);

  boolean existsByPublicSlugAndCompanyIdNot(String publicSlug, UUID companyId);
}
