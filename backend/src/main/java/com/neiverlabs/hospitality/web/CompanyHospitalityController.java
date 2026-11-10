package com.neiverlabs.hospitality.web;

import com.neiverlabs.hospitality.service.HospitalityService;
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

  public CompanyHospitalityController(HospitalityService hospitality) {
    this.hospitality = hospitality;
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

  @GetMapping("/whatsapp/status")
  public WhatsAppStatusResponse whatsAppStatus() {
    return hospitality.whatsAppStatus();
  }

  @PostMapping("/reservations/{reservationId}/whatsapp")
  public WhatsAppMessageResponse notify(
      @PathVariable UUID reservationId, HttpServletRequest httpRequest
  ) {
    return hospitality.notifyReservation(reservationId, RequestContext.from(httpRequest));
  }
}
