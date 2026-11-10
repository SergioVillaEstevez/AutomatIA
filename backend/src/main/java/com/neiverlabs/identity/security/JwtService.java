package com.neiverlabs.identity.security;

import com.neiverlabs.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

  private final SecurityProperties properties;
  private final Clock clock;
  private final SecretKey key;

  public JwtService(SecurityProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
    this.key = Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
  }

  public IssuedAccess issue(UUID userId, int sessionVersion) {
    Instant issuedAt = clock.instant();
    Instant expiresAt = issuedAt.plus(properties.getAccessTokenMinutes(), ChronoUnit.MINUTES);
    String token = Jwts.builder()
        .subject(userId.toString())
        .claim("sv", sessionVersion)
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(expiresAt))
        .signWith(key)
        .compact();
    return new IssuedAccess(token, expiresAt);
  }

  public AccessToken parse(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(key)
          .clock(() -> Date.from(clock.instant()))
          .clockSkewSeconds(30)
          .build()
          .parseSignedClaims(token)
          .getPayload();
      Object rawVersion = claims.get("sv");
      if (!(rawVersion instanceof Number version)) {
        throw new InvalidAccessTokenException();
      }
      return new AccessToken(UUID.fromString(claims.getSubject()), version.intValue());
    } catch (JwtException | IllegalArgumentException exception) {
      throw new InvalidAccessTokenException();
    }
  }

  public record IssuedAccess(String token, Instant expiresAt) {
  }

  public record AccessToken(UUID userId, int sessionVersion) {
  }
}
