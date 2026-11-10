package com.neiverlabs.hospitality.whatsapp;

public class WhatsAppClientException extends RuntimeException {

  private final String errorCode;

  public WhatsAppClientException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
