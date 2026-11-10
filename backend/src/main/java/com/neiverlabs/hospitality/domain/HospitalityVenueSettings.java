package com.neiverlabs.hospitality.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospitality_venue_settings")
public class HospitalityVenueSettings {

  @Id
  @Column(name = "company_id")
  private UUID companyId;

  @Column(name = "public_slug", nullable = false, length = 80)
  private String publicSlug;

  @Column(name = "public_booking_enabled", nullable = false)
  private boolean publicBookingEnabled;

  @Column(name = "public_booking_note", length = 500)
  private String publicBookingNote;

  @Column(name = "floor_width", nullable = false)
  private int floorWidth;

  @Column(name = "floor_height", nullable = false)
  private int floorHeight;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected HospitalityVenueSettings() {
  }

  public HospitalityVenueSettings(UUID companyId, String publicSlug) {
    this.companyId = companyId;
    this.publicSlug = publicSlug;
    this.publicBookingEnabled = false;
    this.floorWidth = 900;
    this.floorHeight = 600;
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getCompanyId() {
    return companyId;
  }

  public String getPublicSlug() {
    return publicSlug;
  }

  public void setPublicSlug(String publicSlug) {
    this.publicSlug = publicSlug;
  }

  public boolean isPublicBookingEnabled() {
    return publicBookingEnabled;
  }

  public void setPublicBookingEnabled(boolean publicBookingEnabled) {
    this.publicBookingEnabled = publicBookingEnabled;
  }

  public String getPublicBookingNote() {
    return publicBookingNote;
  }

  public void setPublicBookingNote(String publicBookingNote) {
    this.publicBookingNote = publicBookingNote;
  }

  public int getFloorWidth() {
    return floorWidth;
  }

  public void setFloorWidth(int floorWidth) {
    this.floorWidth = floorWidth;
  }

  public int getFloorHeight() {
    return floorHeight;
  }

  public void setFloorHeight(int floorHeight) {
    this.floorHeight = floorHeight;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
