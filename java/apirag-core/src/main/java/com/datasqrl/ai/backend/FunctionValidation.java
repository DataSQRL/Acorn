package com.datasqrl.ai.backend;


import java.util.function.Function;
import lombok.Value;

@Value
public class FunctionValidation<Message> {

  boolean isValid;
  boolean isPassthrough;
  Message errorMessage;

  public <OutputMessage> FunctionValidation<OutputMessage> translate(
      Function<Message,OutputMessage> errorHandler) {
    if (isValid) return new FunctionValidation<>(isValid, isPassthrough, null);
    return new FunctionValidation<>(isValid, isPassthrough, errorHandler.apply(errorMessage));
  }

}
