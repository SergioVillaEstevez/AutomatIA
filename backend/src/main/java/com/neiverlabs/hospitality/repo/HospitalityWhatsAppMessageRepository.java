package com.neiverlabs.hospitality.repo;

import com.neiverlabs.hospitality.domain.HospitalityWhatsAppMessage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalityWhatsAppMessageRepository extends JpaRepository<HospitalityWhatsAppMessage, UUID> {
}
