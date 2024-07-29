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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.Value;

public class TraceRecordingToolManager extends BaseTraceToolManager {

  private final Trace.TraceBuilder traceBuilder;
  private final Queue<FunctionResponse> replayResponses;

  public TraceRecordingToolManager(ToolManager manager, Trace.TraceBuilder traceBuilder, Trace replayTrace) {
    super(manager);
    this.traceBuilder = traceBuilder;
    if (replayTrace == null) {
      this.replayResponses = null;
    } else {
      this.replayResponses = new LinkedList<>();
      replayTrace.getEntries().stream().filter(entry -> entry instanceof FunctionResponse)
          .map(entry -> (FunctionResponse) entry).forEach(replayResponses::offer);
    }
  }

  @Override
  public FunctionValidation<String> validateFunctionCall(String functionName, JsonNode arguments) {
    return manager.validateFunctionCall(functionName, arguments);
  }

  @Override
  public String executeFunctionCall(String functionName, JsonNode arguments,
      @NonNull Context context) throws IOException {
    traceBuilder.entry(new FunctionCall(true, functionName, arguments, List.of()));
    String result;
    if (replayResponses == null) {
      result = manager.executeFunctionCall(functionName, arguments, context);
    } else {
      FunctionResponse response = replayResponses.poll();
      result = response.response();
    }
    traceBuilder.entry(new FunctionResponse(functionName, result));
    return result;
  }


}
