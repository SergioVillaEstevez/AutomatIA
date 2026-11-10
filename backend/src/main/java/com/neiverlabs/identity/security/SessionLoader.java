package com.neiverlabs.identity.security;

import com.neiverlabs.company.domain.Company;
import com.neiverlabs.company.domain.CompanyStatus;
import com.neiverlabs.company.repo.CompanyRepository;
import com.neiverlabs.identity.domain.Membership;
import com.neiverlabs.identity.domain.Role;
import com.neiverlabs.identity.domain.UserAccount;
import com.neiverlabs.identity.domain.UserStatus;
import com.neiverlabs.identity.repo.MembershipRepository;
import com.neiverlabs.identity.repo.UserAccountRepository;
import com.neiverlabs.shared.error.ApiException;
import com.neiverlabs.tenant.RlsSession;
import com.neiverlabs.tenant.TenantContext;
import com.neiverlabs.tenant.TenantSnapshot;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionLoader {

  private final UserAccountRepository users;
  private final MembershipRepository memberships;
  private final CompanyRepository companies;
  private final RlsSession rlsSession;

  public SessionLoader(
      UserAccountRepository users,
      MembershipRepository memberships,
      CompanyRepository companies,
      RlsSession rlsSession
  ) {
    this.users = users;
    this.memberships = memberships;
    this.companies = companies;
    this.rlsSession = rlsSession;
  }

  @Transactional(readOnly = true)
  public AuthenticatedSession load(UUID userId, int sessionVersion) {
    rlsSession.sync();
    UserAccount user = users.findById(userId).orElseThrow(InvalidAccessTokenException::new);
    if (user.getStatus() == UserStatus.DISABLED) {
      throw ApiException.accountDisabled();
    }
    Membership membership = memberships.findByUserId(userId).orElseThrow(InvalidAccessTokenException::new);
    if (membership.getRole() != Role.SUPER_ADMIN) {
      TenantContext.set(TenantSnapshot.company(user.getId(), membership.getCompanyId()));
      rlsSession.sync();
      Company company = companies.findById(membership.getCompanyId()).orElseThrow(InvalidAccessTokenException::new);
      if (company.getStatus() == CompanyStatus.SUSPENDED) {
        throw ApiException.companySuspended();
      }
    }
    if (user.getSessionVersion() != sessionVersion) {
      throw new InvalidAccessTokenException();
    }
    String fullName = user.getFirstName() + " " + user.getLastName();
    if (membership.getRole() == Role.SUPER_ADMIN) {
      TenantContext.set(TenantSnapshot.bypass(user.getId()));
      rlsSession.sync();
      NeiverPrincipal principal = new NeiverPrincipal(
          user.getId(),
          user.getEmail(),
          fullName,
          Role.SUPER_ADMIN,
          null,
          null,
          user.isPasswordChangeRequired());
      return new AuthenticatedSession(principal, TenantSnapshot.bypass(user.getId()));
    }
    Company company = companies.findById(membership.getCompanyId()).orElseThrow(InvalidAccessTokenException::new);
    NeiverPrincipal principal = new NeiverPrincipal(
        user.getId(),
        user.getEmail(),
        fullName,
        membership.getRole(),
        company.getId(),
        company.getTradeName(),
        user.isPasswordChangeRequired());
    return new AuthenticatedSession(principal, TenantSnapshot.company(user.getId(), company.getId()));
  }

  public record AuthenticatedSession(NeiverPrincipal principal, TenantSnapshot snapshot) {
  }
}
