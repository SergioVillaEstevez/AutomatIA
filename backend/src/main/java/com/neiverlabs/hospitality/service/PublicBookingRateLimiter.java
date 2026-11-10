package com.neiverlabs.hospitality.service;

import com.neiverlabs.shared.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Límite en memoria por IP para el widget público (un nodo). */
@Component
public class PublicBookingRateLimiter {

  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public PublicBookingRateLimiter(Clock clock) {
    this.clock = clock;
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
    if (seen[0] > 20) {
      throw ApiException.rateLimited();
    }
  }

  private record Window(Instant start, int count) {
  }
}
