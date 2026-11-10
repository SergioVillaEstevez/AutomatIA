package com.neiverlabs.hospitality.repo;

import com.neiverlabs.hospitality.domain.HospitalityTicketLine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalityTicketLineRepository extends JpaRepository<HospitalityTicketLine, UUID> {

  List<HospitalityTicketLine> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);

  void deleteByIdAndCompanyId(UUID id, UUID companyId);
}
