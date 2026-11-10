package com.neiverlabs.identity.security;

import com.neiverlabs.config.SecurityProperties;
import com.neiverlabs.shared.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Límite en memoria para un solo nodo. Con varias instancias haría falta un almacén compartido.
 */
@Component
public class LoginRateLimiter {

  private final Clock clock;
  private final int limit;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public LoginRateLimiter(Clock clock, SecurityProperties properties) {
    this.clock = clock;
    this.limit = properties.getLoginAttemptsPerMinute();
  }

  public void check(String ip) {
    String key = ip == null || ip.isBlank() ? "unknown" : ip;
    Instant now = clock.instant();
    int[] seen = new int[1];
    windows.compute(key, (ignored, current) -> {
      Window next;
      if (current == null || Duration.between(current.start, now).toSeconds() >= 60) {
        next = new Window(now, 1);
      } else {
        next = new Window(current.start, current.count + 1);
      }
      seen[0] = next.count;
      return next;
    });
    if (windows.size() > 10_000) {
      windows.entrySet().removeIf(entry -> Duration.between(entry.getValue().start, now).toMinutes() >= 2);
    }
    if (seen[0] > limit) {
      throw ApiException.rateLimited();
    }
  }

  private record Window(Instant start, int count) {
  }
}
