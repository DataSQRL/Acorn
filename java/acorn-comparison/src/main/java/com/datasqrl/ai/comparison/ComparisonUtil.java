package com.datasqrl.ai.comparison;

import com.datasqrl.ai.trace.EntryComparisonEvaluation;
import com.datasqrl.ai.trace.QualitativeTraceJudge;
import com.datasqrl.ai.trace.Trace;
import com.datasqrl.ai.trace.TraceComparisonEvaluation;
import com.datasqrl.ai.trace.TraceComparisonResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
public class ComparisonUtil {

  static ObjectMapper mapper = new ObjectMapper();

  public static AggregatedComparisonResult aggregateComparisonMap(Map<Trace.Entry, TraceComparisonResult> resultMap) {
    long noResponses = resultMap.keySet().stream()
        .filter(entry -> entry instanceof Trace.Response)
        .count();
    long noFunctionCalls = resultMap.keySet().stream()
        .filter(entry -> entry instanceof Trace.FunctionCall)
        .count();
    double avgCorrectResponses = (double) resultMap.entrySet().stream()
        .filter(entry -> entry.getKey() instanceof Trace.Response && entry.getValue().isCorrect())
        .count() / noResponses;
    double avgCorrectFunctionCalls = (double) resultMap.entrySet().stream()
        .filter(entry -> entry.getKey() instanceof Trace.FunctionCall && entry.getValue().isCorrect())
        .count() / noFunctionCalls;
    double avgCorrect = (double) resultMap.values().stream()
        .filter(TraceComparisonResult::isCorrect)
        .count() / resultMap.size();
    double avgJudgeScore = resultMap.values().stream()
        .filter(result -> result instanceof QualitativeTraceJudge.QualitativeResult)
        .mapToDouble(result -> ((QualitativeTraceJudge.QualitativeResult) result).getQualityScore())
        .average()
        .orElse(0.0);
    return new AggregatedComparisonResult(1, Math.toIntExact(noResponses), Math.toIntExact(noFunctionCalls), avgCorrect, avgCorrectResponses, avgCorrectFunctionCalls, avgJudgeScore);
  }

  public static AggregatedComparisonResult convertToAggregatedResult(TraceComparisonEvaluation evaluation) {
    long noResponses = evaluation.evaluations().stream()
        .map(EntryComparisonEvaluation::reference)
        .filter(entry -> entry instanceof Trace.Response)
        .count();
    long noFunctionCalls = evaluation.evaluations().stream()
        .map(EntryComparisonEvaluation::reference)
        .filter(entry -> entry instanceof Trace.FunctionCall)
        .count();
    double avgCorrectResponses = (double) evaluation.evaluations().stream()
        .filter(eval -> eval.reference() instanceof Trace.Response && eval.comparisonResult().isCorrect())
        .count() / noResponses;
    double avgCorrectFunctionCalls = (double) evaluation.evaluations().stream()
        .filter(entry -> entry.reference() instanceof Trace.FunctionCall && entry.comparisonResult().isCorrect())
        .count() / noFunctionCalls;
    double avgCorrect = (double) evaluation.evaluations().stream()
        .map(EntryComparisonEvaluation::comparisonResult)
        .filter(TraceComparisonResult::isCorrect)
        .count() / evaluation.evaluations().size();
    double avgJudgeScore = evaluation.evaluations().stream()
        .map(EntryComparisonEvaluation::comparisonResult)
        .filter(result -> result instanceof QualitativeTraceJudge.QualitativeResult)
        .mapToDouble(result -> ((QualitativeTraceJudge.QualitativeResult) result).getQualityScore())
        .average()
        .orElse(0.0);
    return new AggregatedComparisonResult(1, Math.toIntExact(noResponses), Math.toIntExact(noFunctionCalls), avgCorrect, avgCorrectResponses, avgCorrectFunctionCalls, avgJudgeScore);
  }

  public static AggregatedComparisonResult aggregate(List<AggregatedComparisonResult> aggregatedComparisonResults) {
    int noRuns = aggregatedComparisonResults.size();
    long noResponses = aggregatedComparisonResults.stream()
        .mapToInt(AggregatedComparisonResult::noResponses)
        .sum();
    long noFunctionCalls = aggregatedComparisonResults.stream()
        .mapToInt(AggregatedComparisonResult::noFunctionCalls)
        .sum();
    double avgCorrectResponses = aggregatedComparisonResults.stream()
        .mapToDouble(AggregatedComparisonResult::avgCorrectResponses)
        .average()
        .orElse(0.0);
    double avgCorrectFunctionCalls = aggregatedComparisonResults.stream()
        .mapToDouble(AggregatedComparisonResult::avgCorrectFunctionCalls)
        .average()
        .orElse(0.0);
    double avgCorrect = aggregatedComparisonResults.stream()
        .mapToDouble(AggregatedComparisonResult::avgCorrect)
        .average()
        .orElse(0.0);
    double avgJudgeScore = aggregatedComparisonResults.stream()
        .mapToDouble(AggregatedComparisonResult::avgJudgeScore)
        .average()
        .orElse(0.0);
    return new AggregatedComparisonResult(noRuns, Math.toIntExact(noResponses), Math.toIntExact(noFunctionCalls), avgCorrect, avgCorrectResponses, avgCorrectFunctionCalls, avgJudgeScore);
  }

  public static void createDirectories(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SneakyThrows
  public static Trace loadTraceFromFile(Path path) {
    return mapper.readValue(path.toFile(), Trace.class);
  }

  @SneakyThrows
  public static void writeComparisonResultJson(AggregatedComparisonResult result, Path file) {
    Files.createDirectories(file.getParent());
    String resultTxt = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    writeToFile(resultTxt, file);
    log.info("Saved AggregatedComparisonResult to file: {}", file);
  }

  @SneakyThrows
  public static void writeComparisonResultCsv(AggregatedComparisonResult result, Path file) {
    Files.createDirectories(file.getParent());
    StringBuilder s = new StringBuilder();
    s.append("runs").append(", ");
    s.append("no.responses").append(", ");
    s.append("no.function.calls").append(", ");
    s.append("avg.correct").append(", ");
    s.append("avg.correct.responses").append(", ");
    s.append("avg.correct.function.calls").append(", ");
    s.append("avg.judge.score").append("\n");
    s.append(result.runs()).append(", ");
    s.append(result.noResponses()).append(", ");
    s.append(result.noFunctionCalls()).append(", ");
    s.append(result.avgCorrect()).append(", ");
    s.append(result.avgCorrectResponses()).append(", ");
    s.append(result.avgCorrectFunctionCalls()).append(", ");
    s.append(result.avgJudgeScore());
    writeToFile(s.toString(), file);
    log.info("Saved AggregatedComparisonResult to file: {}", file);
  }

  @SneakyThrows
  public static void writeTrace(Trace trace, Path file) {
    Files.createDirectories(file.getParent());
    trace.writeToFile(file);
    log.info("Saved trace {} to file: {}", trace.getId(), file);
  }

  public static void writeToFile(String text, Path file) {
    try {
      Files.write(file, text.getBytes());
    } catch (IOException e) {
      log.error("Could not write {} to file: {}", text, file, e);
    }
  }

}
