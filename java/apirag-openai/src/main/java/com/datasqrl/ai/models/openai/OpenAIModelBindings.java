package com.datasqrl.ai.models.openai;

import com.datasqrl.ai.backend.GenericChatMessage;
import com.datasqrl.ai.backend.ModelAnalyzer;
import com.datasqrl.ai.backend.ModelBindings;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.completion.chat.FunctionMessage;
import com.theokanning.openai.completion.chat.SystemMessage;
import com.theokanning.openai.completion.chat.ToolMessage;
import com.theokanning.openai.completion.chat.UserMessage;

import java.time.Instant;
import java.util.Map;

public class OpenAIModelBindings implements ModelBindings<ChatMessage> {

  OpenAITokenCounter tokenCounter;

  public OpenAIModelBindings(OpenAiChatModel model) {
    this.tokenCounter = OpenAITokenCounter.of(model);
  }


  @Override
  public ChatMessage convertMessage(GenericChatMessage message) {
    ChatMessageRole role = ChatMessageRole.valueOf(message.getRole().toUpperCase());
    //Parse function call?
    return switch (role) {
      case SYSTEM -> new SystemMessage(message.getContent(), message.getName());
      case USER -> new UserMessage(message.getContent(), message.getName());
      case ASSISTANT -> new AssistantMessage(message.getContent(), message.getName());
      case FUNCTION -> new FunctionMessage(message.getContent(), message.getName());
      case TOOL -> new ToolMessage(message.getContent(), message.getName());
    };
  }


  @Override
  public GenericChatMessage convertMessage(ChatMessage msg, Map<String, Object> sessionContext) {
    ChatFunctionCall fctCall = null;
    if (ChatMessageRole.valueOf(msg.getRole().toUpperCase()) == ChatMessageRole.ASSISTANT) {
      fctCall = ((AssistantMessage) msg).getFunctionCall();
    }
    return GenericChatMessage.builder()
        .role(msg.getRole())
        .content(fctCall == null ? msg.getTextContent() : functionCall2String(fctCall))
        .name(msg.getName())
        .context(sessionContext)
        .timestamp(Instant.now().toString())
        .numTokens(tokenCounter.countTokens(msg))
        .build();
  }

  @Override
  public boolean isUserOrAssistantMessage(ChatMessage chatMessage) {
    return ChatMessageRole.valueOf(chatMessage.getRole().toUpperCase()) == ChatMessageRole.ASSISTANT
    || ChatMessageRole.valueOf(chatMessage.getRole().toUpperCase()) == ChatMessageRole.USER;
  }

  @Override
  public ModelAnalyzer<ChatMessage> getTokenizer() {
    return tokenCounter;
  }

  private static String functionCall2String(ChatFunctionCall fctCall) {
    return fctCall.getName() + "@" + fctCall.getArguments().toPrettyString();
  }

}
