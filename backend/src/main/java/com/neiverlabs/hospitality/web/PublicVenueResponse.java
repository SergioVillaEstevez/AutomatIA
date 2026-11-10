package com.neiverlabs.hospitality.web;

import java.util.List;

public record PublicVenueResponse(
    String slug,
    String tradeName,
    String note,
    List<PublicResourceResponse> resources
) {
  public record PublicResourceResponse(String id, String name, int seats) {
  }
}
