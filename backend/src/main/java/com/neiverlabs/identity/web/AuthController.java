package com.neiverlabs.identity.web;

import com.neiverlabs.config.SecurityProperties;
import com.neiverlabs.identity.security.RefreshCookie;
import com.neiverlabs.identity.security.RequestContext;
import com.neiverlabs.identity.service.AuthService;
import com.neiverlabs.identity.service.IssuedSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;
  private final SecurityProperties securityProperties;

  public AuthController(AuthService authService, SecurityProperties securityProperties) {
    this.authService = authService;
    this.securityProperties = securityProperties;
  }

  @PostMapping("/login")
  public AuthResponse login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse
  ) {
    IssuedSession session = authService.login(request, RequestContext.from(httpRequest));
    writeRefreshCookie(httpResponse, session.refreshToken());
    return session.response();
  }

  @PostMapping("/refresh")
  public AuthResponse refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    IssuedSession session = authService.refresh(RefreshCookie.read(httpRequest), RequestContext.from(httpRequest));
    writeRefreshCookie(httpResponse, session.refreshToken());
    return session.response();
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    try {
      authService.logout(RefreshCookie.read(httpRequest));
    } finally {
      RefreshCookie.clear(httpResponse, securityProperties.isCookieSecure());
    }
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/change-password")
  public AuthResponse changePassword(
      @Valid @RequestBody ChangePasswordRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse
  ) {
    IssuedSession session = authService.changePassword(request, RequestContext.from(httpRequest));
    writeRefreshCookie(httpResponse, session.refreshToken());
    return session.response();
  }

  @GetMapping("/me")
  public CurrentUserResponse me() {
    return authService.me();
  }

  private void writeRefreshCookie(HttpServletResponse response, String refreshToken) {
    RefreshCookie.write(
        response,
        refreshToken,
        Duration.ofDays(securityProperties.getRefreshTokenDays()),
        securityProperties.isCookieSecure());
  }
}
