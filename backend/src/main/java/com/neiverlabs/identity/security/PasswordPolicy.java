package com.neiverlabs.identity.security;

import com.neiverlabs.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

  private static final Set<String> DENIED = Set.of(
      "password",
      "contraseña",
      "123456789012",
      "qwertyuiop12",
      "neiverlabs");

  public void check(String password, String email) {
    if (password == null || password.length() < 12 || password.length() > 200) {
      throw ApiException.validation("La clave debe tener entre 12 y 200 caracteres.");
    }
    if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
      throw ApiException.validation("La clave no puede superar 72 bytes: BCrypt descartaría el resto.");
    }
    boolean letter = password.codePoints().anyMatch(Character::isLetter);
    boolean digit = password.codePoints().anyMatch(Character::isDigit);
    if (!letter || !digit) {
      throw ApiException.validation("La clave debe incluir al menos una letra y un número.");
    }
    if (DENIED.contains(password.toLowerCase(Locale.ROOT))) {
      throw ApiException.validation("Esa clave es demasiado habitual.");
    }
    if (email != null && password.equalsIgnoreCase(email)) {
      throw ApiException.validation("La clave no puede ser igual al correo.");
    }
  }
}
