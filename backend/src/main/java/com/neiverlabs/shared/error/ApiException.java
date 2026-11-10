package com.neiverlabs.shared.error;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

  private final HttpStatus status;
  private final ErrorCode code;
  private final String title;

  public ApiException(HttpStatus status, ErrorCode code, String title, String detail) {
    super(detail);
    this.status = status;
    this.code = code;
    this.title = title;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public ErrorCode getCode() {
    return code;
  }

  public String getTitle() {
    return title;
  }

  public static ApiException invalidCredentials() {
    return new ApiException(
        HttpStatus.UNAUTHORIZED,
        ErrorCode.INVALID_CREDENTIALS,
        "Acceso denegado",
        "El correo o la contraseña no son correctos.");
  }

  public static ApiException accountLocked() {
    return new ApiException(
        HttpStatus.LOCKED,
        ErrorCode.ACCOUNT_LOCKED,
        "Cuenta bloqueada",
        "Demasiados intentos fallidos. Espera unos minutos o pide a un administrador que restablezca la clave.");
  }

  public static ApiException accountDisabled() {
    return new ApiException(
        HttpStatus.FORBIDDEN,
        ErrorCode.ACCOUNT_DISABLED,
        "Cuenta desactivada",
        "Esta cuenta está desactivada. Un administrador de la empresa puede volver a activarla.");
  }

  public static ApiException companySuspended() {
    return new ApiException(
        HttpStatus.FORBIDDEN,
        ErrorCode.COMPANY_SUSPENDED,
        "Empresa suspendida",
        "La empresa está suspendida y no puede usar la plataforma.");
  }

  public static ApiException passwordChangeRequired() {
    return new ApiException(
        HttpStatus.FORBIDDEN,
        ErrorCode.PASSWORD_CHANGE_REQUIRED,
        "Cambio de clave pendiente",
        "Hay que cambiar la clave antes de continuar.");
  }

  public static ApiException permissionDenied() {
    return new ApiException(
        HttpStatus.FORBIDDEN,
        ErrorCode.PERMISSION_DENIED,
        "Permiso insuficiente",
        "Tu rol no puede realizar esta operación.");
  }

  public static ApiException tenantRequired() {
    return new ApiException(
        HttpStatus.FORBIDDEN,
        ErrorCode.TENANT_REQUIRED,
        "Operación de empresa",
        "El administrador general usa el panel de administración, no el espacio de una empresa.");
  }

  public static ApiException notFound(String detail) {
    return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "No encontrado", detail);
  }

  public static ApiException conflict(String detail) {
    return new ApiException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "Dato duplicado", detail);
  }

  public static ApiException validation(String detail) {
    return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Datos no válidos", detail);
  }

  public static ApiException rateLimited() {
    return new ApiException(
        HttpStatus.TOO_MANY_REQUESTS,
        ErrorCode.RATE_LIMITED,
        "Demasiados intentos",
        "Demasiados intentos de acceso desde esta dirección. Espera un minuto.");
  }

  public static ApiException lastAdmin() {
    return new ApiException(
        HttpStatus.CONFLICT,
        ErrorCode.LAST_ADMIN,
        "Último administrador",
        "La empresa debe conservar al menos un administrador activo.");
  }

  public static ApiException badRequest(String detail) {
    return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Solicitud no válida", detail);
  }
}
