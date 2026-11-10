package com.neiverlabs.hospitality.repo;

import com.neiverlabs.hospitality.domain.HospitalityTicket;
import com.neiverlabs.hospitality.domain.TicketStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalityTicketRepository extends JpaRepository<HospitalityTicket, UUID> {

  List<HospitalityTicket> findByCompanyIdAndStatusOrderByOpenedAtDesc(UUID companyId, TicketStatus status);

  List<HospitalityTicket> findByCompanyIdOrderByOpenedAtDesc(UUID companyId);

  Optional<HospitalityTicket> findByIdAndCompanyId(UUID id, UUID companyId);
}
