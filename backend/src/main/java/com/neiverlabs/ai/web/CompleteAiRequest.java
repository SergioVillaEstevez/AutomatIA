package com.neiverlabs.ai.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteAiRequest(
    @NotBlank @Size(max = 8000) String prompt,
    @Size(max = 60) String taskType,
    @Min(16) @Max(4000) Integer maxOutputTokens
) {
}
