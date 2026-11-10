package com.neiverlabs.ai.provider;

import java.util.List;

public record AiCompletionRequest(
    String model,
    List<Message> messages,
    Integer maxOutputTokens
) {
  public record Message(String role, String content) {
  }
}
