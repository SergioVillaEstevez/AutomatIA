package com.neiverlabs.identity.web;

import com.neiverlabs.shared.web.Text;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    @Size(max = 320) String email,
    @NotBlank(message = "La clave es obligatoria")
    @Size(max = 200) String password
) {

  public LoginRequest {
    email = Text.email(email);
  }
}
