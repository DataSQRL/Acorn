package com.datasqrl.ai.trace;

import com.datasqrl.ai.tool.ChatMessageInterface;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.FunctionValidation;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.tool.ToolManager;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.datasqrl.ai.models.ChatProviderFactory.MODEL_PROVIDER_KEY;

@Value
public class TraceRecordingToolManager implements ToolManager {

  @NonNull ToolManager manager;
  @NonNull Trace.TraceBuilder traceBuilder;
  @NonNull Optional<Trace> replayTrace;
  @NonNull Optional<Configuration> modelConfiguration;

  public TraceRecordingToolManager(@NonNull ToolManager manager, @NonNull Trace.TraceBuilder traceBuilder, @NonNull Optional<Trace> replayTrace) {
    this.manager = manager;
    this.traceBuilder = traceBuilder;
    this.replayTrace = replayTrace;
    modelConfiguration = Optional.empty();
  }

  public TraceRecordingToolManager(@NonNull ToolManager manager, @NonNull Trace.TraceBuilder traceBuilder, @NonNull Optional<Trace> replayTrace, @NonNull Optional<Configuration> modelConfiguration) {
    this.manager = manager;
    this.traceBuilder = traceBuilder;
    this.replayTrace = replayTrace;
    this.modelConfiguration = modelConfiguration;
  }

  @Override
  public FunctionValidation<String> validateFunctionCall(String functionName, JsonNode arguments) {
    return manager.validateFunctionCall(functionName, arguments);
  }

  @Override
  public String executeFunctionCall(String functionName, JsonNode arguments,
      @NonNull Context context) throws IOException {
    modelConfiguration.ifPresent(mapConfiguration -> TraceUtil.waitBetweenRequests(mapConfiguration.getString(MODEL_PROVIDER_KEY)));
    TraceContext tContext = TraceContext.convert(context);
    traceBuilder.entry(new Trace.FunctionCall(tContext.getRequestId(), tContext.getInvocationId(),
        functionName, true, arguments, ""));
    String result;
    if (replayTrace.isEmpty()) {
      result = manager.executeFunctionCall(functionName, arguments, context);
    } else {
      Trace.FunctionResponse response = findResponse(tContext);
      result = response.response();
    }
    traceBuilder.entry(new Trace.FunctionResponse(tContext.getRequestId(), tContext.getInvocationId(),functionName, result));
    return result;
  }

  private Trace.FunctionResponse findResponse(TraceContext tContext) {
    //For now, we make the assumption that invocation produces a single response
    return replayTrace.get().getEntries().stream().filter(e -> e instanceof Trace.FunctionResponse)
        .map(e -> (Trace.FunctionResponse) e)
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
