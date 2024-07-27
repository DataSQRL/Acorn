package com.datasqrl.ai.comparison.trace;

import com.datasqrl.ai.tool.ChatMessageInterface;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.tool.ToolManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public abstract class BaseTraceToolManager implements ToolManager {

  ToolManager manager;

  @Override
  public Map<String, RuntimeFunctionDefinition> getFunctions() {
    return manager.getFunctions();
  }

  //We don't use history during trace recording

  @Override
  public CompletableFuture<String> saveChatMessage(ChatMessageInterface message) {
    return CompletableFuture.completedFuture("Ignored");
  }

  @Override
  public <ChatMessage extends ChatMessageInterface> List<ChatMessage> getChatMessages(
      @NonNull Map<String, Object> context, int limit, @NonNull Class<ChatMessage> clazz) {
    return List.of();
  }

}
