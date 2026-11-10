package com.neiverlabs.ai.provider;

public interface AiProvider {

  String code();

  boolean available();

  AiCompletionResult complete(AiCompletionRequest request);
}
