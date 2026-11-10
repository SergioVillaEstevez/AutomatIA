package com.neiverlabs.identity.security;

import jakarta.servlet.http.HttpServletRequest;

public record RequestContext(String ip, String userAgent) {

  public static RequestContext from(HttpServletRequest request) {
    String agent = request.getHeader("User-Agent");
    if (agent != null && agent.length() > 300) {
      agent = agent.substring(0, 300);
    }
    return new RequestContext(request.getRemoteAddr(), agent);
  }

  public static RequestContext none() {
    return new RequestContext(null, null);
  }
}
