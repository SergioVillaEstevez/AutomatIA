package com.neiverlabs.hospitality.web;

import com.neiverlabs.hospitality.domain.TicketStatus;
import com.neiverlabs.hospitality.service.HospitalityService;
import com.neiverlabs.hospitality.service.TpvService;
import com.neiverlabs.hospitality.service.VenueSettingsService;
import com.neiverlabs.hospitality.service.WhatsAppTemplateService;
import com.neiverlabs.hospitality.web.TicketRequests.AddLineRequest;
import com.neiverlabs.hospitality.web.TicketRequests.CreateTicketRequest;
import com.neiverlabs.hospitality.web.TicketRequests.PayTicketRequest;
import com.neiverlabs.identity.security.RequestContext;
import com.neiverlabs.shared.web.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/company/hospitality")
@PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPANY_USER')")
public class CompanyHospitalityController {

  private final HospitalityService hospitality;
  private final VenueSettingsService venues;
  private final WhatsAppTemplateService templates;
  private final TpvService tpv;

  public CompanyHospitalityController(
      HospitalityService hospitality,
      VenueSettingsService venues,
      WhatsAppTemplateService templates,
      TpvService tpv
  ) {
    this.hospitality = hospitality;
    this.venues = venues;
    this.templates = templates;
    this.tpv = tpv;
  }

  @GetMapping("/resources")
  public List<ResourceResponse> resources() {
    return hospitality.listResources();
  }

  @PostMapping("/resources")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('COMPANY_ADMIN')")
  public ResourceResponse createResource(
      @Valid @RequestBody ResourceRequest request, HttpServletRequest httpRequest
  ) {
    return hospitality.createResource(request, RequestContext.from(httpRequest));
  }

  @PutMapping("/resources/{resourceId}")
  @PreAuthorize("hasRole('COMPANY_ADMIN')")
  public ResourceResponse updateResource(
      @PathVariable UUID resourceId,
      @Valid @RequestBody ResourceRequest request,
      HttpServletRequest httpRequest
  ) {
    return hospitality.updateResource(resourceId, request, RequestContext.from(httpRequest));
  }

  @GetMapping("/reservations")
  public PageResponse<ReservationResponse> reservations(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
      Pageable pageable
  ) {
    return hospitality.listReservations(day, pageable);
  }

  @PostMapping("/reservations")
  @ResponseStatus(HttpStatus.CREATED)
  public ReservationResponse createReservation(
      @Valid @RequestBody CreateReservationRequest request, HttpServletRequest httpRequest
  ) {
    return hospitality.createReservation(request, RequestContext.from(httpRequest));
  }

  @PutMapping("/reservations/{reservationId}")
  public ReservationResponse updateReservation(
      @PathVariable UUID reservationId,
      @Valid @RequestBody UpdateReservationRequest request,
      HttpServletRequest httpRequest
  ) {
    return hospitality.updateReservation(reservationId, request, RequestContext.from(httpRequest));
  }

  @GetMapping("/occupancy")
  public OccupancyResponse occupancy(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day
  ) {
    return hospitality.occupancy(day);
  }

  @GetMapping("/floor-plan")
  public FloorPlanResponse floorPlan(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day
  ) {
    return venues.floorPlan(day);
  }

  @GetMapping("/venue")
  @PreAuthorize("hasRole('COMPANY_ADMIN')")
  public VenueSettingsResponse venue() {
    return venues.get();
  }

  @PutMapping("/venue")
  @PreAuthorize("hasRole('COMPANY_ADMIN')")
  public VenueSettingsResponse saveVenue(@Valid @RequestBody VenueSettingsRequest request) {
    return venues.save(request);
  }

  @GetMapping("/whatsapp/status")
  public WhatsAppStatusResponse whatsAppStatus() {
    return hospitality.whatsAppStatus();
  }

  @GetMapping("/whatsapp/templates")
  public List<WhatsAppTemplateResponse> whatsAppTemplates() {
    return templates.list();
  }

  @PostMapping("/whatsapp/templates")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('COMPANY_ADMIN')")
  public WhatsAppTemplateResponse createTemplate(@Valid @RequestBody WhatsAppTemplateRequest request) {
    return templates.create(request);
  }

  @PutMapping("/whatsapp/templates/{templateId}")
  @PreAuthorize("hasRole('COMPANY_ADMIN')")
  public WhatsAppTemplateResponse updateTemplate(
      @PathVariable UUID templateId, @Valid @RequestBody WhatsAppTemplateRequest request
  ) {
    return templates.update(templateId, request);
  }

  @PostMapping("/reservations/{reservationId}/whatsapp")
  public WhatsAppMessageResponse notify(
      @PathVariable UUID reservationId,
      @RequestBody(required = false) NotifyWhatsAppRequest body,
      HttpServletRequest httpRequest
  ) {
    UUID templateId = body == null ? null : body.templateId();
    return hospitality.notifyReservation(reservationId, templateId, RequestContext.from(httpRequest));
  }

  @GetMapping("/tickets")
  public List<TicketResponse> tickets(@RequestParam(required = false) TicketStatus status) {
    return tpv.list(status);
  }

  @GetMapping("/tickets/{ticketId}")
  public TicketResponse ticket(@PathVariable UUID ticketId) {
    return tpv.get(ticketId);
  }

  @PostMapping("/tickets")
  @ResponseStatus(HttpStatus.CREATED)
  public TicketResponse openTicket(
      @Valid @RequestBody CreateTicketRequest request
  ) {
    return tpv.open(request);
  }

  @PostMapping("/tickets/{ticketId}/lines")
  public TicketResponse addLine(
      @PathVariable UUID ticketId, @Valid @RequestBody AddLineRequest request
  ) {
    return tpv.addLine(ticketId, request);
  }

  @PostMapping("/tickets/{ticketId}/payment")
  public TicketResponse pay(
      @PathVariable UUID ticketId, @Valid @RequestBody PayTicketRequest request
  ) {
    return tpv.pay(ticketId, request);
  }

  @PostMapping("/tickets/{ticketId}/cancellation")
  public TicketResponse cancel(@PathVariable UUID ticketId) {
    return tpv.cancel(ticketId);
  }
}
