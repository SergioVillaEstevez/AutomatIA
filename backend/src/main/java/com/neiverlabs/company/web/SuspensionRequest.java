package com.neiverlabs.company.web;

import com.neiverlabs.shared.web.Text;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuspensionRequest(
    @NotBlank(message = "Indica el motivo de la suspensión")
    @Size(max = 500) String reason
) {

  public SuspensionRequest {
    reason = Text.trim(reason);
  }
}
