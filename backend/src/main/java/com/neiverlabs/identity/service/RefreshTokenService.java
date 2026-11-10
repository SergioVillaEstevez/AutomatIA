package com.neiverlabs.identity.service;

import com.neiverlabs.config.SecurityProperties;
import com.neiverlabs.identity.domain.RefreshToken;
import com.neiverlabs.identity.repo.RefreshTokenRepository;
import com.neiverlabs.identity.security.RequestContext;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

  private final RefreshTokenRepository repository;
  private final SecurityProperties properties;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  public RefreshTokenService(RefreshTokenRepository repository, SecurityProperties properties, Clock clock) {
    this.repository = repository;
    this.properties = properties;
    this.clock = clock;
  }

  public String hash(String rawToken) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 no está disponible", exception);
    }
  }

  public String issue(UUID userId, UUID familyId, RequestContext context) {
    String raw = newSecret();
    RefreshToken token = new RefreshToken(
        userId,
        hash(raw),
        familyId,
        clock.instant().plus(properties.getRefreshTokenDays(), ChronoUnit.DAYS),
        context.ip(),
        context.userAgent());
    repository.save(token);
    return raw;
  }

  public String rotate(RefreshToken current, RequestContext context) {
    String raw = newSecret();
    UUID replacementId = UUID.randomUUID();
    RefreshToken replacement = new RefreshToken(
        current.getUserId(),
        hash(raw),
        current.getFamilyId(),
        clock.instant().plus(properties.getRefreshTokenDays(), ChronoUnit.DAYS),
        context.ip(),
        context.userAgent());
    replacement.setId(replacementId);
    current.setRevokedAt(clock.instant());
    current.setReplacedBy(replacementId);
    repository.save(current);
    repository.save(replacement);
    return raw;
  }

  private String newSecret() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public Instant now() {
    return clock.instant();
  }
}
