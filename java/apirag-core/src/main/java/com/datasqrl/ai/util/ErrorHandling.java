package com.datasqrl.ai.util;

public class ErrorHandling {

  public static void checkArgument(boolean condition, String message, Object... args) {
    if (!condition) {
      throw new IllegalArgumentException(String.format(message, args));
    }
  }

  public static void checkArgument(boolean condition) {
    checkArgument(condition, "Unexpected arguments in method invocation");
  }


}
