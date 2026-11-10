package com.neiverlabs.identity.security;

import com.neiverlabs.identity.domain.Role;
import com.neiverlabs.shared.error.ApiException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AccessGuard {

  private AccessGuard() {
  }

  public static NeiverPrincipal current() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof NeiverPrincipal principal)) {
      throw new ApiException(
          org.springframework.http.HttpStatus.UNAUTHORIZED,
          com.neiverlabs.shared.error.ErrorCode.UNAUTHENTICATED,
          "Autenticación necesaria",
          "Inicia sesión para continuar.");
    }
    return principal;
  }

  public static NeiverPrincipal requireSuperAdmin() {
    NeiverPrincipal principal = current();
    if (principal.role() != Role.SUPER_ADMIN) {
      throw ApiException.permissionDenied();
    }
    return principal;
  }

  public static NeiverPrincipal requireCompanyAdmin() {
    NeiverPrincipal principal = current();
    if (principal.role() != Role.COMPANY_ADMIN || principal.companyId() == null) {
      throw ApiException.permissionDenied();
    }
    return principal;
  }

  public static UUID requireTenant() {
    NeiverPrincipal principal = current();
    if (principal.companyId() == null) {
      throw ApiException.tenantRequired();
    }
    return principal.companyId();
  }
}
