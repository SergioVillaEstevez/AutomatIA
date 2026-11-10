package com.neiverlabs.company.domain;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SpanishTaxIdValidator implements ConstraintValidator<ValidSpanishTaxId, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return value != null && SpanishTaxId.isValid(value);
  }
}
