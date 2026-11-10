package com.neiverlabs.company.web;

import com.neiverlabs.company.domain.SpanishTaxId;
import com.neiverlabs.company.domain.ValidSpanishTaxId;
import com.neiverlabs.shared.web.Text;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateCompanyRequest(
    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 200) String legalName,
    @NotBlank(message = "El nombre comercial es obligatorio")
    @Size(max = 200) String tradeName,
    @NotBlank(message = "El CIF o NIF es obligatorio")
    @ValidSpanishTaxId String taxId,
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    @Size(max = 320) String email,
    @Size(max = 30) String phone,
    @Size(max = 200) String addressLine,
    @Size(max = 120) String city,
    @Size(max = 120) String province,
    @Size(max = 12) String postalCode,
    @NotBlank(message = "El plan es obligatorio")
    @Size(max = 40)
    @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9 ._-]{0,39}", message = "El plan solo puede contener letras, números, espacios, punto, guion o guion bajo")
    String planLabel,
    @DecimalMin(value = "0.00", message = "El límite mensual no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El límite mensual admite como máximo dos decimales")
    BigDecimal monthlyAiBudgetEur
) {

  public CreateCompanyRequest {
    legalName = Text.trim(legalName);
    tradeName = Text.trim(tradeName);
    taxId = SpanishTaxId.normalize(taxId);
    email = Text.email(email);
    phone = Text.trim(phone);
    addressLine = Text.trim(addressLine);
    city = Text.trim(city);
    province = Text.trim(province);
    postalCode = Text.trim(postalCode);
    planLabel = Text.trim(planLabel);
  }
}
