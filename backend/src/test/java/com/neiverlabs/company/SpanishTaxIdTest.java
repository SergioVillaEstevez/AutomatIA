package com.neiverlabs.company;

import static org.assertj.core.api.Assertions.assertThat;

import com.neiverlabs.company.domain.SpanishTaxId;
import org.junit.jupiter.api.Test;

class SpanishTaxIdTest {

  @Test
  void acceptsDniNieAndCifWithValidControl() {
    assertThat(SpanishTaxId.isValid("12345678Z")).isTrue();
    assertThat(SpanishTaxId.isValid("X0000000T")).isTrue();
    assertThat(SpanishTaxId.isValid("B12345674")).isTrue();
    assertThat(SpanishTaxId.isValid("B00000000")).isTrue();
    assertThat(SpanishTaxId.isValid("P0000000J")).isTrue();
    assertThat(SpanishTaxId.isValid("b-12345674")).isTrue();
  }

  @Test
  void rejectsWrongControlDigit() {
    assertThat(SpanishTaxId.isValid("12345678A")).isFalse();
    assertThat(SpanishTaxId.isValid("B1234567J")).isFalse();
    assertThat(SpanishTaxId.isValid("B0000000J")).isFalse();
    assertThat(SpanishTaxId.isValid("1234")).isFalse();
    assertThat(SpanishTaxId.isValid(null)).isFalse();
  }
}
