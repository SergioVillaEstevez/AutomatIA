package com.neiverlabs.identity.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

public final class RefreshCookie {

  public static final String NAME = "neiver_refresh";
  public static final String PATH = "/api/v1/auth";

  private RefreshCookie() {
  }

  public static void write(HttpServletResponse response, String token, Duration maxAge, boolean secure) {
    ResponseCookie cookie = ResponseCookie.from(NAME, token)
        .httpOnly(true)
        .secure(secure)
        .sameSite("Strict")
        .path(PATH)
        .maxAge(maxAge)
        .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  public static void clear(HttpServletResponse response, boolean secure) {
    write(response, "", Duration.ZERO, secure);
  }

  public static String read(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
