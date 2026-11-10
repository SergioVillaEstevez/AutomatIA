package com.neiverlabs.hospitality.web;

import java.time.Instant;
import java.util.UUID;

public record PublicBookingResponse(
    UUID id,
    String guestName,
    int partySize,
    Instant startsAt,
    Instant endsAt,
    String status,
    String detail
) {
}
