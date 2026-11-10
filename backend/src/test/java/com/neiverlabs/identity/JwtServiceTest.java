package com.neiverlabs.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neiverlabs.config.SecurityProperties;
import com.neiverlabs.identity.security.InvalidAccessTokenException;
import com.neiverlabs.identity.security.JwtService;
import com.neiverlabs.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  @Test
  void issuesATokenBoundToTheSessionVersionAndRejectsItAfterExpiry() {
    SecurityProperties properties = new SecurityProperties();
    properties.setJwtSecret("test-jwt-secret-neiver-labs-32bytes");
    properties.setAccessTokenMinutes(15);
    MutableClock clock = new MutableClock(Instant.parse("2026-03-01T10:00:00Z"));
    JwtService jwtService = new JwtService(properties, clock);
    UUID userId = UUID.randomUUID();

    JwtService.IssuedAccess issued = jwtService.issue(userId, 4);
    JwtService.AccessToken parsed = jwtService.parse(issued.token());

    assertThat(parsed.userId()).isEqualTo(userId);
    assertThat(parsed.sessionVersion()).isEqualTo(4);
    assertThat(issued.token()).doesNotContain(userId + "role");

    clock.advance(Duration.ofMinutes(20));
    assertThatThrownBy(() -> jwtService.parse(issued.token())).isInstanceOf(InvalidAccessTokenException.class);
  }
}
