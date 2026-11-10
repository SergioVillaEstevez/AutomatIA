package com.neiverlabs.identity.web;

import com.neiverlabs.identity.security.RequestContext;
import com.neiverlabs.identity.service.UserAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/companies/{companyId}/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {

  private final UserAdminService users;

  public AdminUserController(UserAdminService users) {
    this.users = users;
  }

  @GetMapping
  public List<UserResponse> list(@PathVariable UUID companyId) {
    return users.list(companyId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse create(
      @PathVariable UUID companyId,
      @Valid @RequestBody CreateUserRequest request,
      HttpServletRequest httpRequest
  ) {
    return users.create(companyId, request, RequestContext.from(httpRequest));
  }

  @PutMapping("/{userId}")
  public UserResponse update(
      @PathVariable UUID companyId,
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateUserRequest request,
      HttpServletRequest httpRequest
  ) {
    return users.update(companyId, userId, request, RequestContext.from(httpRequest));
  }

  @PostMapping("/{userId}/password")
  public UserResponse resetPassword(
      @PathVariable UUID companyId,
      @PathVariable UUID userId,
      @Valid @RequestBody ResetPasswordRequest request,
      HttpServletRequest httpRequest
  ) {
    return users.resetPassword(companyId, userId, request, RequestContext.from(httpRequest));
  }

  @PostMapping("/{userId}/deactivation")
  public UserResponse deactivate(
      @PathVariable UUID companyId,
      @PathVariable UUID userId,
      HttpServletRequest httpRequest
  ) {
    return users.setEnabled(companyId, userId, false, RequestContext.from(httpRequest));
  }

  @PostMapping("/{userId}/activation")
  public UserResponse activate(
      @PathVariable UUID companyId,
      @PathVariable UUID userId,
      HttpServletRequest httpRequest
  ) {
    return users.setEnabled(companyId, userId, true, RequestContext.from(httpRequest));
  }
}
