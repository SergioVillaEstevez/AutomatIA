package com.neiverlabs.company.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Comprueba NIF, NIE y CIF españoles. No consulta el censo de Hacienda:
 * solo rechaza identificadores con formato o dígito de control incorrectos.
 */
public final class SpanishTaxId {

  private static final String NIF_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";
  private static final String CIF_CONTROL_LETTERS = "JABCDEFGHI";
  private static final String CIF_LETTER_ONLY = "PQSW";
  private static final String CIF_NUMBER_ONLY = "ABEH";
  private static final Pattern NIF = Pattern.compile("\\d{8}[A-Z]");
  private static final Pattern NIE = Pattern.compile("[XYZ]\\d{7}[A-Z]");
  private static final Pattern CIF = Pattern.compile("[ABCDEFGHJNPQRSUVW]\\d{7}[0-9A-J]");

  private SpanishTaxId() {
  }

  public static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String compact = value.replace(" ", "").replace("-", "").trim().toUpperCase(Locale.ROOT);
    return compact.isEmpty() ? null : compact;
  }

  public static boolean isValid(String value) {
    String taxId = normalize(value);
    if (taxId == null) {
      return false;
    }
    if (NIF.matcher(taxId).matches()) {
      return nifLetter(taxId.substring(0, 8)) == taxId.charAt(8);
    }
    if (NIE.matcher(taxId).matches()) {
      String digits = switch (taxId.charAt(0)) {
        case 'X' -> "0";
        case 'Y' -> "1";
        case 'Z' -> "2";
        default -> "";
      } + taxId.substring(1, 8);
      return nifLetter(digits) == taxId.charAt(8);
    }
    if (CIF.matcher(taxId).matches()) {
      return cifMatches(taxId);
    }
    return false;
  }

  public static String require(String value) {
    String taxId = normalize(value);
    if (!isValid(taxId)) {
      throw new IllegalArgumentException("tax-id");
    }
    return taxId;
  }

  private static char nifLetter(String eightDigits) {
    int number = Integer.parseInt(eightDigits);
    return NIF_LETTERS.charAt(number % 23);
  }

  private static boolean cifMatches(String taxId) {
    int even = 0;
    int odd = 0;
    for (int index = 0; index < 7; index++) {
      int digit = taxId.charAt(index + 1) - '0';
      if (index % 2 == 0) {
        int doubled = digit * 2;
        odd += doubled / 10 + doubled % 10;
      } else {
        even += digit;
      }
    }
    int control = (10 - ((even + odd) % 10)) % 10;
    char expectedDigit = (char) ('0' + control);
    char expectedLetter = CIF_CONTROL_LETTERS.charAt(control);
    char actual = taxId.charAt(8);
    char organization = taxId.charAt(0);
    if (CIF_LETTER_ONLY.indexOf(organization) >= 0) {
      return actual == expectedLetter;
    }
    if (CIF_NUMBER_ONLY.indexOf(organization) >= 0) {
      return actual == expectedDigit;
    }
    return actual == expectedDigit || actual == expectedLetter;
  }
}
