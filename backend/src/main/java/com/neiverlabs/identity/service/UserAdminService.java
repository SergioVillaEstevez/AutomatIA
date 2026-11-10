package com.neiverlabs.identity.service;

import com.neiverlabs.audit.service.AuditActions;
import com.neiverlabs.audit.service.AuditService;
import com.neiverlabs.company.repo.CompanyRepository;
import com.neiverlabs.identity.domain.Membership;
import com.neiverlabs.identity.domain.Role;
import com.neiverlabs.identity.domain.UserAccount;
import com.neiverlabs.identity.domain.UserStatus;
import com.neiverlabs.identity.repo.MembershipRepository;
import com.neiverlabs.identity.repo.RefreshTokenRepository;
import com.neiverlabs.identity.repo.UserAccountRepository;
import com.neiverlabs.identity.security.AccessGuard;
import com.neiverlabs.identity.security.NeiverPrincipal;
import com.neiverlabs.identity.security.PasswordPolicy;
import com.neiverlabs.identity.security.RequestContext;
import com.neiverlabs.identity.web.CreateUserRequest;
import com.neiverlabs.identity.web.ResetPasswordRequest;
import com.neiverlabs.identity.web.UpdateUserRequest;
import com.neiverlabs.identity.web.UserResponse;
import com.neiverlabs.shared.error.ApiException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminService {

  private final UserAccountRepository users;
  private final MembershipRepository memberships;
  private final CompanyRepository companies;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordEncoder passwordEncoder;
  private final PasswordPolicy passwordPolicy;
  private final AuditService audit;
  private final Clock clock;

  public UserAdminService(
      UserAccountRepository users,
      MembershipRepository memberships,
      CompanyRepository companies,
      RefreshTokenRepository refreshTokens,
      PasswordEncoder passwordEncoder,
      PasswordPolicy passwordPolicy,
      AuditService audit,
      Clock clock
  ) {
    this.users = users;
    this.memberships = memberships;
    this.companies = companies;
    this.refreshTokens = refreshTokens;
    this.passwordEncoder = passwordEncoder;
    this.passwordPolicy = passwordPolicy;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<UserResponse> list(UUID companyId) {
    assertCanManage(companyId);
    requireCompany(companyId);
    return memberships.findUsersInCompany(companyId);
  }

  @Transactional
  public UserResponse create(UUID companyId, CreateUserRequest request, RequestContext context) {
    NeiverPrincipal actor = assertCanManage(companyId);
    requireCompany(companyId);
    rejectPlatformRole(request.role());
    passwordPolicy.check(request.password(), request.email());
    if (users.existsByEmail(request.email())) {
      throw ApiException.conflict("Ya existe un usuario con ese correo.");
    }
    UserAccount user = new UserAccount(
        request.email(),
        passwordEncoder.encode(request.password()),
        request.firstName(),
        request.lastName());
    users.save(user);
    Membership membership = new Membership(user.getId(), companyId, request.role());
    memberships.save(membership);
    audit.record(AuditActions.USER_CREATED, "User", user.getId(), actor.userId(), companyId,
        Map.of("role", request.role().name()), context);
    return UserResponse.from(user, membership);
  }

  @Transactional
  public UserResponse update(UUID companyId, UUID userId, UpdateUserRequest request, RequestContext context) {
    NeiverPrincipal actor = assertCanManage(companyId);
    rejectPlatformRole(request.role());
    UserAccount user = requireUser(companyId, userId);
    Membership membership = memberships.findByUserId(userId)
        .orElseThrow(() -> ApiException.notFound("No encontramos ese usuario."));
    if (user.getId().equals(actor.userId()) && request.role() != membership.getRole()) {
      throw ApiException.badRequest("No puedes cambiar tu propio rol.");
    }
    if (wouldRemoveLastAdmin(companyId, user, membership, request.role(), user.getStatus())) {
      throw ApiException.lastAdmin();
    }
    if (users.existsByEmailAndIdNot(request.email(), userId)) {
      throw ApiException.conflict("Ya existe un usuario con ese correo.");
    }
    List<String> changed = new ArrayList<>();
    if (!user.getEmail().equals(request.email())) {
      changed.add("email");
    }
    if (!user.getFirstName().equals(request.firstName())) {
      changed.add("firstName");
    }
    if (!user.getLastName().equals(request.lastName())) {
      changed.add("lastName");
    }
    if (membership.getRole() != request.role()) {
      changed.add("role");
    }
    user.setEmail(request.email());
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    membership.setRole(request.role());
    audit.record(AuditActions.USER_UPDATED, "User", user.getId(), actor.userId(), companyId,
        Map.of("fields", changed), context);
    return UserResponse.from(user, membership);
  }

  @Transactional
  public UserResponse resetPassword(
      UUID companyId,
      UUID userId,
      ResetPasswordRequest request,
      RequestContext context
  ) {
    NeiverPrincipal actor = assertCanManage(companyId);
    UserAccount user = requireUser(companyId, userId);
    Membership membership = memberships.findByUserId(userId)
        .orElseThrow(() -> ApiException.notFound("No encontramos ese usuario."));
    passwordPolicy.check(request.password(), user.getEmail());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setPasswordChangeRequired(true);
    user.clearLock();
    user.bumpSession();
    users.saveAndFlush(user);
    refreshTokens.revokeActiveForUser(user.getId(), clock.instant());
    audit.record(AuditActions.USER_PASSWORD_RESET, "User", user.getId(), actor.userId(), companyId,
        Map.of(), context);
    return UserResponse.from(users.findById(user.getId()).orElseThrow(), membership);
  }

  @Transactional
  public UserResponse setEnabled(UUID companyId, UUID userId, boolean enabled, RequestContext context) {
    NeiverPrincipal actor = assertCanManage(companyId);
    if (userId.equals(actor.userId()) && !enabled) {
      throw ApiException.badRequest("No puedes desactivar tu propia cuenta.");
    }
    UserAccount user = requireUser(companyId, userId);
    Membership membership = memberships.findByUserId(userId)
        .orElseThrow(() -> ApiException.notFound("No encontramos ese usuario."));
    UserStatus next = enabled ? UserStatus.ACTIVE : UserStatus.DISABLED;
    if (!enabled && wouldRemoveLastAdmin(companyId, user, membership, membership.getRole(), next)) {
      throw ApiException.lastAdmin();
    }
    user.setStatus(next);
    if (!enabled) {
      user.bumpSession();
      users.saveAndFlush(user);
      refreshTokens.revokeActiveForUser(user.getId(), clock.instant());
    }
    audit.record(enabled ? AuditActions.USER_ENABLED : AuditActions.USER_DISABLED,
        "User", user.getId(), actor.userId(), companyId, Map.of(), context);
    UserAccount fresh = users.findById(user.getId()).orElseThrow();
    return UserResponse.from(fresh, membership);
  }

  private boolean wouldRemoveLastAdmin(
      UUID companyId,
      UserAccount user,
      Membership membership,
      Role nextRole,
      UserStatus nextStatus
  ) {
    boolean isActiveAdmin = membership.getRole() == Role.COMPANY_ADMIN && user.getStatus() == UserStatus.ACTIVE;
    boolean staysActiveAdmin = nextRole == Role.COMPANY_ADMIN && nextStatus == UserStatus.ACTIVE;
    if (!isActiveAdmin || staysActiveAdmin) {
      return false;
    }
    return users.countOtherActiveAdmins(companyId, user.getId()) == 0;
  }

  private NeiverPrincipal assertCanManage(UUID companyId) {
    NeiverPrincipal principal = AccessGuard.current();
    if (principal.role() == Role.SUPER_ADMIN) {
      return principal;
    }
    if (principal.role() == Role.COMPANY_ADMIN && companyId.equals(principal.companyId())) {
      return principal;
    }
    throw ApiException.permissionDenied();
  }

  private void requireCompany(UUID companyId) {
    if (!companies.existsById(companyId)) {
      throw ApiException.notFound("No encontramos esa empresa.");
    }
  }

  private UserAccount requireUser(UUID companyId, UUID userId) {
    UserAccount user = users.findById(userId)
        .orElseThrow(() -> ApiException.notFound("No encontramos ese usuario."));
    Membership membership = memberships.findByUserId(userId)
        .orElseThrow(() -> ApiException.notFound("No encontramos ese usuario."));
    if (!companyId.equals(membership.getCompanyId())) {
      throw ApiException.notFound("No encontramos ese usuario.");
    }
    return user;
  }

  private static void rejectPlatformRole(Role role) {
    if (role == Role.SUPER_ADMIN) {
      throw ApiException.validation("El rol de administrador general no se asigna desde una empresa.");
    }
  }
}
