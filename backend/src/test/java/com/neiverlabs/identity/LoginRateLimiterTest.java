package com.neiverlabs.identity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neiverlabs.config.SecurityProperties;
import com.neiverlabs.identity.security.LoginRateLimiter;
import com.neiverlabs.shared.error.ApiException;
import com.neiverlabs.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

  @Test
  void blocksTheEleventhAttemptAndOpensANewWindowLater() {
    SecurityProperties properties = new SecurityProperties();
    properties.setLoginAttemptsPerMinute(10);
    MutableClock clock = new MutableClock(Instant.parse("2026-03-01T10:00:00Z"));
    LoginRateLimiter limiter = new LoginRateLimiter(clock, properties);

    for (int attempt = 0; attempt < 10; attempt++) {
      assertThatCode(() -> limiter.check("203.0.113.10")).doesNotThrowAnyException();
    }
    assertThatThrownBy(() -> limiter.check("203.0.113.10")).isInstanceOf(ApiException.class);
    assertThatCode(() -> limiter.check("203.0.113.11")).doesNotThrowAnyException();

    clock.advance(Duration.ofSeconds(61));
    assertThatCode(() -> limiter.check("203.0.113.10")).doesNotThrowAnyException();
  }
}
