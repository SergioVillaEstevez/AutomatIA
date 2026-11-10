package com.neiverlabs.hospitality.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.neiverlabs.config.WhatsAppProperties;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class WhatsAppCloudClient {

  private final WhatsAppProperties properties;
  private final RestClient client;

  @Autowired
  public WhatsAppCloudClient(WhatsAppProperties properties) {
    this.properties = properties;
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
    factory.setReadTimeout(Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds())));
    this.client = RestClient.builder()
        .baseUrl(properties.getApiBaseUrl())
        .requestFactory(factory)
        .build();
  }

  WhatsAppCloudClient(WhatsAppProperties properties, RestClient client) {
    this.properties = properties;
    this.client = client;
  }

  public boolean available() {
    return StringUtils.hasText(properties.getAccessToken())
        && StringUtils.hasText(properties.getPhoneNumberId());
  }

  public String sendText(String e164Phone, String body) {
    if (!available()) {
      throw new WhatsAppClientException(
          "WHATSAPP_NOT_CONFIGURED",
          "Faltan WHATSAPP_ACCESS_TOKEN o WHATSAPP_PHONE_NUMBER_ID en el servidor.");
    }
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("messaging_product", "whatsapp");
    payload.put("to", e164Phone);
    payload.put("type", "text");
    payload.put("text", Map.of("preview_url", false, "body", body));

    try {
      JsonNode response = client.post()
          .uri("/{phoneNumberId}/messages", properties.getPhoneNumberId())
          .contentType(MediaType.APPLICATION_JSON)
          .header("Authorization", "Bearer " + properties.getAccessToken())
          .body(payload)
          .retrieve()
          .body(JsonNode.class);
      if (response == null) {
        throw new WhatsAppClientException("WHATSAPP_EMPTY_RESPONSE", "WhatsApp no devolvió cuerpo.");
      }
      String messageId = response.path("messages").path(0).path("id").asText("");
      if (!StringUtils.hasText(messageId)) {
        throw new WhatsAppClientException("WHATSAPP_EMPTY_ID", "WhatsApp no devolvió id de mensaje.");
      }
      return messageId;
    } catch (RestClientResponseException ex) {
      throw new WhatsAppClientException(
          "WHATSAPP_HTTP_" + ex.getStatusCode().value(),
          "WhatsApp rechazó el envío: " + ex.getResponseBodyAsString());
    }
  }
}
