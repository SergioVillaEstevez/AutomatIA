package com.neiverlabs.bootstrap;

import com.neiverlabs.audit.service.AuditActions;
import com.neiverlabs.audit.service.AuditService;
import com.neiverlabs.config.SecurityProperties;
import com.neiverlabs.identity.domain.Membership;
import com.neiverlabs.identity.domain.Role;
import com.neiverlabs.identity.domain.UserAccount;
import com.neiverlabs.identity.repo.MembershipRepository;
import com.neiverlabs.identity.repo.UserAccountRepository;
import com.neiverlabs.identity.security.PasswordPolicy;
import com.neiverlabs.identity.security.RequestContext;
import com.neiverlabs.shared.web.Text;
import com.neiverlabs.tenant.RlsSession;
import com.neiverlabs.tenant.TenantContext;
import com.neiverlabs.tenant.TenantSnapshot;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuperAdminCreator {

  private static final Logger log = LoggerFactory.getLogger(SuperAdminCreator.class);

  private final MembershipRepository memberships;
  private final UserAccountRepository users;
  private final PasswordEncoder passwordEncoder;
  private final PasswordPolicy passwordPolicy;
  private final AuditService audit;
  private final RlsSession rlsSession;
  private final SecurityProperties properties;
  private final Environment environment;

  public SuperAdminCreator(
      MembershipRepository memberships,
      UserAccountRepository users,
      PasswordEncoder passwordEncoder,
      PasswordPolicy passwordPolicy,
      AuditService audit,
      RlsSession rlsSession,
      SecurityProperties properties,
      Environment environment
  ) {
    this.memberships = memberships;
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.passwordPolicy = passwordPolicy;
    this.audit = audit;
    this.rlsSession = rlsSession;
    this.properties = properties;
    this.environment = environment;
  }

  @Transactional
  public void createIfMissing() {
    TenantContext.set(TenantSnapshot.bypass(null));
    rlsSession.sync();
    if (memberships.existsByRole(Role.SUPER_ADMIN)) {
      return;
    }
    String email = Text.email(properties.getSuperAdminEmail());
    String password = properties.getSuperAdminPassword();
    if (email == null || email.isBlank() || password == null || password.isBlank()) {
      if (production()) {
        throw new IllegalStateException(
            "No existe un SUPER_ADMIN. Define NEIVER_SUPERADMIN_EMAIL y NEIVER_SUPERADMIN_PASSWORD.");
      }
      log.warn("No se crea el SUPER_ADMIN inicial: faltan NEIVER_SUPERADMIN_EMAIL o NEIVER_SUPERADMIN_PASSWORD.");
      return;
    }
    passwordPolicy.check(password, email);
    Name name = Name.split(properties.getSuperAdminName());
    UserAccount user = new UserAccount(email, passwordEncoder.encode(password), name.first(), name.last());
    users.save(user);
    memberships.save(new Membership(user.getId(), null, Role.SUPER_ADMIN));
    audit.record(AuditActions.USER_CREATED, "User", user.getId(), user.getId(), null,
        Map.of("role", Role.SUPER_ADMIN.name(), "bootstrap", true), RequestContext.none());
    log.info("SUPER_ADMIN inicial creado para {}. Debe cambiar la clave en el primer acceso.", email);
  }

  private boolean production() {
    for (String profile : environment.getActiveProfiles()) {
      if ("prod".equals(profile)) {
        return true;
      }
    }
    return false;
  }

  private record Name(String first, String last) {
    static Name split(String raw) {
      String value = raw == null ? "" : raw.trim();
      if (value.isBlank()) {
        return new Name("Administrador", "Neiver");
      }
      int space = value.indexOf(' ');
      if (space < 0) {
        return new Name(value, "-");
      }
      return new Name(value.substring(0, space), value.substring(space + 1).trim());
    }
  }
}
