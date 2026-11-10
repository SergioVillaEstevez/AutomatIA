package com.neiverlabs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "neiver.security")
public class SecurityProperties {

  /**
   * Secreto conocido del perfil dev. El perfil prod rechaza este valor.
   */
  public static final String DEV_JWT_SECRET = "dev-only-neiver-labs-jwt-secret-32";
  public static final String DEV_SUPER_ADMIN_PASSWORD = "Neiver-Dev-Admin-12";

  private String jwtSecret;
  private int accessTokenMinutes = 15;
  private int refreshTokenDays = 14;
  private boolean cookieSecure;
  private int bcryptStrength = 12;
  private String superAdminEmail = "";
  private String superAdminPassword = "";
  private String superAdminName = "Administrador Neiver";
  private int loginAttemptsPerMinute = 10;
  private int maxFailedLogins = 5;
  private int lockMinutes = 15;

  public String getJwtSecret() {
    return jwtSecret;
  }

  public void setJwtSecret(String jwtSecret) {
    this.jwtSecret = jwtSecret;
  }

  public int getAccessTokenMinutes() {
    return accessTokenMinutes;
  }

  public void setAccessTokenMinutes(int accessTokenMinutes) {
    this.accessTokenMinutes = accessTokenMinutes;
  }

  public int getRefreshTokenDays() {
    return refreshTokenDays;
  }

  public void setRefreshTokenDays(int refreshTokenDays) {
    this.refreshTokenDays = refreshTokenDays;
  }

  public boolean isCookieSecure() {
    return cookieSecure;
  }

  public void setCookieSecure(boolean cookieSecure) {
    this.cookieSecure = cookieSecure;
  }

  public int getBcryptStrength() {
    return bcryptStrength;
  }

  public void setBcryptStrength(int bcryptStrength) {
    this.bcryptStrength = bcryptStrength;
  }

  public String getSuperAdminEmail() {
    return superAdminEmail;
  }

  public void setSuperAdminEmail(String superAdminEmail) {
    this.superAdminEmail = superAdminEmail;
  }

  public String getSuperAdminPassword() {
    return superAdminPassword;
  }

  public void setSuperAdminPassword(String superAdminPassword) {
    this.superAdminPassword = superAdminPassword;
  }

  public String getSuperAdminName() {
    return superAdminName;
  }

  public void setSuperAdminName(String superAdminName) {
    this.superAdminName = superAdminName;
  }

  public int getLoginAttemptsPerMinute() {
    return loginAttemptsPerMinute;
  }

  public void setLoginAttemptsPerMinute(int loginAttemptsPerMinute) {
    this.loginAttemptsPerMinute = loginAttemptsPerMinute;
  }

  public int getMaxFailedLogins() {
    return maxFailedLogins;
  }

  public void setMaxFailedLogins(int maxFailedLogins) {
    this.maxFailedLogins = maxFailedLogins;
  }

  public int getLockMinutes() {
    return lockMinutes;
  }

  public void setLockMinutes(int lockMinutes) {
    this.lockMinutes = lockMinutes;
  }
}
