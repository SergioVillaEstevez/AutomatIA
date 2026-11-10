package com.neiverlabs.hospitality.service;

import com.neiverlabs.hospitality.domain.HospitalityReservation;
import com.neiverlabs.hospitality.domain.HospitalityResource;
import com.neiverlabs.hospitality.repo.HospitalityResourceRepository;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class TemplateRenderer {

  private static final DateTimeFormatter WHEN =
      DateTimeFormatter.ofPattern("d/M/yyyy HH:mm", Locale.forLanguageTag("es-ES"));

  private TemplateRenderer() {
  }

  static String render(
      String template,
      HospitalityReservation reservation,
      ZoneId zone,
      HospitalityResourceRepository resources
  ) {
    String resourceName = reservation.getResourceId() == null
        ? ""
        : resources.findById(reservation.getResourceId()).map(HospitalityResource::getName).orElse("");
    String when = WHEN.format(reservation.getStartsAt().atZone(zone));
    return template
        .replace("{{guestName}}", nullToEmpty(reservation.getGuestName()))
        .replace("{{partySize}}", String.valueOf(reservation.getPartySize()))
        .replace("{{startsAt}}", when)
        .replace("{{status}}", reservation.getStatus().name())
        .replace("{{resourceName}}", resourceName)
        .replace("{{phone}}", nullToEmpty(reservation.getGuestPhone()));
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
