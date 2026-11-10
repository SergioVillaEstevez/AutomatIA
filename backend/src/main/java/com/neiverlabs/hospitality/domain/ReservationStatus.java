package com.neiverlabs.hospitality.domain;

public enum ReservationStatus {
  PENDING,
  CONFIRMED,
  SEATED,
  COMPLETED,
  CANCELLED,
  NO_SHOW;

  public boolean occupiesSlot() {
    return this == PENDING || this == CONFIRMED || this == SEATED;
  }
}
