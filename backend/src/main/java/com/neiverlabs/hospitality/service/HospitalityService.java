package com.neiverlabs.hospitality.service;

import com.neiverlabs.audit.service.AuditActions;
import com.neiverlabs.audit.service.AuditService;
import com.neiverlabs.hospitality.domain.HospitalityReservation;
import com.neiverlabs.hospitality.domain.HospitalityResource;
import com.neiverlabs.hospitality.domain.HospitalityWhatsAppMessage;
import com.neiverlabs.hospitality.domain.ReservationStatus;
import com.neiverlabs.hospitality.repo.HospitalityReservationRepository;
import com.neiverlabs.hospitality.repo.HospitalityResourceRepository;
import com.neiverlabs.hospitality.repo.HospitalityWhatsAppMessageRepository;
import com.neiverlabs.hospitality.repo.HospitalityWhatsAppTemplateRepository;
import com.neiverlabs.hospitality.web.CreateReservationRequest;
import com.neiverlabs.hospitality.web.OccupancyResponse;
import com.neiverlabs.hospitality.web.ReservationResponse;
import com.neiverlabs.hospitality.web.ResourceRequest;
import com.neiverlabs.hospitality.web.ResourceResponse;
import com.neiverlabs.hospitality.web.UpdateReservationRequest;
import com.neiverlabs.hospitality.web.WhatsAppMessageResponse;
import com.neiverlabs.hospitality.web.WhatsAppStatusResponse;
import com.neiverlabs.hospitality.whatsapp.WhatsAppClientException;
import com.neiverlabs.hospitality.whatsapp.WhatsAppCloudClient;
import com.neiverlabs.identity.security.AccessGuard;
import com.neiverlabs.identity.security.NeiverPrincipal;
import com.neiverlabs.identity.security.RequestContext;
import com.neiverlabs.shared.error.ApiException;
import com.neiverlabs.shared.error.ErrorCode;
import com.neiverlabs.shared.web.PageResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HospitalityService {

  public static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Madrid");
  private static final Duration DEFAULT_STAY = Duration.ofMinutes(90);
  private static final Set<ReservationStatus> OCCUPYING = EnumSet.of(
      ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.SEATED);

  private final HospitalityResourceRepository resources;
  private final HospitalityReservationRepository reservations;
  private final HospitalityWhatsAppMessageRepository messages;
  private final HospitalityWhatsAppTemplateRepository templateBodies;
  private final WhatsAppCloudClient whatsApp;
  private final AuditService audit;
  private final Clock clock;

  public HospitalityService(
      HospitalityResourceRepository resources,
      HospitalityReservationRepository reservations,
      HospitalityWhatsAppMessageRepository messages,
      HospitalityWhatsAppTemplateRepository templateBodies,
      WhatsAppCloudClient whatsApp,
      AuditService audit,
      Clock clock
  ) {
    this.resources = resources;
    this.reservations = reservations;
    this.messages = messages;
    this.templateBodies = templateBodies;
    this.whatsApp = whatsApp;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<ResourceResponse> listResources() {
    UUID companyId = AccessGuard.requireTenant();
    return resources.findByCompanyIdOrderBySortOrderAscNameAsc(companyId).stream()
        .map(ResourceResponse::from)
        .toList();
  }

  @Transactional
  public ResourceResponse createResource(ResourceRequest request, RequestContext context) {
    NeiverPrincipal actor = AccessGuard.requireCompanyAdmin();
    UUID companyId = actor.companyId();
    String name = request.name();
    if (resources.existsByCompanyIdAndNameIgnoreCase(companyId, name)) {
      throw ApiException.conflict("Ya hay una mesa o recurso con ese nombre.");
    }
    HospitalityResource resource = new HospitalityResource(
        companyId, name, request.seats(), request.sortOrder() == null ? 0 : request.sortOrder());
    applyLayout(resource, request);
    resources.save(resource);
    audit.record(AuditActions.HOSP_RESOURCE_CREATED, "HospitalityResource", resource.getId(),
        actor.userId(), companyId, Map.of("name", name, "seats", request.seats()), context);
    return ResourceResponse.from(resource);
  }

  @Transactional
  public ResourceResponse updateResource(UUID resourceId, ResourceRequest request, RequestContext context) {
    NeiverPrincipal actor = AccessGuard.requireCompanyAdmin();
    UUID companyId = actor.companyId();
    HospitalityResource resource = resources.findByIdAndCompanyId(resourceId, companyId)
        .orElseThrow(() -> ApiException.notFound("Mesa o recurso no encontrado."));
    if (resources.existsByCompanyIdAndNameIgnoreCaseAndIdNot(companyId, request.name(), resourceId)) {
      throw ApiException.conflict("Ya hay una mesa o recurso con ese nombre.");
    }
    resource.setName(request.name());
    resource.setSeats(request.seats());
    if (request.sortOrder() != null) {
      resource.setSortOrder(request.sortOrder());
    }
    if (request.active() != null) {
      resource.setActive(request.active());
    }
    applyLayout(resource, request);
    resources.save(resource);
    audit.record(AuditActions.HOSP_RESOURCE_UPDATED, "HospitalityResource", resource.getId(),
        actor.userId(), companyId, Map.of("name", resource.getName(), "active", resource.isActive()), context);
    return ResourceResponse.from(resource);
  }

  @Transactional(readOnly = true)
  public PageResponse<ReservationResponse> listReservations(LocalDate day, Pageable pageable) {
    UUID companyId = AccessGuard.requireTenant();
    Pageable safe = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    Page<HospitalityReservation> page;
    if (day != null) {
      Instant from = day.atStartOfDay(BUSINESS_ZONE).toInstant();
      Instant to = day.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
      page = reservations.findByCompanyIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
          companyId, from, to, safe);
    } else {
      page = reservations.findByCompanyIdOrderByStartsAtDesc(companyId, safe);
    }
    Map<UUID, String> names = resourceNames(companyId);
    return PageResponse.from(page.map(item -> ReservationResponse.from(item, names.get(item.getResourceId()))));
  }

  @Transactional
  public ReservationResponse createReservation(CreateReservationRequest request, RequestContext context) {
    NeiverPrincipal actor = AccessGuard.current();
    UUID companyId = AccessGuard.requireTenant();
    Instant startsAt = request.startsAt();
    Instant endsAt = request.endsAt() == null ? startsAt.plus(DEFAULT_STAY) : request.endsAt();
    if (!endsAt.isAfter(startsAt)) {
      throw ApiException.badRequest("La hora de fin tiene que ser posterior al inicio.");
    }
    ReservationStatus status = request.status() == null ? ReservationStatus.CONFIRMED : request.status();
    UUID resourceId = validateResource(companyId, request.resourceId(), request.partySize());
    assertNoOverlap(companyId, resourceId, startsAt, endsAt, null);
    HospitalityReservation reservation = new HospitalityReservation(
        companyId,
        resourceId,
        request.guestName(),
        blankToNull(request.guestPhone()),
        request.partySize(),
        startsAt,
        endsAt,
        status,
        blankToNull(request.notes()),
        actor.userId());
    reservations.save(reservation);
    audit.record(AuditActions.HOSP_RESERVATION_CREATED, "HospitalityReservation", reservation.getId(),
        actor.userId(), companyId,
        Map.of("guest", reservation.getGuestName(), "partySize", reservation.getPartySize(), "status", status.name()),
        context);
    return toResponse(reservation);
  }

  @Transactional
  public ReservationResponse updateReservation(
      UUID reservationId, UpdateReservationRequest request, RequestContext context
  ) {
    NeiverPrincipal actor = AccessGuard.current();
    UUID companyId = AccessGuard.requireTenant();
    HospitalityReservation reservation = reservations.findByIdAndCompanyId(reservationId, companyId)
        .orElseThrow(() -> ApiException.notFound("Reserva no encontrada."));
    if (!request.endsAt().isAfter(request.startsAt())) {
      throw ApiException.badRequest("La hora de fin tiene que ser posterior al inicio.");
    }
    UUID resourceId = validateResource(companyId, request.resourceId(), request.partySize());
    if (request.status().occupiesSlot()) {
      assertNoOverlap(companyId, resourceId, request.startsAt(), request.endsAt(), reservationId);
    }
    reservation.setResourceId(resourceId);
    reservation.setGuestName(request.guestName());
    reservation.setGuestPhone(blankToNull(request.guestPhone()));
    reservation.setPartySize(request.partySize());
    reservation.setStartsAt(request.startsAt());
    reservation.setEndsAt(request.endsAt());
    reservation.setStatus(request.status());
    reservation.setNotes(blankToNull(request.notes()));
    reservations.save(reservation);
    audit.record(AuditActions.HOSP_RESERVATION_UPDATED, "HospitalityReservation", reservation.getId(),
        actor.userId(), companyId, Map.of("status", reservation.getStatus().name()), context);
    return toResponse(reservation);
  }

  @Transactional(readOnly = true)
  public OccupancyResponse occupancy(LocalDate day) {
    UUID companyId = AccessGuard.requireTenant();
    LocalDate target = day == null ? LocalDate.ofInstant(clock.instant(), BUSINESS_ZONE) : day;
    Instant dayStart = target.atStartOfDay(BUSINESS_ZONE).toInstant();
    Instant dayEnd = target.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
    int activeSeats = (int) Math.min(Integer.MAX_VALUE, resources.sumActiveSeats(companyId));
    List<HospitalityReservation> open = reservations.findOccupying(companyId, dayStart, dayEnd, OCCUPYING);
    int covers = open.stream().mapToInt(HospitalityReservation::getPartySize).sum();
    Map<UUID, String> names = resourceNames(companyId);
    double ratio = activeSeats == 0 ? 0d : Math.min(1d, (double) covers / activeSeats);
    return new OccupancyResponse(
        target.toString(),
        BUSINESS_ZONE.getId(),
        activeSeats,
        covers,
        open.size(),
        ratio,
        open.stream().map(item -> ReservationResponse.from(item, names.get(item.getResourceId()))).toList());
  }

  @Transactional(readOnly = true)
  public WhatsAppStatusResponse whatsAppStatus() {
    AccessGuard.requireTenant();
    if (whatsApp.available()) {
      return new WhatsAppStatusResponse(true, "WhatsApp Cloud API configurado en el servidor.");
    }
    return new WhatsAppStatusResponse(
        false,
        "Faltan WHATSAPP_ACCESS_TOKEN o WHATSAPP_PHONE_NUMBER_ID. Sin eso no se envían avisos.");
  }

  @Transactional
  public WhatsAppMessageResponse notifyReservation(UUID reservationId, RequestContext context) {
    return notifyReservation(reservationId, null, context);
  }

  @Transactional
  public WhatsAppMessageResponse notifyReservation(
      UUID reservationId, UUID templateId, RequestContext context
  ) {
    NeiverPrincipal actor = AccessGuard.current();
    UUID companyId = AccessGuard.requireTenant();
    HospitalityReservation reservation = reservations.findByIdAndCompanyId(reservationId, companyId)
        .orElseThrow(() -> ApiException.notFound("Reserva no encontrada."));
    if (!StringUtils.hasText(reservation.getGuestPhone())) {
      throw ApiException.badRequest("La reserva no tiene teléfono para WhatsApp.");
    }
    String phone = normalizePhone(reservation.getGuestPhone());
    String body = templateId == null
        ? buildConfirmationBody(reservation)
        : renderTemplateBody(companyId, templateId, reservation);
    if (!whatsApp.available()) {
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          ErrorCode.WHATSAPP_NOT_CONFIGURED,
          "WhatsApp no configurado",
          "Define WHATSAPP_ACCESS_TOKEN y WHATSAPP_PHONE_NUMBER_ID en el servidor.");
    }
    try {
      String providerId = whatsApp.sendText(phone, body);
      HospitalityWhatsAppMessage saved = messages.save(
          HospitalityWhatsAppMessage.sent(companyId, reservationId, phone, body, providerId));
      audit.record(AuditActions.HOSP_WHATSAPP_SENT, "HospitalityWhatsAppMessage", saved.getId(),
          actor.userId(), companyId, Map.of("reservationId", reservationId.toString()), context);
      return WhatsAppMessageResponse.from(saved, true);
    } catch (WhatsAppClientException ex) {
      messages.save(HospitalityWhatsAppMessage.failed(
          companyId, reservationId, phone, body, ex.getErrorCode(), ex.getMessage()));
      throw new ApiException(
          HttpStatus.BAD_GATEWAY,
          ErrorCode.WHATSAPP_PROVIDER_ERROR,
          "Error de WhatsApp",
          ex.getMessage());
    }
  }

  private UUID validateResource(UUID companyId, UUID resourceId, int partySize) {
    if (resourceId == null) {
      return null;
    }
    HospitalityResource resource = resources.findByIdAndCompanyId(resourceId, companyId)
        .orElseThrow(() -> ApiException.notFound("Mesa o recurso no encontrado."));
    if (!resource.isActive()) {
      throw ApiException.badRequest("Esa mesa está desactivada.");
    }
    if (partySize > resource.getSeats()) {
      throw ApiException.badRequest(
          "La mesa " + resource.getName() + " admite como máximo " + resource.getSeats() + " comensales.");
    }
    return resourceId;
  }

  private void assertNoOverlap(
      UUID companyId, UUID resourceId, Instant startsAt, Instant endsAt, UUID excludeId
  ) {
    if (resourceId == null) {
      return;
    }
    if (reservations.existsOverlap(companyId, resourceId, startsAt, endsAt, OCCUPYING, excludeId)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          ErrorCode.RESOURCE_CONFLICT,
          "Mesa ocupada",
          "Esa mesa ya tiene una reserva que se solapa en ese horario.");
    }
  }

  private Map<UUID, String> resourceNames(UUID companyId) {
    return resources.findByCompanyIdOrderBySortOrderAscNameAsc(companyId).stream()
        .collect(Collectors.toMap(HospitalityResource::getId, HospitalityResource::getName));
  }

  private ReservationResponse toResponse(HospitalityReservation reservation) {
    String name = null;
    if (reservation.getResourceId() != null) {
      name = resources.findById(reservation.getResourceId()).map(HospitalityResource::getName).orElse(null);
    }
    return ReservationResponse.from(reservation, name);
  }

  private void applyLayout(HospitalityResource resource, ResourceRequest request) {
    if (request.posX() != null) {
      resource.setPosX(request.posX());
    }
    if (request.posY() != null) {
      resource.setPosY(request.posY());
    }
    if (request.width() != null) {
      resource.setWidth(request.width());
    }
    if (request.height() != null) {
      resource.setHeight(request.height());
    }
  }

  private String renderTemplateBody(UUID companyId, UUID templateId, HospitalityReservation reservation) {
    var template = templateBodies.findByIdAndCompanyId(templateId, companyId)
        .orElseThrow(() -> ApiException.notFound("Plantilla WhatsApp no encontrada."));
    if (!template.isActive()) {
      throw ApiException.badRequest("Esa plantilla está desactivada.");
    }
    return TemplateRenderer.render(template.getBody(), reservation, BUSINESS_ZONE, resources);
  }

  private String buildConfirmationBody(HospitalityReservation reservation) {
    var localStart = reservation.getStartsAt().atZone(BUSINESS_ZONE);
    String when = localStart.toLocalDate() + " a las "
        + String.format("%02d:%02d", localStart.getHour(), localStart.getMinute());
    String resource = reservation.getResourceId() == null
        ? ""
        : resources.findById(reservation.getResourceId())
            .map(item -> " Mesa: " + item.getName() + ".")
            .orElse("");
    return "Hola " + reservation.getGuestName() + ", tu reserva para "
        + reservation.getPartySize() + " el " + when + " está "
        + reservation.getStatus().name().toLowerCase() + "." + resource
        + " Si necesitas cambiarla, responde a este mensaje.";
  }

  static String normalizePhone(String raw) {
    String digits = raw.replaceAll("[^0-9+]", "");
    if (digits.startsWith("00")) {
      digits = "+" + digits.substring(2);
    }
    if (digits.startsWith("+")) {
      return digits.substring(1);
    }
    if (digits.startsWith("6") || digits.startsWith("7")) {
      return "34" + digits;
    }
    return digits;
  }

  private static String blankToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
