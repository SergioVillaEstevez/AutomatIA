package com.neiverlabs.audit.service;

public final class AuditActions {

  public static final String LOGIN_SUCCESS = "AUTH_LOGIN_SUCCESS";
  public static final String LOGIN_FAILURE = "AUTH_LOGIN_FAILURE";
  public static final String LOGOUT = "AUTH_LOGOUT";
  public static final String REFRESH = "AUTH_REFRESH";
  public static final String REFRESH_REUSE = "AUTH_REFRESH_REUSE";
  public static final String PASSWORD_CHANGED = "AUTH_PASSWORD_CHANGED";
  public static final String COMPANY_CREATED = "COMPANY_CREATED";
  public static final String COMPANY_UPDATED = "COMPANY_UPDATED";
  public static final String COMPANY_SUSPENDED = "COMPANY_SUSPENDED";
  public static final String COMPANY_ACTIVATED = "COMPANY_ACTIVATED";
  public static final String USER_CREATED = "USER_CREATED";
  public static final String USER_UPDATED = "USER_UPDATED";
  public static final String USER_DISABLED = "USER_DISABLED";
  public static final String USER_ENABLED = "USER_ENABLED";
  public static final String USER_PASSWORD_RESET = "USER_PASSWORD_RESET";
  public static final String HOSP_RESOURCE_CREATED = "HOSP_RESOURCE_CREATED";
  public static final String HOSP_RESOURCE_UPDATED = "HOSP_RESOURCE_UPDATED";
  public static final String HOSP_RESERVATION_CREATED = "HOSP_RESERVATION_CREATED";
  public static final String HOSP_RESERVATION_UPDATED = "HOSP_RESERVATION_UPDATED";
  public static final String HOSP_WHATSAPP_SENT = "HOSP_WHATSAPP_SENT";

  private AuditActions() {
  }
}
