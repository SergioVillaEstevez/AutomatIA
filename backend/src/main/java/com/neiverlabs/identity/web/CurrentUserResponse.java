package com.neiverlabs.identity.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.neiverlabs.identity.domain.Role;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)

public record CurrentUserResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    Role role,
    UUID companyId,
    String companyName,
    boolean passwordChangeRequired
) {
}
