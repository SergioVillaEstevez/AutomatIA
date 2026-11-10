package com.neiverlabs.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ProblemDetail> handleApi(ApiException exception) {
    ProblemDetail problem = HttpProblems.problem(
        exception.getStatus(), exception.getCode(), exception.getTitle(), exception.getMessage());
    return ResponseEntity.status(exception.getStatus()).body(problem);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
    List<FieldErrorBody> errors = exception.getBindingResult().getFieldErrors().stream()
        .map(error -> new FieldErrorBody(error.getField(), error.getDefaultMessage()))
        .toList();
    ProblemDetail problem = HttpProblems.problem(
        HttpStatus.BAD_REQUEST,
        ErrorCode.VALIDATION_ERROR,
        "Datos no válidos",
        "Revisa los campos indicados.");
    problem.setProperty("errors", errors);
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException exception) {
    return ResponseEntity.badRequest().body(HttpProblems.problem(
        HttpStatus.BAD_REQUEST,
        ErrorCode.BAD_REQUEST,
        "Solicitud no válida",
        "El cuerpo de la petición no tiene un formato válido."));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemDetail> handleType(MethodArgumentTypeMismatchException exception) {
    return ResponseEntity.badRequest().body(HttpProblems.problem(
        HttpStatus.BAD_REQUEST,
        ErrorCode.BAD_REQUEST,
        "Solicitud no válida",
        "Uno de los parámetros no tiene el formato esperado."));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleDenied(AccessDeniedException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(HttpProblems.problem(
        HttpStatus.FORBIDDEN,
        ErrorCode.PERMISSION_DENIED,
        "Permiso insuficiente",
        "Tu rol no puede realizar esta operación."));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> handleConflict(DataIntegrityViolationException exception) {
    String message = "Ya existe un registro con ese correo o ese CIF.";
    Throwable cause = exception.getMostSpecificCause();
    String text = cause.getMessage() == null ? "" : cause.getMessage();
    if (text.contains("uq_companies_tax_id")) {
      message = "Ya existe una empresa con ese CIF o NIF.";
    } else if (text.contains("uq_users_email")) {
      message = "Ya existe un usuario con ese correo.";
    } else if (text.contains("user_without_membership")) {
      message = "El usuario no quedó asociado a ninguna empresa.";
    }
    return ResponseEntity.status(HttpStatus.CONFLICT).body(HttpProblems.problem(
        HttpStatus.CONFLICT, ErrorCode.CONFLICT, "No se puede guardar", message));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
    log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpProblems.problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.INTERNAL_ERROR,
        "Error interno",
        "No se ha podido completar la operación."));
  }

  public record FieldErrorBody(String field, String message) {
  }
}
