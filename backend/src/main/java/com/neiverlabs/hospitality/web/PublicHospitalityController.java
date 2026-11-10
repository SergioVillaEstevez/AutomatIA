package com.neiverlabs.hospitality.web;

import com.neiverlabs.hospitality.service.PublicBookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/hospitality/{slug}")
public class PublicHospitalityController {

  private final PublicBookingService bookings;

  public PublicHospitalityController(PublicBookingService bookings) {
    this.bookings = bookings;
  }

  @GetMapping
  public PublicVenueResponse venue(@PathVariable String slug, HttpServletRequest request) {
    return bookings.venue(slug, request.getRemoteAddr());
  }

  @PostMapping("/reservations")
  @ResponseStatus(HttpStatus.CREATED)
  public PublicBookingResponse book(
      @PathVariable String slug,
      @Valid @RequestBody PublicBookingRequest body,
      HttpServletRequest request
  ) {
    return bookings.book(slug, body, request.getRemoteAddr());
  }
}
