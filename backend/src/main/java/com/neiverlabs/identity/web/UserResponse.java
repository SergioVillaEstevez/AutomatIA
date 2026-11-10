package com.neiverlabs.identity.web;

import com.neiverlabs.identity.domain.Membership;
import com.neiverlabs.identity.domain.Role;
import com.neiverlabs.identity.domain.UserAccount;
import com.neiverlabs.identity.domain.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    UserStatus status,
    Role role,
    boolean passwordChangeRequired,
    Instant lastLoginAt,
    UUID companyId
) {

  public static UserResponse from(UserAccount user, Membership membership) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getStatus(),
        membership.getRole(),
        user.isPasswordChangeRequired(),
        user.getLastLoginAt(),
        membership.getCompanyId());
  }
}
