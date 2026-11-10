package com.neiverlabs.hospitality.service;

import com.neiverlabs.hospitality.domain.HospitalityTicket;
import com.neiverlabs.hospitality.domain.HospitalityTicketLine;
import com.neiverlabs.hospitality.domain.TicketStatus;
import com.neiverlabs.hospitality.repo.HospitalityReservationRepository;
import com.neiverlabs.hospitality.repo.HospitalityResourceRepository;
import com.neiverlabs.hospitality.repo.HospitalityTicketLineRepository;
import com.neiverlabs.hospitality.repo.HospitalityTicketRepository;
import com.neiverlabs.hospitality.web.TicketRequests.AddLineRequest;
import com.neiverlabs.hospitality.web.TicketRequests.CreateTicketRequest;
import com.neiverlabs.hospitality.web.TicketRequests.PayTicketRequest;
import com.neiverlabs.hospitality.web.TicketResponse;
import com.neiverlabs.identity.security.AccessGuard;
import com.neiverlabs.identity.security.NeiverPrincipal;
import com.neiverlabs.shared.error.ApiException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TpvService {

  private final HospitalityTicketRepository tickets;
  private final HospitalityTicketLineRepository lines;
  private final HospitalityReservationRepository reservations;
  private final HospitalityResourceRepository resources;
  private final Clock clock;

  public TpvService(
      HospitalityTicketRepository tickets,
      HospitalityTicketLineRepository lines,
      HospitalityReservationRepository reservations,
      HospitalityResourceRepository resources,
      Clock clock
  ) {
    this.tickets = tickets;
    this.lines = lines;
    this.reservations = reservations;
    this.resources = resources;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<TicketResponse> list(TicketStatus status) {
    UUID companyId = AccessGuard.requireTenant();
    List<HospitalityTicket> found = status == null
        ? tickets.findByCompanyIdOrderByOpenedAtDesc(companyId)
        : tickets.findByCompanyIdAndStatusOrderByOpenedAtDesc(companyId, status);
    return found.stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public TicketResponse get(UUID ticketId) {
    UUID companyId = AccessGuard.requireTenant();
    return toResponse(require(ticketId, companyId));
  }

  @Transactional
  public TicketResponse open(CreateTicketRequest request) {
    NeiverPrincipal actor = AccessGuard.current();
    UUID companyId = AccessGuard.requireTenant();
    UUID reservationId = request.reservationId();
    UUID resourceId = request.resourceId();
    if (reservationId != null) {
      var reservation = reservations.findByIdAndCompanyId(reservationId, companyId)
          .orElseThrow(() -> ApiException.notFound("Reserva no encontrada."));
      if (resourceId == null) {
        resourceId = reservation.getResourceId();
      }
    }
    if (resourceId != null) {
      resources.findByIdAndCompanyId(resourceId, companyId)
          .orElseThrow(() -> ApiException.notFound("Mesa no encontrada."));
    }
    HospitalityTicket ticket = new HospitalityTicket(
        companyId,
        reservationId,
        resourceId,
        request.label(),
        actor.userId(),
        clock.instant());
    if (StringUtils.hasText(request.notes())) {
      ticket.setNotes(request.notes());
    }
    tickets.save(ticket);
    return toResponse(ticket);
  }

  @Transactional
  public TicketResponse addLine(UUID ticketId, AddLineRequest request) {
    UUID companyId = AccessGuard.requireTenant();
    HospitalityTicket ticket = requireOpen(ticketId, companyId);
    lines.save(new HospitalityTicketLine(
        ticket.getId(),
        companyId,
        request.description(),
        request.quantity(),
        request.unitPriceEur()));
    return toResponse(ticket);
  }

  @Transactional
  public TicketResponse pay(UUID ticketId, PayTicketRequest request) {
    NeiverPrincipal actor = AccessGuard.current();
    UUID companyId = AccessGuard.requireTenant();
    HospitalityTicket ticket = requireOpen(ticketId, companyId);
    if (lines.findByTicketIdOrderByCreatedAtAsc(ticketId).isEmpty()) {
      throw ApiException.badRequest("No se puede cobrar un ticket sin líneas.");
    }
    ticket.setStatus(TicketStatus.PAID);
    ticket.setPaymentMethod(request.paymentMethod());
    ticket.setClosedBy(actor.userId());
    ticket.setClosedAt(clock.instant());
    if (StringUtils.hasText(request.notes())) {
      ticket.setNotes(request.notes());
    }
    tickets.save(ticket);
    return toResponse(ticket);
  }

  @Transactional
  public TicketResponse cancel(UUID ticketId) {
    NeiverPrincipal actor = AccessGuard.current();
    UUID companyId = AccessGuard.requireTenant();
    HospitalityTicket ticket = requireOpen(ticketId, companyId);
    ticket.setStatus(TicketStatus.CANCELLED);
    ticket.setClosedBy(actor.userId());
    ticket.setClosedAt(clock.instant());
    tickets.save(ticket);
    return toResponse(ticket);
  }

  private HospitalityTicket require(UUID ticketId, UUID companyId) {
    return tickets.findByIdAndCompanyId(ticketId, companyId)
        .orElseThrow(() -> ApiException.notFound("Ticket no encontrado."));
  }

  private HospitalityTicket requireOpen(UUID ticketId, UUID companyId) {
    HospitalityTicket ticket = require(ticketId, companyId);
    if (ticket.getStatus() != TicketStatus.OPEN) {
      throw ApiException.badRequest("Ese ticket ya está cerrado.");
    }
    return ticket;
  }

  private TicketResponse toResponse(HospitalityTicket ticket) {
    return TicketResponse.from(ticket, lines.findByTicketIdOrderByCreatedAtAsc(ticket.getId()));
  }
}
