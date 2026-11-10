package com.neiverlabs.hospitality.web;

import com.neiverlabs.hospitality.domain.ReservationStatus;
import com.neiverlabs.shared.web.Text;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CreateReservationRequest(
    UUID resourceId,
    @NotBlank @Size(max = 120) String guestName,
    @Size(max = 30) String guestPhone,
    @NotNull @Min(1) @Max(50) Integer partySize,
    @NotNull Instant startsAt,
    Instant endsAt,
    ReservationStatus status,
    @Size(max = 500) String notes
) {
  public CreateReservationRequest {
    guestName = Text.trim(guestName);
    guestPhone = Text.trim(guestPhone);
    notes = Text.trim(notes);
  }
}
