package com.neiverlabs.ai.provider;

public record AiCompletionResult(
    String providerCode,
    String model,
    String content,
    int inputTokens,
    int outputTokens
) {
}
