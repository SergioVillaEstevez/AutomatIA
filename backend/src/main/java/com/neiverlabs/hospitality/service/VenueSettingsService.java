package com.neiverlabs.hospitality.service;

import com.neiverlabs.company.repo.CompanyRepository;
import com.neiverlabs.hospitality.domain.HospitalityVenueSettings;
import com.neiverlabs.hospitality.domain.ReservationStatus;
import com.neiverlabs.hospitality.repo.HospitalityReservationRepository;
import com.neiverlabs.hospitality.repo.HospitalityResourceRepository;
import com.neiverlabs.hospitality.repo.HospitalityVenueSettingsRepository;
import com.neiverlabs.hospitality.web.FloorPlanResponse;
import com.neiverlabs.hospitality.web.ReservationResponse;
import com.neiverlabs.hospitality.web.ResourceResponse;
import com.neiverlabs.hospitality.web.VenueSettingsRequest;
import com.neiverlabs.hospitality.web.VenueSettingsResponse;
import com.neiverlabs.identity.security.AccessGuard;
import com.neiverlabs.identity.security.NeiverPrincipal;
import com.neiverlabs.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VenueSettingsService {

  private final HospitalityVenueSettingsRepository venues;
  private final HospitalityResourceRepository resources;
  private final HospitalityReservationRepository reservations;
  private final CompanyRepository companies;
  private final Clock clock;

  public VenueSettingsService(
      HospitalityVenueSettingsRepository venues,
      HospitalityResourceRepository resources,
      HospitalityReservationRepository reservations,
      CompanyRepository companies,
      Clock clock
  ) {
    this.venues = venues;
    this.resources = resources;
    this.reservations = reservations;
    this.companies = companies;
    this.clock = clock;
  }

  @Transactional
  public VenueSettingsResponse get() {
    UUID companyId = AccessGuard.requireTenant();
    return VenueSettingsResponse.from(requireOrBootstrap(companyId));
  }

  @Transactional
  public VenueSettingsResponse save(VenueSettingsRequest request) {
    NeiverPrincipal actor = AccessGuard.requireCompanyAdmin();
    UUID companyId = actor.companyId();
    String slug = request.publicSlug().toLowerCase(Locale.ROOT);
    if (venues.existsByPublicSlugAndCompanyIdNot(slug, companyId)) {
      throw ApiException.conflict("Ese slug público ya está en uso.");
    }
    HospitalityVenueSettings settings = venues.findById(companyId)
        .orElseGet(() -> new HospitalityVenueSettings(companyId, slug));
    settings.setPublicSlug(slug);
    settings.setPublicBookingEnabled(Boolean.TRUE.equals(request.publicBookingEnabled()));
    settings.setPublicBookingNote(request.publicBookingNote());
    if (request.floorWidth() != null) {
      settings.setFloorWidth(request.floorWidth());
    }
    if (request.floorHeight() != null) {
      settings.setFloorHeight(request.floorHeight());
    }
    venues.save(settings);
    return VenueSettingsResponse.from(settings);
  }

  @Transactional
  public FloorPlanResponse floorPlan(LocalDate day) {
    UUID companyId = AccessGuard.requireTenant();
    HospitalityVenueSettings settings = requireOrBootstrap(companyId);
    LocalDate target = day == null
        ? LocalDate.ofInstant(clock.instant(), HospitalityService.BUSINESS_ZONE)
        : day;
    Instant from = target.atStartOfDay(HospitalityService.BUSINESS_ZONE).toInstant();
    Instant to = target.plusDays(1).atStartOfDay(HospitalityService.BUSINESS_ZONE).toInstant();
    var open = reservations.findOccupying(
        companyId, from, to,
        EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.SEATED));
    Map<UUID, String> names = resources.findByCompanyIdOrderBySortOrderAscNameAsc(companyId).stream()
        .collect(Collectors.toMap(r -> r.getId(), r -> r.getName()));
    return new FloorPlanResponse(
        settings.getFloorWidth(),
        settings.getFloorHeight(),
        target.toString(),
        resources.findByCompanyIdOrderBySortOrderAscNameAsc(companyId).stream().map(ResourceResponse::from).toList(),
        open.stream().map(item -> ReservationResponse.from(item, names.get(item.getResourceId()))).toList());
  }

  private HospitalityVenueSettings requireOrBootstrap(UUID companyId) {
    return venues.findById(companyId).orElseGet(() -> {
      String base = companies.findById(companyId)
          .map(company -> slugify(company.getTradeName()))
          .orElse("local-" + companyId.toString().substring(0, 8));
      return venues.save(new HospitalityVenueSettings(companyId, uniqueSlug(base, companyId)));
    });
  }

  private String uniqueSlug(String base, UUID companyId) {
    String candidate = base;
    for (int i = 2; i <= 100; i++) {
      var existing = venues.findByPublicSlug(candidate);
      if (existing.isEmpty() || existing.get().getCompanyId().equals(companyId)) {
        return candidate;
      }
      candidate = base + "-" + i;
    }
    return base + "-" + companyId.toString().substring(0, 8);
  }

  static String slugify(String value) {
    String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-)|(-$)", "");
    if (normalized.isBlank()) {
      return "local";
    }
    return normalized.substring(0, Math.min(60, normalized.length()));
  }
}
