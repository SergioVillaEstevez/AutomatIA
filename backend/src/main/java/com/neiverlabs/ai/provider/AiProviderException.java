package com.neiverlabs.ai.provider;

public class AiProviderException extends RuntimeException {

  private final String errorCode;

  public AiProviderException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
