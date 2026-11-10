package com.neiverlabs.hospitality.service;

import com.neiverlabs.company.repo.CompanyRepository;
import com.neiverlabs.hospitality.domain.HospitalityReservation;
import com.neiverlabs.hospitality.domain.HospitalityVenueSettings;
import com.neiverlabs.hospitality.domain.ReservationStatus;
import com.neiverlabs.hospitality.repo.HospitalityReservationRepository;
import com.neiverlabs.hospitality.repo.HospitalityResourceRepository;
import com.neiverlabs.hospitality.repo.HospitalityVenueSettingsRepository;
import com.neiverlabs.hospitality.web.PublicBookingRequest;
import com.neiverlabs.hospitality.web.PublicBookingResponse;
import com.neiverlabs.hospitality.web.PublicVenueResponse;
import com.neiverlabs.shared.error.ApiException;
import com.neiverlabs.tenant.RlsSession;
import com.neiverlabs.tenant.TenantContext;
import com.neiverlabs.tenant.TenantSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PublicBookingService {

  private static final Duration DEFAULT_STAY = Duration.ofMinutes(90);

  private final HospitalityVenueSettingsRepository venues;
  private final HospitalityResourceRepository resources;
  private final HospitalityReservationRepository reservations;
  private final CompanyRepository companies;
  private final RlsSession rlsSession;
  private final PublicBookingRateLimiter rateLimiter;

  public PublicBookingService(
      HospitalityVenueSettingsRepository venues,
      HospitalityResourceRepository resources,
      HospitalityReservationRepository reservations,
      CompanyRepository companies,
      RlsSession rlsSession,
      PublicBookingRateLimiter rateLimiter
  ) {
    this.venues = venues;
    this.resources = resources;
    this.reservations = reservations;
    this.companies = companies;
    this.rlsSession = rlsSession;
    this.rateLimiter = rateLimiter;
  }

  @Transactional(readOnly = true)
  public PublicVenueResponse venue(String slug, String ip) {
    rateLimiter.check(ip);
    HospitalityVenueSettings settings = resolveEnabled(slug);
    TenantContext.set(TenantSnapshot.forPublicBooking(settings.getCompanyId()));
    rlsSession.sync();
    String tradeName = companies.findById(settings.getCompanyId())
        .map(company -> company.getTradeName())
        .orElse(settings.getPublicSlug());
    var publicResources = resources.findByCompanyIdOrderBySortOrderAscNameAsc(settings.getCompanyId()).stream()
        .filter(resource -> resource.isActive())
        .map(resource -> new PublicVenueResponse.PublicResourceResponse(
            resource.getId().toString(), resource.getName(), resource.getSeats()))
        .toList();
    return new PublicVenueResponse(
        settings.getPublicSlug(),
        tradeName,
        settings.getPublicBookingNote(),
        publicResources);
  }

  @Transactional
  public PublicBookingResponse book(String slug, PublicBookingRequest request, String ip) {
    rateLimiter.check(ip);
    HospitalityVenueSettings settings = resolveEnabled(slug);
    TenantContext.set(TenantSnapshot.forPublicBooking(settings.getCompanyId()));
    rlsSession.sync();
    UUID companyId = settings.getCompanyId();
    Instant startsAt = request.startsAt();
    Instant endsAt = request.endsAt() == null ? startsAt.plus(DEFAULT_STAY) : request.endsAt();
    if (!endsAt.isAfter(startsAt)) {
      throw ApiException.badRequest("La hora de fin tiene que ser posterior al inicio.");
    }
    UUID resourceId = null;
    if (request.resourceId() != null) {
      var resource = resources.findByIdAndCompanyId(request.resourceId(), companyId)
          .orElseThrow(() -> ApiException.notFound("Mesa no disponible."));
      if (!resource.isActive()) {
        throw ApiException.badRequest("Esa mesa no admite reservas públicas.");
      }
      if (request.partySize() > resource.getSeats()) {
        throw ApiException.badRequest("La mesa no admite tantos comensales.");
      }
      if (reservations.existsOverlap(
          companyId, resource.getId(), startsAt, endsAt,
          EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.SEATED),
          null)) {
        throw ApiException.conflict("Esa mesa ya tiene una reserva en ese horario.");
      }
      resourceId = resource.getId();
    }
    HospitalityReservation reservation = new HospitalityReservation(
        companyId,
        resourceId,
        request.guestName(),
        StringUtils.hasText(request.guestPhone()) ? request.guestPhone() : null,
        request.partySize(),
        startsAt,
        endsAt,
        ReservationStatus.PENDING,
        StringUtils.hasText(request.notes()) ? request.notes() : null,
        null);
    reservations.save(reservation);
    return new PublicBookingResponse(
        reservation.getId(),
        reservation.getGuestName(),
        reservation.getPartySize(),
        reservation.getStartsAt(),
        reservation.getEndsAt(),
        reservation.getStatus().name(),
        "Reserva recibida como pendiente. El local la confirmará.");
  }

  private HospitalityVenueSettings resolveEnabled(String slug) {
    TenantContext.set(TenantSnapshot.bypass(null));
    rlsSession.sync();
    HospitalityVenueSettings settings = venues.findByPublicSlug(slug.toLowerCase())
        .orElseThrow(() -> ApiException.notFound("Local no encontrado."));
    if (!settings.isPublicBookingEnabled()) {
      throw ApiException.notFound("Este local no admite reservas online ahora mismo.");
    }
    return settings;
  }
}
