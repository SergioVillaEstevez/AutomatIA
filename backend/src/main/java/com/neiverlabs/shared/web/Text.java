package com.neiverlabs.shared.web;

import java.util.Locale;

public final class Text {

  private Text() {
  }

  public static String trim(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public static String email(String value) {
    String trimmed = trim(value);
    return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
  }

  public static String likePattern(String value) {
    String normalized = value.toLowerCase(Locale.ROOT)
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_");
    return "%" + normalized + "%";
  }
}
