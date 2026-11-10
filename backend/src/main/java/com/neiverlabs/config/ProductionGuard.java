package com.neiverlabs.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class ProductionGuard implements ApplicationRunner {

  private final Environment environment;
  private final SecurityProperties securityProperties;

  public ProductionGuard(Environment environment, SecurityProperties securityProperties) {
    this.environment = environment;
    this.securityProperties = securityProperties;
  }

  @Override
  public void run(ApplicationArguments args) {
    String secret = securityProperties.getJwtSecret();
    if (secret == null || secret.length() < 32) {
      throw new IllegalStateException("NEIVER_JWT_SECRET debe tener al menos 32 caracteres.");
    }
    boolean production = ListHas.profile(environment, "prod");
    if (!production) {
      return;
    }
    if (secret.equals(SecurityProperties.DEV_JWT_SECRET)) {
      throw new IllegalStateException("El perfil prod no puede usar el secreto JWT de desarrollo.");
    }
    if (!securityProperties.isCookieSecure()) {
      throw new IllegalStateException("El perfil prod exige NEIVER_COOKIE_SECURE=true.");
    }
    if (securityProperties.getBcryptStrength() < 10) {
      throw new IllegalStateException("El perfil prod exige un coste BCrypt de al menos 10.");
    }
    if (SecurityProperties.DEV_SUPER_ADMIN_PASSWORD.equals(securityProperties.getSuperAdminPassword())) {
      throw new IllegalStateException("El perfil prod no puede usar la contraseña de administrador de desarrollo.");
    }
  }

  static final class ListHas {
    private ListHas() {
    }

    static boolean profile(Environment environment, String profile) {
      for (String active : environment.getActiveProfiles()) {
        if (profile.equals(active)) {
          return true;
        }
      }
      return false;
    }
  }
}
