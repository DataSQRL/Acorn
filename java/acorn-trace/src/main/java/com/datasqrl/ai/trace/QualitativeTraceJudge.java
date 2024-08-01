package com.datasqrl.ai.trace;

import com.datasqrl.ai.function.UDFConverter;
import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.models.ChatProviderFactory;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.ModelObservability;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.ToolsBackendFactory;
import com.datasqrl.ai.trace.Trace.FunctionCall;
import com.datasqrl.ai.trace.Trace.Response;
import com.datasqrl.ai.util.ConfigurationUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.Value;
import org.apache.commons.configuration2.Configuration;

@Value
public class QualitativeTraceJudge implements TraceJudge{

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static final String MESSAGE_JUDGE_PROMPT = "You are given two answers. The first is the reference answer and the second is the provided answer. Judge the provided answer against the reference answer by calling the `judge` function with the required arguments to render your judgement.";
  public static final String FUNCTION_JUDGE_PROMPT = "You are given two sets of arguments to the same function. The first are the reference arguments and the second are the provided arguments. Judge the provided arguments against the reference arguments by calling the `judge` function. Evaluate correctness and quality by comparing pairs of arguments. Determine correctness by whether the argument pairs are substantially similar.";

  public static final String MESSAGE_TEMPLATE = "==REFERENCE==\n%s\n==PROVIDED==\n%s";

  ChatProvider functionJudge;
  ChatProvider messageJudge;

  public static QualitativeTraceJudge fromConfiguration(Configuration modelConfiguration) {

    ToolsBackend backend = ToolsBackendFactory.of(List.of(), Map.of(), Set.of());
    URL url = ConfigurationUtil.getResourceFile("judge-functions/quality-judge.json");
    UDFConverter.addClientFunction(backend, url);
    ChatProviderFactory factory = ChatProviderFactory.fromConfiguration(modelConfiguration);
    ChatProvider functionJudge = factory.create(modelConfiguration, backend, FUNCTION_JUDGE_PROMPT,
        ModelObservability.NOOP);
    ChatProvider messageJudge = factory.create(modelConfiguration, backend, MESSAGE_JUDGE_PROMPT,
        ModelObservability.NOOP);

    return new QualitativeTraceJudge(functionJudge, messageJudge);
  }

  @Override
  public QualitativeResult judge(Response reference, Response given) {
    GenericChatMessage result = messageJudge.chat(String.format(MESSAGE_TEMPLATE, reference.content(), given.content()),
        Context.of());
    return QualitativeResult.fromJson(result.getFunctionCall().getArguments());
  }

  @Override
  public QualitativeResult judge(FunctionCall reference, FunctionCall given) {
    Preconditions.checkArgument(reference.name().equalsIgnoreCase(given.name()),"Not the same functions");
    GenericChatMessage result = functionJudge.chat(String.format(MESSAGE_TEMPLATE, reference.arguments(), given.arguments()),
        Context.of());
    return QualitativeResult.fromJson(result.getFunctionCall().getArguments());
  }


  @Data
  public static class QualitativeResult implements Result {

    boolean correct;
    String incorrectAnalysis;
    int qualityScore;
    String qualityAnalysis;

    public static QualitativeResult fromJson(JsonNode node) {
      return objectMapper.convertValue(node, QualitativeResult.class);
    }

  }

}
