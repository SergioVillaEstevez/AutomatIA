package com.neiverlabs.hospitality.web;

import com.neiverlabs.shared.web.Text;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WhatsAppTemplateRequest(
    @NotBlank
    @Size(max = 60)
    @Pattern(regexp = "^[a-z0-9_]+$", message = "El código solo admite minúsculas, números y guion bajo")
    String code,
    @NotBlank @Size(max = 120) String title,
    @NotBlank @Size(max = 1000) String body,
    @Size(max = 120) String metaTemplateName,
    Boolean active
) {
  public WhatsAppTemplateRequest {
    code = Text.trim(code);
    title = Text.trim(title);
    body = Text.trim(body);
    metaTemplateName = Text.trim(metaTemplateName);
  }
}
