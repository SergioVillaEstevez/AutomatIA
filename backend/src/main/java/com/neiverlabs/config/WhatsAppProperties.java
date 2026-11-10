package com.neiverlabs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "neiver.whatsapp")
public class WhatsAppProperties {

  /** Token permanente o temporal de Meta. Vacío = WhatsApp no disponible. */
  private String accessToken = "";

  /** Identificador del número de WhatsApp Business. */
  private String phoneNumberId = "";

  private String apiBaseUrl = "https://graph.facebook.com/v21.0";

  private int timeoutSeconds = 30;

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getPhoneNumberId() {
    return phoneNumberId;
  }

  public void setPhoneNumberId(String phoneNumberId) {
    this.phoneNumberId = phoneNumberId;
  }

  public String getApiBaseUrl() {
    return apiBaseUrl;
  }

  public void setApiBaseUrl(String apiBaseUrl) {
    this.apiBaseUrl = apiBaseUrl;
  }

  public int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  public void setTimeoutSeconds(int timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
  }
}
