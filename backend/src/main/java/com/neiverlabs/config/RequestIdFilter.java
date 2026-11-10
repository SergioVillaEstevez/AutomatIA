package com.neiverlabs.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestIdFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String requestId = UUID.randomUUID().toString();
    MDC.put("requestId", requestId);
    response.setHeader("X-Request-Id", requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }
}
