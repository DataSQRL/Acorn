package com.datasqrl.ai.function.builtin;

import com.datasqrl.ai.function.UserDefinedFunction;
import java.time.Instant;

public class CurrentTime implements UserDefinedFunction {

  /**
   * @return the current time as an ISO 8601 formatted string.
   */
  @Override
  public String execute() {
    return Instant.now().toString();
  }
}
