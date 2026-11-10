package com.neiverlabs.hospitality.web;

import com.neiverlabs.hospitality.domain.HospitalityTicket;
import com.neiverlabs.hospitality.domain.HospitalityTicketLine;
import com.neiverlabs.hospitality.domain.PaymentMethod;
import com.neiverlabs.hospitality.domain.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketResponse(
    UUID id,
    UUID reservationId,
    UUID resourceId,
    String label,
    TicketStatus status,
    PaymentMethod paymentMethod,
    String notes,
    Instant openedAt,
    Instant closedAt,
    BigDecimal totalEur,
    List<Line> lines
) {
  public record Line(
      UUID id,
      String description,
      BigDecimal quantity,
      BigDecimal unitPriceEur,
      BigDecimal totalEur
  ) {
    public static Line from(HospitalityTicketLine line) {
      return new Line(
          line.getId(),
          line.getDescription(),
          line.getQuantity(),
          line.getUnitPriceEur(),
          line.lineTotal());
    }
  }

  public static TicketResponse from(HospitalityTicket ticket, List<HospitalityTicketLine> lines) {
    BigDecimal total = lines.stream()
        .map(HospitalityTicketLine::lineTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new TicketResponse(
        ticket.getId(),
        ticket.getReservationId(),
        ticket.getResourceId(),
        ticket.getLabel(),
        ticket.getStatus(),
        ticket.getPaymentMethod(),
        ticket.getNotes(),
        ticket.getOpenedAt(),
        ticket.getClosedAt(),
        total,
        lines.stream().map(Line::from).toList());
  }
}
