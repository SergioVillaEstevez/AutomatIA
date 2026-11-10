package com.neiverlabs.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.neiverlabs.config.AiProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OpenAIProvider implements AiProvider {

  public static final String CODE = "openai";

  private final AiProperties properties;
  private final RestClient client;

  @Autowired
  public OpenAIProvider(AiProperties properties) {
    this.properties = properties;
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
    factory.setReadTimeout(Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds())));
    this.client = RestClient.builder()
        .baseUrl(properties.getOpenaiBaseUrl())
        .requestFactory(factory)
        .build();
  }

  OpenAIProvider(AiProperties properties, RestClient client) {
    this.properties = properties;
    this.client = client;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public boolean available() {
    return StringUtils.hasText(properties.getOpenaiApiKey());
  }

  @Override
  public AiCompletionResult complete(AiCompletionRequest request) {
    if (!available()) {
      throw new AiProviderException("AI_NOT_CONFIGURED", "Falta OPENAI_API_KEY en el servidor.");
    }
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", request.model());
    List<Map<String, String>> messages = new ArrayList<>();
    for (AiCompletionRequest.Message message : request.messages()) {
      messages.add(Map.of("role", message.role(), "content", message.content()));
    }
    body.put("messages", messages);
    if (request.maxOutputTokens() != null) {
      body.put("max_tokens", request.maxOutputTokens());
    }

    try {
      JsonNode response = client.post()
          .uri("/v1/chat/completions")
          .contentType(MediaType.APPLICATION_JSON)
          .header("Authorization", "Bearer " + properties.getOpenaiApiKey())
          .body(body)
          .retrieve()
          .body(JsonNode.class);
      if (response == null) {
        throw new AiProviderException("AI_EMPTY_RESPONSE", "OpenAI no devolvió cuerpo.");
      }
      String content = response.path("choices").path(0).path("message").path("content").asText("");
      int inputTokens = response.path("usage").path("prompt_tokens").asInt(0);
      int outputTokens = response.path("usage").path("completion_tokens").asInt(0);
      String model = response.path("model").asText(request.model());
      return new AiCompletionResult(CODE, model, content, inputTokens, outputTokens);
    } catch (RestClientResponseException ex) {
      throw new AiProviderException(
          "AI_PROVIDER_HTTP_" + ex.getStatusCode().value(),
          "OpenAI respondió " + ex.getStatusCode().value() + ": " + truncate(ex.getResponseBodyAsString()));
    } catch (AiProviderException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new AiProviderException("AI_PROVIDER_ERROR", "Error al llamar a OpenAI: " + ex.getMessage());
    }
  }

  private static String truncate(String value) {
    if (value == null) {
      return "";
    }
    return value.length() <= 300 ? value : value.substring(0, 300);
  }
}
