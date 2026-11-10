package com.neiverlabs.support;

import com.neiverlabs.identity.domain.Membership;
import com.neiverlabs.identity.domain.Role;
import com.neiverlabs.identity.domain.UserAccount;
import com.neiverlabs.identity.repo.MembershipRepository;
import com.neiverlabs.identity.repo.UserAccountRepository;
import com.neiverlabs.tenant.RlsSession;
import com.neiverlabs.tenant.TenantContext;
import com.neiverlabs.tenant.TenantSnapshot;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformFixtures {

  private final UserAccountRepository users;
  private final MembershipRepository memberships;
  private final PasswordEncoder passwordEncoder;
  private final RlsSession rlsSession;

  public PlatformFixtures(
      UserAccountRepository users,
      MembershipRepository memberships,
      PasswordEncoder passwordEncoder,
      RlsSession rlsSession
  ) {
    this.users = users;
    this.memberships = memberships;
    this.passwordEncoder = passwordEncoder;
    this.rlsSession = rlsSession;
  }

  @Transactional
  public UUID superAdmin(String email, String password) {
    TenantContext.set(TenantSnapshot.bypass(null));
    rlsSession.sync();
    return users.findByEmail(email).map(UserAccount::getId).orElseGet(() -> {
      UserAccount user = new UserAccount(email, passwordEncoder.encode(password), "Super", "Admin");
      user.setPasswordChangeRequired(false);
      users.save(user);
      memberships.save(new Membership(user.getId(), null, Role.SUPER_ADMIN));
      return user.getId();
    });
  }
}
