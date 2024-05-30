package com.datasqrl.ai.models;

import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionValidation;
import com.datasqrl.ai.backend.ModelBindings;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
@AllArgsConstructor
public abstract class ChatClientProvider<Message, FunctionCall> {

  protected final FunctionBackend backend;
  @Getter
  protected final ModelBindings<Message, FunctionCall> bindings;


  protected FunctionValidation<Message> validateFunctionCall(FunctionCall chatFunctionCall) {
    return backend.validateFunctionCall(bindings.getFunctionName(chatFunctionCall),
        bindings.getFunctionArguments(chatFunctionCall)).translate(this::convertExceptionToMessage);
  }

  protected Message executeFunctionCall(FunctionCall chatFunctionCall, Map<String, Object> context) {
      String functionName = bindings.getFunctionName(chatFunctionCall);
      JsonNode functionArguments = bindings.getFunctionArguments(chatFunctionCall);
    try {
      String functionResult = backend.executeFunctionCall(functionName, functionArguments, context);
      return bindings.newFunctionResultMessage(functionName, functionResult);
    } catch (Exception e) {
      return convertExceptionToMessage(e);
    }
  }

  private Message convertExceptionToMessage(Exception exception) {
    String error = exception.getMessage() == null ? exception.toString() : exception.getMessage();
    return convertExceptionToMessage(error);
  }

  public abstract Message chat(String message, Map<String, Object> context);

  protected abstract Message convertExceptionToMessage(String error);
}
