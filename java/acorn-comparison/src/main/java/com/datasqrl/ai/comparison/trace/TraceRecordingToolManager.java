package com.datasqrl.ai.comparison.trace;

import com.datasqrl.ai.comparison.trace.Trace.FunctionCall;
import com.datasqrl.ai.comparison.trace.Trace.FunctionResponse;
import com.datasqrl.ai.tool.ChatMessageInterface;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.FunctionValidation;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.tool.ToolManager;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import lombok.NonNull;
import lombok.Value;

@Value
public class TraceRecordingToolManager implements ToolManager {

  @NonNull ToolManager manager;
  @NonNull Trace.TraceBuilder traceBuilder;
  @NonNull Optional<Trace> replayTrace;


  @Override
  public FunctionValidation<String> validateFunctionCall(String functionName, JsonNode arguments) {
    return manager.validateFunctionCall(functionName, arguments);
  }

  @Override
  public String executeFunctionCall(String functionName, JsonNode arguments,
      @NonNull Context context) throws IOException {
    TraceContext tContext = TraceContext.convert(context);
    traceBuilder.entry(new FunctionCall(tContext.getRequestId(), tContext.getInvocationId(),
        functionName, true, arguments, List.of()));
    String result;
    if (replayTrace.isEmpty()) {
      result = manager.executeFunctionCall(functionName, arguments, context);
    } else {
      FunctionResponse response = findResponse(tContext);
      result = response.response();
    }
    traceBuilder.entry(new FunctionResponse(tContext.getRequestId(), tContext.getInvocationId(),functionName, result));
    return result;
  }

  private FunctionResponse findResponse(TraceContext tContext) {
    //For now, we make the assumption that invocation produces a single response
    return replayTrace.get().getEntries().stream().filter(e -> e instanceof FunctionResponse)
        .map(e -> (FunctionResponse) e)
        .filter(r -> r.requestId() == tContext.getRequestId() && r.invocationId() == tContext.getInvocationId())
        .findFirst()
        .orElseThrow();
  }

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
      @NonNull Context context, int limit, @NonNull Class<ChatMessage> clazz) {
    return List.of();
  }


}
