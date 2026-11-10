package com.neiverlabs.identity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank(message = "La clave temporal es obligatoria")
    @Size(max = 200) String password
) {
}
