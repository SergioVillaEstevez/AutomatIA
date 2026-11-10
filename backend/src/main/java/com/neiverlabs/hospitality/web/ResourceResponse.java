package com.neiverlabs.hospitality.web;

import com.neiverlabs.hospitality.domain.HospitalityResource;
import java.time.Instant;
import java.util.UUID;

public record ResourceResponse(
    UUID id,
    String name,
    int seats,
    boolean active,
    int sortOrder,
    Instant createdAt,
    Instant updatedAt
) {
  public static ResourceResponse from(HospitalityResource resource) {
    return new ResourceResponse(
        resource.getId(),
        resource.getName(),
        resource.getSeats(),
        resource.isActive(),
        resource.getSortOrder(),
        resource.getCreatedAt(),
        resource.getUpdatedAt());
  }
}
