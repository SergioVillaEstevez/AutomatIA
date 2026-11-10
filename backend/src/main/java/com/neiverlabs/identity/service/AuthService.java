package com.neiverlabs.identity.service;

import com.neiverlabs.audit.service.AuditActions;
import com.neiverlabs.audit.service.AuditService;
import com.neiverlabs.company.domain.Company;
import com.neiverlabs.company.domain.CompanyStatus;
import com.neiverlabs.company.repo.CompanyRepository;
import com.neiverlabs.config.SecurityProperties;
import com.neiverlabs.identity.domain.Membership;
import com.neiverlabs.identity.domain.RefreshToken;
import com.neiverlabs.identity.domain.Role;
import com.neiverlabs.identity.domain.UserAccount;
import com.neiverlabs.identity.domain.UserStatus;
import com.neiverlabs.identity.repo.MembershipRepository;
import com.neiverlabs.identity.repo.RefreshTokenRepository;
import com.neiverlabs.identity.repo.UserAccountRepository;
import com.neiverlabs.identity.security.AccessGuard;
import com.neiverlabs.identity.security.JwtService;
import com.neiverlabs.identity.security.LoginRateLimiter;
import com.neiverlabs.identity.security.NeiverPrincipal;
import com.neiverlabs.identity.security.PasswordPolicy;
import com.neiverlabs.identity.security.RequestContext;
import com.neiverlabs.identity.web.ChangePasswordRequest;
import com.neiverlabs.identity.web.CurrentUserResponse;
import com.neiverlabs.identity.web.LoginRequest;
import com.neiverlabs.shared.error.ApiException;
import com.neiverlabs.shared.web.Text;
import com.neiverlabs.tenant.RlsSession;
import com.neiverlabs.tenant.TenantContext;
import com.neiverlabs.tenant.TenantSnapshot;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserAccountRepository users;
  private final MembershipRepository memberships;
  private final CompanyRepository companies;
  private final RefreshTokenRepository refreshTokens;
  private final RefreshTokenService refreshTokenService;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;
  private final PasswordPolicy passwordPolicy;
  private final LoginRateLimiter rateLimiter;
  private final AuditService audit;
  private final RlsSession rlsSession;
  private final SecurityProperties properties;
  private final String dummyHash;

  public AuthService(
      UserAccountRepository users,
      MembershipRepository memberships,
      CompanyRepository companies,
      RefreshTokenRepository refreshTokens,
      RefreshTokenService refreshTokenService,
      JwtService jwtService,
      PasswordEncoder passwordEncoder,
      PasswordPolicy passwordPolicy,
      LoginRateLimiter rateLimiter,
      AuditService audit,
      RlsSession rlsSession,
      SecurityProperties properties
  ) {
    this.users = users;
    this.memberships = memberships;
    this.companies = companies;
    this.refreshTokens = refreshTokens;
    this.refreshTokenService = refreshTokenService;
    this.jwtService = jwtService;
    this.passwordEncoder = passwordEncoder;
    this.passwordPolicy = passwordPolicy;
    this.rateLimiter = rateLimiter;
    this.audit = audit;
    this.rlsSession = rlsSession;
    this.properties = properties;
    this.dummyHash = passwordEncoder.encode("dummy-password-not-used");
  }

  @Transactional(noRollbackFor = ApiException.class)
  public IssuedSession login(LoginRequest request, RequestContext context) {
    rateLimiter.check(context.ip());
    TenantContext.set(TenantSnapshot.forLoginLookup());
    rlsSession.sync();

    String email = Text.email(request.email());
    UserAccount user = users.findByEmail(email).orElse(null);
    if (user == null) {
      passwordEncoder.matches(request.password(), dummyHash);
      audit.record(AuditActions.LOGIN_FAILURE, "User", null, null, null,
          Map.of("reason", "UNKNOWN_USER", "email", email), context);
      throw ApiException.invalidCredentials();
    }

    TenantContext.set(TenantSnapshot.userOnly(user.getId()));
    rlsSession.sync();
    Membership membership = memberships.findByUserId(user.getId())
        .orElseThrow(ApiException::invalidCredentials);
    applyMembershipContext(user, membership);

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      if (!user.isTemporarilyLocked(refreshTokenService.now())) {
        user.registerFailedLogin(
            refreshTokenService.now(), properties.getMaxFailedLogins(), properties.getLockMinutes());
      }
      audit.record(AuditActions.LOGIN_FAILURE, "User", user.getId(), null, membership.getCompanyId(),
          Map.of("reason", "BAD_PASSWORD", "email", email), context);
      throw ApiException.invalidCredentials();
    }
    if (user.isTemporarilyLocked(refreshTokenService.now())) {
      audit.record(AuditActions.LOGIN_FAILURE, "User", user.getId(), user.getId(), membership.getCompanyId(),
          Map.of("reason", "LOCKED", "email", email), context);
      throw ApiException.accountLocked();
    }
    if (user.getStatus() == UserStatus.DISABLED) {
      audit.record(AuditActions.LOGIN_FAILURE, "User", user.getId(), user.getId(), membership.getCompanyId(),
          Map.of("reason", "DISABLED", "email", email), context);
      throw ApiException.accountDisabled();
    }

    String companyName = null;
    if (membership.getRole() != Role.SUPER_ADMIN) {
      Company company = companies.findById(membership.getCompanyId())
          .orElseThrow(ApiException::invalidCredentials);
      if (company.getStatus() == CompanyStatus.SUSPENDED) {
        audit.record(AuditActions.LOGIN_FAILURE, "Company", company.getId(), user.getId(), company.getId(),
            Map.of("reason", "COMPANY_SUSPENDED"), context);
        throw ApiException.companySuspended();
      }
      companyName = company.getTradeName();
    }

    user.registerSuccessfulLogin(refreshTokenService.now());
    IssuedSession session = openSession(user, membership, companyName, context);
    audit.record(AuditActions.LOGIN_SUCCESS, "User", user.getId(), user.getId(), membership.getCompanyId(),
        Map.of(), context);
    return session;
  }

  @Transactional(noRollbackFor = ApiException.class)
  public IssuedSession refresh(String rawToken, RequestContext context) {
    if (rawToken == null || rawToken.isBlank()) {
      throw ApiException.invalidCredentials();
    }
    TenantContext.set(TenantSnapshot.forTokenLookup());
    rlsSession.sync();
    RefreshToken stored = refreshTokens.findByTokenHash(refreshTokenService.hash(rawToken))
        .orElseThrow(ApiException::invalidCredentials);
    if (stored.getRevokedAt() != null) {
      reuseDetected(stored, context);
      throw ApiException.invalidCredentials();
    }
    if (stored.getExpiresAt().isBefore(refreshTokenService.now())) {
      throw ApiException.invalidCredentials();
    }

    TenantContext.set(TenantSnapshot.userOnly(stored.getUserId()));
    rlsSession.sync();
    UserAccount user = users.findById(stored.getUserId()).orElseThrow(ApiException::invalidCredentials);
    if (user.getStatus() == UserStatus.DISABLED) {
      throw ApiException.accountDisabled();
    }
    Membership membership = memberships.findByUserId(user.getId()).orElseThrow(ApiException::invalidCredentials);
    applyMembershipContext(user, membership);
    String companyName = ensureCompanyIsUsable(user, membership, context);

    String replacement = refreshTokenService.rotate(stored, context);
    JwtService.IssuedAccess access = jwtService.issue(user.getId(), user.getSessionVersion());
    audit.record(AuditActions.REFRESH, "User", user.getId(), user.getId(), membership.getCompanyId(),
        Map.of(), context);
    return new IssuedSession(
        access.token(),
        access.expiresAt(),
        replacement,
        currentUser(user, membership, companyName));
  }

  @Transactional
  public void logout(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return;
    }
    TenantContext.set(TenantSnapshot.forTokenLookup());
    rlsSession.sync();
    RefreshToken stored = refreshTokens.findByTokenHash(refreshTokenService.hash(rawToken)).orElse(null);
    if (stored == null) {
      return;
    }
    TenantContext.set(TenantSnapshot.userOnly(stored.getUserId()));
    rlsSession.sync();
    UserAccount user = users.findById(stored.getUserId()).orElse(null);
    if (user == null) {
      return;
    }
    Membership membership = memberships.findByUserId(user.getId()).orElse(null);
    user.bumpSession();
    users.saveAndFlush(user);
    applyMembershipContext(user, membership);
    audit.record(
        AuditActions.LOGOUT,
        "User",
        user.getId(),
        user.getId(),
        membership == null ? null : membership.getCompanyId(),
        Map.of(),
        RequestContext.none());
    refreshTokens.revokeActiveForUser(user.getId(), refreshTokenService.now());
  }

  @Transactional
  public IssuedSession changePassword(ChangePasswordRequest request, RequestContext context) {
    NeiverPrincipal principal = AccessGuard.current();
    rlsSession.sync();
    UserAccount user = users.findById(principal.userId()).orElseThrow(ApiException::invalidCredentials);
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw ApiException.invalidCredentials();
    }
    passwordPolicy.check(request.newPassword(), user.getEmail());
    if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
      throw ApiException.validation("La clave nueva tiene que ser distinta de la actual.");
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    user.setPasswordChangeRequired(false);
    user.clearLock();
    user.bumpSession();
    users.saveAndFlush(user);
    refreshTokens.revokeActiveForUser(user.getId(), refreshTokenService.now());

    TenantContext.set(principal.companyId() == null
        ? TenantSnapshot.bypass(user.getId())
        : TenantSnapshot.company(user.getId(), principal.companyId()));
    rlsSession.sync();
    user = users.findById(user.getId()).orElseThrow(ApiException::invalidCredentials);
    Membership membership = memberships.findByUserId(user.getId()).orElseThrow(ApiException::invalidCredentials);
    IssuedSession session = openSession(user, membership, principal.companyName(), context);
    audit.record(AuditActions.PASSWORD_CHANGED, "User", user.getId(), user.getId(), membership.getCompanyId(),
        Map.of(), context);
    return session;
  }

  @Transactional(readOnly = true)
  public CurrentUserResponse me() {
    NeiverPrincipal principal = AccessGuard.current();
    UserAccount user = users.findById(principal.userId())
        .orElseThrow(() -> ApiException.notFound("No encontramos el usuario de la sesión."));
    return new CurrentUserResponse(
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        principal.role(),
        principal.companyId(),
        principal.companyName(),
        user.isPasswordChangeRequired());
  }

  private void reuseDetected(RefreshToken stored, RequestContext context) {
    TenantContext.set(TenantSnapshot.userOnly(stored.getUserId()));
    rlsSession.sync();
    UserAccount user = users.findById(stored.getUserId()).orElse(null);
    Membership membership = memberships.findByUserId(stored.getUserId()).orElse(null);
    if (user != null) {
      user.bumpSession();
      users.saveAndFlush(user);
      if (membership != null) {
        applyMembershipContext(user, membership);
      }
    }
    audit.record(AuditActions.REFRESH_REUSE, "User", stored.getUserId(), stored.getUserId(),
        membership == null ? null : membership.getCompanyId(),
        Map.of("familyId", stored.getFamilyId().toString()), context);
    refreshTokens.revokeFamily(stored.getFamilyId(), refreshTokenService.now());
  }

  private void applyMembershipContext(UserAccount user, Membership membership) {
    if (membership == null || membership.getRole() == Role.SUPER_ADMIN) {
      TenantContext.set(TenantSnapshot.bypass(user.getId()));
    } else {
      TenantContext.set(TenantSnapshot.company(user.getId(), membership.getCompanyId()));
    }
    rlsSession.sync();
  }

  private String ensureCompanyIsUsable(UserAccount user, Membership membership, RequestContext context) {
    if (membership.getRole() == Role.SUPER_ADMIN) {
      return null;
    }
    Company company = companies.findById(membership.getCompanyId()).orElseThrow(ApiException::invalidCredentials);
    if (company.getStatus() == CompanyStatus.SUSPENDED) {
      audit.record(AuditActions.LOGIN_FAILURE, "Company", company.getId(), user.getId(), company.getId(),
          Map.of("reason", "COMPANY_SUSPENDED"), context);
      throw ApiException.companySuspended();
    }
    return company.getTradeName();
  }

  private IssuedSession openSession(
      UserAccount user,
      Membership membership,
      String companyName,
      RequestContext context
  ) {
    String refresh = refreshTokenService.issue(user.getId(), UUID.randomUUID(), context);
    JwtService.IssuedAccess access = jwtService.issue(user.getId(), user.getSessionVersion());
    return new IssuedSession(access.token(), access.expiresAt(), refresh, currentUser(user, membership, companyName));
  }

  private static CurrentUserResponse currentUser(UserAccount user, Membership membership, String companyName) {
    return new CurrentUserResponse(
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        membership.getRole(),
        membership.getCompanyId(),
        companyName,
        user.isPasswordChangeRequired());
  }
}
