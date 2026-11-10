package com.neiverlabs.identity.service;

import com.neiverlabs.identity.web.AuthResponse;
import com.neiverlabs.identity.web.CurrentUserResponse;
import java.time.Instant;

/**
 * La clave de renovación sale de aquí hacia la cookie HttpOnly.
 * No forma parte de {@link AuthResponse}, que es lo que ve el navegador en JSON.
 */
public record IssuedSession(
    String accessToken,
    Instant accessTokenExpiresAt,
    String refreshToken,
    CurrentUserResponse user
) {

  public AuthResponse response() {
    return new AuthResponse(accessToken, accessTokenExpiresAt, user);
  }
}
