package com.neiverlabs.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserAccount {

  @Id
  private UUID id;

  @Column(nullable = false, length = 320)
  private String email;

  @Column(nullable = false, length = 255)
  private String passwordHash;

  @Column(nullable = false, length = 80)
  private String firstName;

  @Column(nullable = false, length = 120)
  private String lastName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private UserStatus status;

  @Column(nullable = false)
  private boolean passwordChangeRequired;

  @Column(nullable = false)
  private int failedLoginCount;

  private Instant lockedUntil;

  private Instant lastLoginAt;

  @Column(nullable = false)
  private int sessionVersion;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected UserAccount() {
  }

  public UserAccount(String email, String passwordHash, String firstName, String lastName) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.firstName = firstName;
    this.lastName = lastName;
    this.status = UserStatus.ACTIVE;
    this.passwordChangeRequired = true;
    this.failedLoginCount = 0;
    this.sessionVersion = 0;
  }

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public void registerFailedLogin(Instant now, int maximum, int lockMinutes) {
    failedLoginCount++;
    if (failedLoginCount >= maximum) {
      lockedUntil = now.plusSeconds(lockMinutes * 60L);
    }
  }

  public void registerSuccessfulLogin(Instant now) {
    failedLoginCount = 0;
    lockedUntil = null;
    lastLoginAt = now;
  }

  public void clearLock() {
    failedLoginCount = 0;
    lockedUntil = null;
  }

  public boolean isTemporarilyLocked(Instant now) {
    return lockedUntil != null && lockedUntil.isAfter(now);
  }

  public void bumpSession() {
    sessionVersion++;
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public boolean isPasswordChangeRequired() {
    return passwordChangeRequired;
  }

  public void setPasswordChangeRequired(boolean passwordChangeRequired) {
    this.passwordChangeRequired = passwordChangeRequired;
  }

  public int getFailedLoginCount() {
    return failedLoginCount;
  }

  public Instant getLockedUntil() {
    return lockedUntil;
  }

  public void setLockedUntil(Instant lockedUntil) {
    this.lockedUntil = lockedUntil;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public int getSessionVersion() {
    return sessionVersion;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
