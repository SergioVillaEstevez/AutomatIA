package com.neiverlabs.hospitality.web;

import com.neiverlabs.hospitality.domain.HospitalityReservation;
import com.neiverlabs.hospitality.domain.ReservationStatus;
import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
    UUID id,
    UUID resourceId,
    String resourceName,
    String guestName,
    String guestPhone,
    int partySize,
    Instant startsAt,
    Instant endsAt,
    ReservationStatus status,
    String notes,
    Instant createdAt,
    Instant updatedAt
) {
  public static ReservationResponse from(HospitalityReservation reservation, String resourceName) {
    return new ReservationResponse(
        reservation.getId(),
        reservation.getResourceId(),
        resourceName,
        reservation.getGuestName(),
        reservation.getGuestPhone(),
        reservation.getPartySize(),
        reservation.getStartsAt(),
        reservation.getEndsAt(),
        reservation.getStatus(),
        reservation.getNotes(),
        reservation.getCreatedAt(),
        reservation.getUpdatedAt());
  }
}
