package com.datasqrl.ai.backend;


import java.util.function.Function;

public record FunctionValidation<Message>(
    boolean isValid,
    boolean isClientExecuted,
    ValidationError<Message> validationError) {

  public <OutputMessage> FunctionValidation<OutputMessage> translate(
      Function<Message, OutputMessage> errorHandler) {
    if (isValid) return new FunctionValidation<>(isValid, isClientExecuted, null);
    return new FunctionValidation<>(isValid, isClientExecuted,
        new ValidationError<>(errorHandler.apply(validationError.errorMessage), validationError.errorType));
  }

  public record ValidationError<Message>(
      Message errorMessage,
      Type errorType
  ) {
    public enum Type {
      FUNCTION_NOT_FOUND,
      INVALID_JSON
    }
  }

}
