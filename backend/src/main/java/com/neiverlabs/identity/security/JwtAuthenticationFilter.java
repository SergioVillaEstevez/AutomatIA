package com.neiverlabs.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neiverlabs.identity.security.JwtService.AccessToken;
import com.neiverlabs.shared.error.ApiException;
import com.neiverlabs.shared.error.ErrorCode;
import com.neiverlabs.shared.error.HttpProblems;
import com.neiverlabs.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final SessionLoader sessionLoader;
  private final ObjectMapper objectMapper;

  public JwtAuthenticationFilter(JwtService jwtService, SessionLoader sessionLoader, ObjectMapper objectMapper) {
    this.jwtService = jwtService;
    this.sessionLoader = sessionLoader;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    TenantContext.clear();
    try {
      String header = request.getHeader(HttpHeaders.AUTHORIZATION);
      if (header != null && header.startsWith("Bearer ")) {
        String raw = header.substring("Bearer ".length()).trim();
        if (!authenticate(raw, request, response)) {
          return;
        }
      }
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove("userId");
      TenantContext.clear();
      SecurityContextHolder.clearContext();
    }
  }

  private boolean authenticate(String rawToken, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    try {
      AccessToken token = jwtService.parse(rawToken);
      TenantContext.set(com.neiverlabs.tenant.TenantSnapshot.userOnly(token.userId()));
      SessionLoader.AuthenticatedSession session = sessionLoader.load(token.userId(), token.sessionVersion());
      TenantContext.set(session.snapshot());
      MDC.put("userId", session.principal().userId().toString());
      UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
          session.principal(), null, session.principal().getAuthorities());
      SecurityContextHolder.getContext().setAuthentication(authentication);
      if (session.principal().passwordChangeRequired() && !allowedWhilePasswordChange(request)) {
        HttpProblems.write(
            objectMapper,
            response,
            HttpStatus.FORBIDDEN,
            ErrorCode.PASSWORD_CHANGE_REQUIRED,
            "Cambio de clave pendiente",
            "Hay que cambiar la clave antes de continuar.");
        return false;
      }
      return true;
    } catch (InvalidAccessTokenException exception) {
      HttpProblems.write(
          objectMapper,
          response,
          HttpStatus.UNAUTHORIZED,
          ErrorCode.INVALID_TOKEN,
          "Sesión no válida",
          "La sesión ha caducado o ya no es válida.");
      return false;
    } catch (ApiException exception) {
      HttpProblems.write(
          objectMapper,
          response,
          exception.getStatus(),
          exception.getCode(),
          exception.getTitle(),
          exception.getMessage());
      return false;
    }
  }

  private static boolean allowedWhilePasswordChange(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    return path.equals("/api/v1/auth/change-password")
        || path.equals("/api/v1/auth/logout")
        || path.equals("/api/v1/auth/me")
        || path.equals("/api/v1/auth/refresh");
  }
}
