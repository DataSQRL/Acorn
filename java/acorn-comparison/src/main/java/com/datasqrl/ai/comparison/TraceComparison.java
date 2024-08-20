package com.datasqrl.ai.comparison;

import com.datasqrl.ai.comparison.config.ComparisonConfiguration;
import com.datasqrl.ai.models.AbstractChatProvider;
import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.tool.ToolManager;
import com.datasqrl.ai.trace.QualitativeTraceJudge;
import com.datasqrl.ai.trace.RequestObserver;
import com.datasqrl.ai.trace.Trace;
import com.datasqrl.ai.trace.TraceChatProvider;
import com.datasqrl.ai.trace.TraceComparisonResult;
import com.datasqrl.ai.trace.TraceContext;
import com.datasqrl.ai.trace.TraceEvaluator;
import com.datasqrl.ai.trace.TraceRecordingToolManager;
import com.datasqrl.ai.trace.TraceUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import nonapi.io.github.classgraph.classpath.ClassLoaderOrder;
import org.apache.commons.configuration2.MapConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.datasqrl.ai.comparison.config.ComparisonConfiguration.MODEL_PREFIX;
import static com.datasqrl.ai.models.ChatProviderFactory.MODEL_PROVIDER_KEY;

//TODO: Find better name
@Slf4j
@Value
public class TraceComparison {

  List<String> modelFiles;
  List<String> useCaseFolders;
  Trace referenceTrace;
  ObjectMapper mapper = new ObjectMapper();
  static int MODEL_RUNS = 2;
  static Path basePath = Path.of("experiments", "runs");
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'_'HH:mm");

  public TraceComparison(List<String> modelFiles, List<String> useCaseFolders, String referenceTraceFile) {
    this.modelFiles = modelFiles;
    this.useCaseFolders = useCaseFolders;
    this.referenceTrace = loadTraceFromFile(Paths.get(referenceTraceFile));
    log.info("modelFiles: {}\n useCaseFolders: {}\n referenceTraceFile: {}", modelFiles, useCaseFolders, referenceTraceFile);
  }

  public Path runTraces() throws Exception {
    Path runPath = basePath.resolve(getCurrentTime());
    log.info("Loaded reference Trace with {} entries", referenceTrace.getEntries().size());
    for (String useCaseFolder : useCaseFolders) {
      Path useCaseDir = Paths.get(useCaseFolder);
      log.info("Loading use case and tools from {}", useCaseFolder);
      Optional<Path> useCaseConfig = loadUseCaseConfig(useCaseDir);
      Optional<Path> tools = loadTools(useCaseDir);
      if (useCaseConfig.isPresent() && tools.isPresent()) {
        for (String modelConfigPath : modelFiles) {
          log.info("Loading model config from {}", modelConfigPath);
          SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
          ComparisonConfiguration configuration = ComparisonConfiguration.fromFile(Path.of(modelConfigPath), useCaseConfig.get(), tools.get(), meterRegistry);
          String modelName = configuration.getModelConfiguration().getString(MODEL_PROVIDER_KEY) + "-" + configuration.getModelConfiguration().getString(MODEL_PREFIX);
          Path modelPath = runPath.resolve(modelName);
          createDirectories(modelPath);
          for (int i = 0; i < MODEL_RUNS; i++) {
            Trace.TraceBuilder traceBuilder = Trace.builder();
            RequestObserver requestObserver = TraceUtil.waitingRequestObserver(configuration.getModelConfiguration().getString(MODEL_PROVIDER_KEY));
            ToolManager toolsBackend = new TraceRecordingToolManager(configuration.getToolManager(), traceBuilder, Optional.of(referenceTrace), requestObserver);
            ChatProvider chatProvider = new TraceChatProvider(configuration.getChatProvider(toolsBackend), traceBuilder, requestObserver);
            String id = UUID.randomUUID().toString();
            String fileName = id + "_" + modelName + ".trace.json";
            log.info("Running session {} with model {}", id, modelName);
            new SessionRunner(chatProvider, TraceContext.of(), referenceTrace).run();
            Trace trace = traceBuilder.referenceTraceId(referenceTrace.getId())
                .id(id)
                .build();
            writeTrace(trace, modelPath.resolve(fileName));
          }
          String metricsResults = ((MicrometerObservability) ((AbstractChatProvider<?, ?>) configuration.getChatProvider()).getObservability()).exportToCSV();
          log.info("Metrics results (CSV): {}", metricsResults);
          writeToFile(metricsResults, modelPath.resolve("metrics.csv"));
          meterRegistry.close();
        }
      } else {
        log.error("Could not load configuration and UseCase config from folder {}", useCaseFolder);
      }
    }
    return runPath;
  }

  public void evaluateTraces(Path runPath, MapConfiguration judgeConfig) throws IOException {
    QualitativeTraceJudge judge = QualitativeTraceJudge.fromConfiguration(judgeConfig);
    TraceEvaluator<QualitativeTraceJudge.QualitativeResult> evaluator = new TraceEvaluator<>(false, judge);

    List<Path> modelFolders;
    try (Stream<Path> stream = Files.list(runPath)) {
      modelFolders = stream.filter(Files::isDirectory)
          .toList();
    }

    for (Path modelFolder : modelFolders) {
      log.info("Evaluating model folder {}", modelFolder);
      List<Path> tracePaths;
      List<Path> comparisonFiles = new ArrayList<>();
      try (Stream<Path> stream = Files.list(modelFolder)) {
        tracePaths = stream
            .filter(Files::isRegularFile)
            .filter(path -> path.getFileName().toString().endsWith(".trace.json"))
            .toList();
      }
      for (Path path : tracePaths) {
        log.info("Evaluating model trace {}", path);
        Trace trace = loadTraceFromFile(path);
        Map<Trace.Entry, TraceComparisonResult> resultMap = evaluator.judgeOrCompare(referenceTrace, trace, false);
        String comparisonResult = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultMap);
        Path comparisonFile = modelFolder.resolve(trace.getId() + "_" + modelFolder.getFileName() + ".comparison.json");
        writeToFile(comparisonResult, comparisonFile);
        TraceComparisonResult result = evaluator.combinedJudgeOrCompare(referenceTrace, trace);
        log.info("Trace Comparison is correct: {}", result.isCorrect());
        comparisonFiles.add(comparisonFile);
      }
//      for (Path comparisonFile: comparisonFiles) {
//        TraceComparisonResult result = loadComparisonResultFromFile(comparisonFile);
//        String content = new String(Files.readAllBytes(comparisonFile));
////        log.info("Comparison result: {}", content);
//      }
    }
  }

  private void createDirectories(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SneakyThrows
  private void writeTrace(Trace trace, Path file) {
    Files.createDirectories(file.getParent());
    trace.writeToFile(file);
    log.info("Saved trace {} to file: {}", trace.getId(), file);
  }

  private void writeToFile(String message, Path file) {
    try {
      Files.write(file, message.getBytes());
    } catch (IOException e) {
      log.error("Could not write {} to file: {}", message, file, e);
    }
  }

  private Optional<Path> loadTools(Path useCaseDir) {
    try (Stream<Path> stream = Files.list(useCaseDir)) {
      return stream.filter(path -> !Files.isDirectory(path)).filter(path -> path.getFileName().toString().endsWith(".tools.json")).findFirst();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Optional<Path> loadUseCaseConfig(Path useCaseDir) {
    try (Stream<Path> stream = Files.list(useCaseDir)) {
      return stream.filter(path -> !Files.isDirectory(path)).filter(path -> path.getFileName().toString().endsWith(".config.json")).findFirst();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SneakyThrows
  private Trace loadTraceFromFile(Path path) {
    return mapper.readValue(path.toFile(), Trace.class);
  }

  @SneakyThrows
  private TraceComparisonResult loadComparisonResultFromFile(Path path) {
    return mapper.readValue(path.toFile(), TraceComparisonResult.class);
  }

  private String getCurrentTime() {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    return sdf.format(timestamp);
  }

  public static void main(String... args) throws Exception {
    if (args == null || args.length != 3)
      throw new IllegalArgumentException("Please provide a a model config folder, a use case folder and a reference trace file");

    List<String> modelFiles;
    List<String> useCaseFolders;
    String referenceTraceFile = args[2];
    try (Stream<Path> stream = Files.list(Paths.get(args[0]))) {
      modelFiles = stream
          .filter(file -> !Files.isDirectory(file))
          .filter(file -> file.getFileName().toString().endsWith(".json"))
          .map(Path::toString)
          .toList();
    }
    try (Stream<Path> stream = Files.list(Paths.get(args[1]))) {
      useCaseFolders = stream
          .filter(Files::isDirectory)
          .map(Path::toString)
          .toList();
    }

    MapConfiguration judgeConfig = new MapConfiguration(Map.of(
        "provider", "openai",
        "name", "gpt-4o-mini",
        "temperature", 0.2,
        "max_output_tokens", 512
    ));

    TraceComparison runner = new TraceComparison(modelFiles, useCaseFolders, referenceTraceFile);
    Path runPath = runner.runTraces();
    runner.evaluateTraces(runPath, judgeConfig);
//    runner.evaluateTraces(basePath.resolve("2024-08-20_17:28"), judgeConfig);

//    Trace referenceTrace = Trace.loadFromFile(Path.of(referenceTraceFile));
//    Trace trace = Trace.loadFromFile(Path.of(traceFile));
//    log.info("Loaded reference Trace with {} entries from {}", referenceTrace.getEntries().size(), referenceTraceFile);
//    log.info("Loaded Trace to compare with {} entries from {}", trace.getEntries().size(), traceFile);
//
//
//    log.info("Trace comparison results: {}", resultMap);
//    TraceComparisonResult result = evaluator.combinedJudgeOrCompare(referenceTrace, trace);
//    log.info("Combined trace comparison results:\ncorrect: {}\nmessage: {}", result.isCorrect(), result.getMessage());
  }

}