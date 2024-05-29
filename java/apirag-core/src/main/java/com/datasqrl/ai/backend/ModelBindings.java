package com.datasqrl.ai.backend;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public interface ModelBindings<Message, FunctionCall> {

  Message convertMessage(GenericChatMessage message);

  GenericChatMessage convertMessage(Message message, Map<String, Object> sessionContext);

  boolean isUserOrAssistantMessage(Message message);

  ModelAnalyzer<Message> getTokenCounter();

  int getMaxInputTokens();

  GenericChatMessage createSystemMessage(String systemMessage, Map<String, Object> sessionContext);

  String getFunctionName(FunctionCall functionCall);

  JsonNode getFunctionArguments(FunctionCall functionCall);

  Message newFunctionResultMessage(String functionName, String functionResult);
}
