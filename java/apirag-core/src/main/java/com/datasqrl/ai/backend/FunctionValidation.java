package com.datasqrl.ai.backend;


import java.util.function.Function;

public record FunctionValidation<Message>(
    boolean isValid,
    boolean isPassthrough,
    ValidationError<Message> validationError) {

  public <OutputMessage> FunctionValidation<OutputMessage> translate(
      Function<Message, OutputMessage> errorHandler) {
    if (isValid) return new FunctionValidation<>(isValid, isPassthrough, null);
    return new FunctionValidation<>(isValid, isPassthrough,
        new ValidationError<>(errorHandler.apply(validationError.errorMessage), validationError.errorType));
  }

  public record ValidationError<Message>(
      Message errorMessage,
      ValidationErrorType errorType
  ) {
  }

  public enum ValidationErrorType {
    FUNCTION_NOT_FOUND,
    INVALID_JSON
  }
}
