package com.neiverlabs.shared.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

public final class HttpProblems {

  private HttpProblems() {
  }

  public static ProblemDetail problem(HttpStatus status, ErrorCode code, String title, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setType(URI.create("https://neiverlabs.local/errors/" + code.name().toLowerCase().replace('_', '-')));
    problem.setProperty("code", code.name());
    return problem;
  }

  public static void write(
      ObjectMapper mapper,
      HttpServletResponse response,
      HttpStatus status,
      ErrorCode code,
      String title,
      String detail
  ) throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    mapper.writeValue(response.getOutputStream(), problem(status, code, title, detail));
  }
}
