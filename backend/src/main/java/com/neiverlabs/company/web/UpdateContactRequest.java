package com.neiverlabs.company.web;

import com.neiverlabs.shared.web.Text;
import jakarta.validation.constraints.Size;

public record UpdateContactRequest(
    @Size(max = 30) String phone,
    @Size(max = 200) String addressLine,
    @Size(max = 120) String city,
    @Size(max = 120) String province,
    @Size(max = 12) String postalCode
) {

  public UpdateContactRequest {
    phone = Text.trim(phone);
    addressLine = Text.trim(addressLine);
    city = Text.trim(city);
    province = Text.trim(province);
    postalCode = Text.trim(postalCode);
  }
}
