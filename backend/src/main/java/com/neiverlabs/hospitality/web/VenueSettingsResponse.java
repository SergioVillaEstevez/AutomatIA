package com.neiverlabs.hospitality.web;

import com.neiverlabs.hospitality.domain.HospitalityVenueSettings;
import java.time.Instant;
import java.util.UUID;

public record VenueSettingsResponse(
    UUID companyId,
    String publicSlug,
    boolean publicBookingEnabled,
    String publicBookingNote,
    int floorWidth,
    int floorHeight,
    String publicPath,
    Instant updatedAt
) {
  public static VenueSettingsResponse from(HospitalityVenueSettings settings) {
    return new VenueSettingsResponse(
        settings.getCompanyId(),
        settings.getPublicSlug(),
        settings.isPublicBookingEnabled(),
        settings.getPublicBookingNote(),
        settings.getFloorWidth(),
        settings.getFloorHeight(),
        "/reservar/" + settings.getPublicSlug(),
        settings.getUpdatedAt());
  }
}
