package com.neiverlabs.identity.security;

import com.neiverlabs.identity.domain.Role;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class NeiverPrincipal implements UserDetails {

  private final UUID userId;
  private final String email;
  private final String fullName;
  private final Role role;
  private final UUID companyId;
  private final String companyName;
  private final boolean passwordChangeRequired;

  public NeiverPrincipal(
      UUID userId,
      String email,
      String fullName,
      Role role,
      UUID companyId,
      String companyName,
      boolean passwordChangeRequired
  ) {
    this.userId = userId;
    this.email = email;
    this.fullName = fullName;
    this.role = role;
    this.companyId = companyId;
    this.companyName = companyName;
    this.passwordChangeRequired = passwordChangeRequired;
  }

  public UUID userId() {
    return userId;
  }

  public String email() {
    return email;
  }

  public String fullName() {
    return fullName;
  }

  public Role role() {
    return role;
  }

  public UUID companyId() {
    return companyId;
  }

  public String companyName() {
    return companyName;
  }

  public boolean passwordChangeRequired() {
    return passwordChangeRequired;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public String getPassword() {
    return "";
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
