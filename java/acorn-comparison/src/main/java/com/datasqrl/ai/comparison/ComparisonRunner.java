package com.datasqrl.ai.comparison;

import com.datasqrl.ai.comparison.config.ComparisonConfiguration;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.datasqrl.ai.comparison.config.ComparisonConfiguration.MODEL_PREFIX;
import static com.datasqrl.ai.models.ChatProviderFactory.MODEL_PROVIDER_KEY;

@Slf4j
@Value
public class ComparisonRunner {

  List<String> modelFiles;
  List<String> useCaseFolders;
  String scriptFile;
  ObjectMapper mapper = new ObjectMapper();
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  public ComparisonRunner(List<String> modelFiles, List<String> useCaseFolders, String scriptFile) {
    this.modelFiles = modelFiles;
    this.useCaseFolders = useCaseFolders;
    this.scriptFile = scriptFile;
    log.info("modelFiles: {}\n useCaseFolders: {}\n scriptFile: {}", modelFiles, useCaseFolders, scriptFile);
  }

  public void start() throws IOException {
    useCaseFolders.forEach(useCaseFolder -> {
      log.info("Loading use case from {}", useCaseFolder);
      Path useCaseDir = Paths.get(useCaseFolder);
      Optional<Path> useCaseConfig = loadUseCaseConfig(useCaseDir);
      Optional<Path> tools = loadTools(useCaseDir);
      if (useCaseConfig.isPresent() && tools.isPresent()) {
        List<TestChatSession> testSessions = loadTestChatSessionsFromFile(scriptFile);
        log.info("Loaded {} test sessions from {}", testSessions.size(), scriptFile);
        modelFiles.forEach(modelConfig -> {
          log.info("Loading model config from {}", modelConfig);
          SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
          ComparisonConfiguration configuration = ComparisonConfiguration.fromFile(Path.of(modelConfig), useCaseConfig.get(), tools.get(), meterRegistry);
          AtomicInteger idCounter = new AtomicInteger(0);
          String modelName = configuration.getModelConfiguration().getString(MODEL_PROVIDER_KEY) + "-" + configuration.getModelConfiguration().getString(MODEL_PREFIX);
          String fileName = modelName + "-" + getCurrentTime();
          testSessions.forEach(session -> {
            new SessionRunner(configuration, session, idCounter, fileName).run();
          });
          log.info("Metrics results (CSV): {}", ((MicrometerObservability)configuration.getChatProvider().getObservability()).exportToCSV());
          meterRegistry.close();
        });
      } else {
        log.error("Could not load configuration and UseCase config from folder {}", useCaseFolder);
      }
    });
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

  private List<TestChatSession> loadTestChatSessionsFromFile(String fileName) {
    try {
      return mapper.readValue(Paths.get(fileName).toFile(), new TypeReference<List<TestChatSession>>() {
      });
    } catch (Exception ex) {
      log.error("Could not read test chat sessions from {}", fileName, ex);
    }
    return List.of();
  }

  private String getCurrentTime() {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    return sdf.format(timestamp);
  }

  public static void main(String... args) throws Exception {
    if (args == null || args.length != 3)
      throw new IllegalArgumentException("Please provide a models folder, a use case folder and a script file");
    List<String> modelFiles;
    List<String> useCaseFolders;
    String scriptFile = args[2];
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

    ComparisonRunner runner = new ComparisonRunner(modelFiles, useCaseFolders, scriptFile);
    runner.start();
  }

  private static class CustomLoggingMeterRegistry extends LoggingMeterRegistry {
    @Override
    public void publish() {
      super.publish();
    }
  }

}