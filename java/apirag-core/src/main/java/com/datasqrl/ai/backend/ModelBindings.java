package com.datasqrl.ai.backend;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public interface ModelBindings<Message, FunctionCall> {

  Message convertMessage(GenericChatMessage message);

  GenericChatMessage convertMessage(Message message, Map<String, Object> sessionContext);

  boolean isUserOrAssistantMessage(Message message);

  ModelAnalyzer<Message> getTokenCounter();

  int getMaxInputTokens();

  Message createSystemMessage(String systemMessage);

  String getFunctionName(FunctionCall functionCall);

  JsonNode getFunctionArguments(FunctionCall functionCall);

  Message newFunctionResultMessage(String functionName, String functionResult);

  Message convertExceptionToMessage(String s);

  String getTextContent(Message message);

  Message newUserMessage(String text);
}
