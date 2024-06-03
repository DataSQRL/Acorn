package com.datasqrl.ai.backend;


import java.util.function.Function;
import lombok.Value;

@Value
public class FunctionValidation<Message> {

  boolean isValid;
  boolean isClientExecuted;
  Message errorMessage;

  public <OutputMessage> FunctionValidation<OutputMessage> translate(
      Function<Message,OutputMessage> errorHandler) {
    if (isValid) return new FunctionValidation<>(isValid, isClientExecuted, null);
    return new FunctionValidation<>(isValid, isClientExecuted, errorHandler.apply(errorMessage));
  }

}
