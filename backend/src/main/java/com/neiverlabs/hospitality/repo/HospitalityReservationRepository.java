package com.neiverlabs.hospitality.repo;

import com.neiverlabs.hospitality.domain.HospitalityReservation;
import com.neiverlabs.hospitality.domain.ReservationStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HospitalityReservationRepository extends JpaRepository<HospitalityReservation, UUID> {

  Page<HospitalityReservation> findByCompanyIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
      UUID companyId, Instant from, Instant to, Pageable pageable);

  Page<HospitalityReservation> findByCompanyIdOrderByStartsAtDesc(UUID companyId, Pageable pageable);

  Optional<HospitalityReservation> findByIdAndCompanyId(UUID id, UUID companyId);

  @Query("""
      select count(r) > 0 from HospitalityReservation r
      where r.companyId = :companyId
        and r.resourceId = :resourceId
        and r.status in :statuses
        and r.startsAt < :endsAt
        and r.endsAt > :startsAt
        and (:excludeId is null or r.id <> :excludeId)
      """)
  boolean existsOverlap(
      @Param("companyId") UUID companyId,
      @Param("resourceId") UUID resourceId,
      @Param("startsAt") Instant startsAt,
      @Param("endsAt") Instant endsAt,
      @Param("statuses") Collection<ReservationStatus> statuses,
      @Param("excludeId") UUID excludeId);

  @Query("""
      select r from HospitalityReservation r
      where r.companyId = :companyId
        and r.status in :statuses
        and r.startsAt < :dayEnd
        and r.endsAt > :dayStart
      order by r.startsAt asc
      """)
  List<HospitalityReservation> findOccupying(
      @Param("companyId") UUID companyId,
      @Param("dayStart") Instant dayStart,
      @Param("dayEnd") Instant dayEnd,
      @Param("statuses") Collection<ReservationStatus> statuses);
}
