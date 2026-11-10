package com.neiverlabs.hospitality.web;

import com.neiverlabs.hospitality.domain.PaymentMethod;
import com.neiverlabs.shared.web.Text;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public final class TicketRequests {

  private TicketRequests() {
  }

  public record CreateTicketRequest(
      UUID reservationId,
      UUID resourceId,
      @NotBlank @Size(max = 120) String label,
      @Size(max = 500) String notes
  ) {
    public CreateTicketRequest {
      label = Text.trim(label);
      notes = Text.trim(notes);
    }
  }

  public record AddLineRequest(
      @NotBlank @Size(max = 200) String description,
      @NotNull @DecimalMin("0.01") BigDecimal quantity,
      @NotNull @DecimalMin("0.00") BigDecimal unitPriceEur
  ) {
    public AddLineRequest {
      description = Text.trim(description);
    }
  }

  public record PayTicketRequest(
      @NotNull PaymentMethod paymentMethod,
      @Size(max = 500) String notes
  ) {
    public PayTicketRequest {
      notes = Text.trim(notes);
    }
  }
}
