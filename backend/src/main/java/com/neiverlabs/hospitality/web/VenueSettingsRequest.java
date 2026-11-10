package com.neiverlabs.hospitality.web;

import com.neiverlabs.shared.web.Text;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VenueSettingsRequest(
    @NotBlank
    @Size(max = 80)
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "El slug solo admite minúsculas, números y guiones")
    String publicSlug,
    @NotNull Boolean publicBookingEnabled,
    @Size(max = 500) String publicBookingNote,
    @Min(400) @Max(2000) Integer floorWidth,
    @Min(300) @Max(1600) Integer floorHeight
) {
  public VenueSettingsRequest {
    publicSlug = Text.trim(publicSlug);
    publicBookingNote = Text.trim(publicBookingNote);
  }
}
