package com.neiverlabs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neiverlabs.identity.security.JwtAuthenticationFilter;
import com.neiverlabs.shared.error.ErrorCode;
import com.neiverlabs.shared.error.HttpProblems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      ObjectMapper objectMapper,
      SecurityProperties securityProperties
  ) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(errors -> errors
            .authenticationEntryPoint((request, response, exception) -> HttpProblems.write(
                objectMapper,
                response,
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHENTICATED,
                "Autenticación necesaria",
                "Inicia sesión para continuar."))
            .accessDeniedHandler((request, response, exception) -> HttpProblems.write(
                objectMapper,
                response,
                HttpStatus.FORBIDDEN,
                ErrorCode.PERMISSION_DENIED,
                "Permiso insuficiente",
                "Tu rol no puede realizar esta operación.")))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
            .requestMatchers("/api/v1/public/**").permitAll()
            .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .requestMatchers("/api/v1/admin/**").hasRole("SUPER_ADMIN")
            .anyRequest().authenticated())
        .headers(headers -> {
          headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"));
          headers.frameOptions(frame -> frame.deny());
          if (securityProperties.isCookieSecure()) {
            headers.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000));
          }
        })
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  UserDetailsService userDetailsService() {
    return username -> {
      throw new UsernameNotFoundException("La plataforma no autentica con el usuario local de Spring Security.");
    };
  }

  @Bean
  PasswordEncoder passwordEncoder(SecurityProperties properties) {
    BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(properties.getBcryptStrength());
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put("bcrypt", bcrypt);
    DelegatingPasswordEncoder delegating = new DelegatingPasswordEncoder("bcrypt", encoders);
    delegating.setDefaultPasswordEncoderForMatches(bcrypt);
    return delegating;
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(properties.originList());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
    configuration.setExposedHeaders(List.of("X-Request-Id"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }

  @Bean
  FilterRegistrationBean<OncePerRequestFilter> requestIdFilter() {
    FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(new RequestIdFilter());
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }

  @Bean
  java.time.Clock clock() {
    return java.time.Clock.systemUTC();
  }
}
