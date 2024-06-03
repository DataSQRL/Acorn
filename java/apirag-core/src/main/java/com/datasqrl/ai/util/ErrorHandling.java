package com.datasqrl.ai.util;

public class ErrorHandling {

  public static void checkArgument(boolean condition, String message, Object... args) {
    if (!condition) {
      throw new IllegalArgumentException(String.format(message, args));
    }
  }


}
