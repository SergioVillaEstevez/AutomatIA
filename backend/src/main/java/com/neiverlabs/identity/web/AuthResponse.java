package com.neiverlabs.identity.web;

import java.time.Instant;

public record AuthResponse(
    String accessToken,
    Instant accessTokenExpiresAt,
    CurrentUserResponse user
) {
}
