package com.neiverlabs.hospitality.web;

import java.util.List;

public record OccupancyResponse(
    String day,
    String zone,
    int activeSeats,
    int reservedCovers,
    int openReservations,
    double occupancyRatio,
    List<ReservationResponse> reservations
) {
}
