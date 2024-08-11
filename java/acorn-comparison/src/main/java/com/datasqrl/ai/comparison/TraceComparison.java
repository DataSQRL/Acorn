package com.datasqrl.ai.comparison;

import com.datasqrl.ai.comparison.config.ComparisonConfiguration;
import com.datasqrl.ai.models.AbstractChatProvider;
import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.tool.ToolManager;
import com.datasqrl.ai.trace.QualitativeTraceJudge;
import com.datasqrl.ai.trace.Trace;
import com.datasqrl.ai.trace.TraceChatProvider;
import com.datasqrl.ai.trace.TraceComparisonResult;
import com.datasqrl.ai.trace.TraceContext;
import com.datasqrl.ai.trace.TraceEvaluator;
import com.datasqrl.ai.trace.TraceRecordingToolManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.MapConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.datasqrl.ai.comparison.config.ComparisonConfiguration.MODEL_PREFIX;
import static com.datasqrl.ai.models.ChatProviderFactory.MODEL_PROVIDER_KEY;

//TODO: Find better name
@Slf4j
@Value
public class TraceComparison {

//  List<String> modelFiles;
//  List<String> useCaseFolders;
//  String traceFile;
//  ObjectMapper mapper = new ObjectMapper();
//  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
//
//  public TraceComparison(List<String> modelFiles, List<String> useCaseFolders, String traceFile) {
//    this.modelFiles = modelFiles;
//    this.useCaseFolders = useCaseFolders;
//    this.traceFile = traceFile;
//    log.info("modelFiles: {}\n useCaseFolders: {}\n scriptFile: {}", modelFiles, useCaseFolders, traceFile);
//  }
//
//  public void start() throws IOException {
//    useCaseFolders.forEach(useCaseFolder -> {
//      log.info("Loading use case from {}", useCaseFolder);
//      Path useCaseDir = Paths.get(useCaseFolder);
//      Optional<Path> useCaseConfig = loadUseCaseConfig(useCaseDir);
//      Optional<Path> tools = loadTools(useCaseDir);
//      if (useCaseConfig.isPresent() && tools.isPresent()) {
//        Trace recordedTrace = loadTraceFromFile(traceFile);
//        log.info("Loaded recorded Trace with {} entries from {}", recordedTrace.getEntries().size(), traceFile);
//        modelFiles.forEach(modelConfig -> {
//          log.info("Loading model config from {}", modelConfig);
//          SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
//          ComparisonConfiguration configuration = ComparisonConfiguration.fromFile(Path.of(modelConfig), useCaseConfig.get(), tools.get(), meterRegistry);
//          Trace.TraceBuilder traceBuilder = Trace.builder();
//          ToolManager toolsBackend = new TraceRecordingToolManager(configuration.getToolManager(), traceBuilder, Optional.of(recordedTrace));
//          ChatProvider chatProvider = new TraceChatProvider(configuration.getChatProvider(toolsBackend), traceBuilder);
//          AtomicInteger idCounter = new AtomicInteger(0);
//          String modelName = configuration.getModelConfiguration().getString(MODEL_PROVIDER_KEY) + "-" + configuration.getModelConfiguration().getString(MODEL_PREFIX);
//          String fileName = modelName + "-" + getCurrentTime() + ".json";
//          new SessionRunner(chatProvider, TraceContext.of(), recordedTrace, idCounter).run();
//          Trace trace = traceBuilder.build();
//          writeTrace(trace, Path.of(fileName));
//          log.info("Metrics results (CSV): {}", ((MicrometerObservability) ((AbstractChatProvider<?, ?>) configuration.getChatProvider()).getObservability()).exportToCSV());
//          meterRegistry.close();
//        });
//      } else {
//        log.error("Could not load configuration and UseCase config from folder {}", useCaseFolder);
//      }
//    });
//  }

//  @SneakyThrows
//  private static void writeTrace(Trace trace, Path file) {
//    trace.writeToFile(file);
//  }
//
//  private Optional<Path> loadTools(Path useCaseDir) {
//    try (Stream<Path> stream = Files.list(useCaseDir)) {
//      return stream.filter(path -> !Files.isDirectory(path)).filter(path -> path.getFileName().toString().endsWith(".tools.json")).findFirst();
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
//  }
//
//  private Optional<Path> loadUseCaseConfig(Path useCaseDir) {
//    try (Stream<Path> stream = Files.list(useCaseDir)) {
//      return stream.filter(path -> !Files.isDirectory(path)).filter(path -> path.getFileName().toString().endsWith(".config.json")).findFirst();
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
//  }
//
//  @SneakyThrows
//  private Trace loadTraceFromFile(String fileName) {
//    return mapper.readValue(Paths.get(fileName).toFile(), Trace.class);
//  }
//
//  private String getCurrentTime() {
//    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
//    return sdf.format(timestamp);
//  }

  public static void main(String... args) throws Exception {
    if (args == null || args.length != 2)
      throw new IllegalArgumentException("Please provide a reference trace file and a trace file to compare");

    String referenceTraceFile = args[0];
    String traceFile = args[1];
    Trace referenceTrace = Trace.loadFromFile(Path.of(referenceTraceFile));
    Trace trace = Trace.loadFromFile(Path.of(traceFile));
    log.info("Loaded reference Trace with {} entries from {}", referenceTrace.getEntries().size(), referenceTraceFile);
    log.info("Loaded Trace to compare with {} entries from {}", trace.getEntries().size(), traceFile);

    Map<String,Object> modelConfig = Map.of(
        "provider", "openai",
        "name", "gpt-4o-mini",
        "temperature", 0.2,
        "max_output_tokens", 512
    );

    QualitativeTraceJudge judge = QualitativeTraceJudge.fromConfiguration(new MapConfiguration(modelConfig));
    TraceEvaluator<QualitativeTraceJudge.QualitativeResult> evaluator = new TraceEvaluator<>(false, judge);
    Map<Trace.Entry, TraceComparisonResult> resultMap = evaluator.judgeOrCompare(referenceTrace, trace);
    log.info("Trace comparison results: {}", resultMap);
    TraceComparisonResult result = evaluator.combinedJudgeOrCompare(referenceTrace, trace);
    log.info("Combined trace comparison results:\ncorrect: {}\nmessage: {}", result.isCorrect(), result.getMessage());
  }

}