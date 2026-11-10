package com.neiverlabs.identity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "La clave actual es obligatoria")
    @Size(max = 200) String currentPassword,
    @NotBlank(message = "La clave nueva es obligatoria")
    @Size(max = 200) String newPassword
) {
}
