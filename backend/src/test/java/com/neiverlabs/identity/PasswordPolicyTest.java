package com.neiverlabs.identity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neiverlabs.identity.security.PasswordPolicy;
import com.neiverlabs.shared.error.ApiException;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

  private final PasswordPolicy policy = new PasswordPolicy();

  @Test
  void acceptsALongMixedPassword() {
    assertThatCode(() -> policy.check("Almacen-Toledo-12", "ana@empresa.test")).doesNotThrowAnyException();
  }

  @Test
  void rejectsShortPasswordsEmailsAndBcryptOverflow() {
    assertThatThrownBy(() -> policy.check("corta1", "ana@empresa.test")).isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> policy.check("sololetraslargas", "ana@empresa.test")).isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> policy.check("123456789012", "ana@empresa.test")).isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> policy.check("ana@empresa.test", "ana@empresa.test")).isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> policy.check("A".repeat(73), "ana@empresa.test")).isInstanceOf(ApiException.class);
  }
}
