package com.neiverlabs.hospitality.web;

import com.neiverlabs.shared.web.Text;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record PublicBookingRequest(
    UUID resourceId,
    @NotBlank @Size(max = 120) String guestName,
    @Size(max = 30) String guestPhone,
    @NotNull @Min(1) @Max(20) Integer partySize,
    @NotNull Instant startsAt,
    Instant endsAt,
    @Size(max = 500) String notes
) {
  public PublicBookingRequest {
    guestName = Text.trim(guestName);
    guestPhone = Text.trim(guestPhone);
    notes = Text.trim(notes);
  }
}
