package com.neiverlabs.hospitality.web;

import com.neiverlabs.shared.web.Text;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResourceRequest(
    @NotBlank @Size(max = 80) String name,
    @NotNull @Min(1) @Max(100) Integer seats,
    @Min(0) @Max(10_000) Integer sortOrder,
    Boolean active
) {
  public ResourceRequest {
    name = Text.trim(name);
  }
}
