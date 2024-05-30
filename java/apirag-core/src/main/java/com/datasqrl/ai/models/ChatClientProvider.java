package com.datasqrl.ai.models;

import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionValidation;
import com.datasqrl.ai.backend.GenericChatMessage;
import com.datasqrl.ai.backend.ModelBindings;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
@AllArgsConstructor
public abstract class ChatClientProvider<Message, FunctionCall> {

  public static int DEFAULT_HISTORY_LIMIT = 50;

  protected final FunctionBackend backend;
  @Getter
  protected final ModelBindings<Message, FunctionCall> bindings;

  public abstract GenericChatMessage chat(String message, Map<String, Object> context);

  public List<GenericChatMessage> getHistory(Map<String, Object> sessionContext, boolean includeFunctionCalls) {
    return backend.getChatMessages(sessionContext, DEFAULT_HISTORY_LIMIT, GenericChatMessage.class).stream()
        .filter(message -> includeFunctionCalls || bindings.isUserOrAssistantMessage(message)).toList();
  }

  /**TODO: Move to Bindings **/

  protected abstract Message convertExceptionToMessage(String error);

  private Message convertExceptionToMessage(Exception exception) {
    String error = exception.getMessage() == null ? exception.toString() : exception.getMessage();
    return convertExceptionToMessage(error);
  }

  /**TODO: Move to chatsession as higher level method that validates, executes, and saves message**/

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

}
