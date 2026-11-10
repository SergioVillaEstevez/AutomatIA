package com.neiverlabs.hospitality.web;

import java.util.List;

public record FloorPlanResponse(
    int floorWidth,
    int floorHeight,
    String day,
    List<ResourceResponse> resources,
    List<ReservationResponse> openReservations
) {
}
