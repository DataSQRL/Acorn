package com.datasqrl.ai.trace;

import com.datasqrl.ai.function.FunctionDescription;
import com.datasqrl.ai.function.UDFConverter;
import com.datasqrl.ai.function.UserDefinedFunction;
import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.models.ChatProviderFactory;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.ModelObservability;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.ToolsBackendFactory;
import com.datasqrl.ai.trace.QualitativeTraceJudge.QualitativeResult;
import com.datasqrl.ai.trace.Trace.FunctionCall;
import com.datasqrl.ai.trace.Trace.Response;
import com.datasqrl.ai.util.ConfigurationUtil;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.configuration2.Configuration;

@Value
public class QualitativeTraceJudge implements TraceJudge<QualitativeResult> {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static final String MESSAGE_JUDGE_PROMPT = "You are given two answers. The first is the reference answer and the second is the provided answer. You must call the `judge` function with the required arguments to judge the provided answer against the reference answer.";
  public static final String FUNCTION_JUDGE_PROMPT = "You are given two sets of arguments to the same function. The first are the reference arguments and the second are the provided arguments. You must call the `judge` function with the required arguments to judge the provided arguments against the reference arguments.Evaluate correctness and quality by comparing pairs of arguments. Determine correctness by whether the argument pairs are substantially similar.";

  public static final String MESSAGE_TEMPLATE = "==REFERENCE==\n%s\n==PROVIDED==\n%s";

  ChatProvider functionJudge;
  ChatProvider messageJudge;

  public static QualitativeTraceJudge fromConfiguration(Configuration modelConfiguration) {
    RuntimeFunctionDefinition judgeFunction = UDFConverter.getRuntimeFunctionDefinition(
        QualitativeResult.class);
    ToolsBackend backend = ToolsBackendFactory.of(List.of(judgeFunction), Map.of(), Set.of());
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
    return UDFConverter.getFunctionCall(result.getFunctionCall(), QualitativeResult.class);
  }

  @Override
  public QualitativeResult judge(FunctionCall reference, FunctionCall given) {
    Preconditions.checkArgument(reference.name().equalsIgnoreCase(given.name()),"Not the same functions");
    GenericChatMessage result = functionJudge.chat(String.format(MESSAGE_TEMPLATE, reference.arguments(), given.arguments()),
        Context.of());
    return UDFConverter.getFunctionCall(result.getFunctionCall(), QualitativeResult.class);
  }

  @FunctionDescription("Judge the provided answer against the reference answer")
  @Data
  @NoArgsConstructor
  public static class QualitativeResult implements Result, UserDefinedFunction {

    @JsonPropertyDescription("Whether the provided answer is factually correct compared to the reference answer.")
    @Nonnull
    boolean correct;
    @JsonPropertyDescription("If the provided answer is incorrect, provide an analysis of why and how it is incorrect. If the provided answer is correct, this should be empty.")
    String incorrectAnalysis;
    @JsonPropertyDescription("Evaluate the quality of the provided answer compared to the reference answer in terms of comprehensiveness, ease of understanding, and clarity and return a quality score between 0 (the provided answer has much lower quality) to 5 (the provided answer has identical or better quality).")
    @Nonnull
    int qualityScore;
    @JsonPropertyDescription("Provide an analysis that justifies the quality score.")
    @Nonnull
    String qualityAnalysis;

    @Override
    public Object execute() {
      throw new UnsupportedOperationException("Not a locally executable function");
    }

    public void assertCorrect() {
      if (!correct) throw new AssertionError(incorrectAnalysis);
    }
    public static boolean isLocalFunction() {
      return true;
    }
  }

}
