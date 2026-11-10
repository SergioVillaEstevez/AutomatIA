package com.neiverlabs.identity.web;

import com.neiverlabs.identity.domain.Role;
import com.neiverlabs.shared.web.Text;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    @Size(max = 320) String email,
    @NotBlank(message = "La clave temporal es obligatoria")
    @Size(max = 200) String password,
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80) String firstName,
    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 120) String lastName,
    @NotNull(message = "El rol es obligatorio") Role role
) {

  public CreateUserRequest {
    email = Text.email(email);
    firstName = Text.trim(firstName);
    lastName = Text.trim(lastName);
  }
}
