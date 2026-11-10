package com.neiverlabs.identity.security;

public class InvalidAccessTokenException extends RuntimeException {

  public InvalidAccessTokenException() {
    super("invalid-access-token");
  }
}
