package com.datasqrl.ai.comparison;

import com.datasqrl.ai.backend.GenericChatMessage;
import com.datasqrl.ai.comparison.config.ComparisonConfiguration;
import com.datasqrl.ai.models.ChatClientProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.datasqrl.ai.comparison.config.ComparisonConfiguration.MODEL_PREFIX;
import static com.datasqrl.ai.comparison.config.ComparisonConfiguration.MODEL_PROVIDER_KEY;

@Slf4j
public class SessionRunner {

  private final TestChatSession testSession;
  private final ChatClientProvider chatProvider;
  private final Function<String, Map<String, Object>> contextFunction;
  private final String modelName;
  private final AtomicInteger userId;
  private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
  private final ObjectMapper mapper = new ObjectMapper();

  public SessionRunner(ComparisonConfiguration configuration, TestChatSession testChatSession, AtomicInteger userId) {
    this.testSession = testChatSession;
    this.userId = userId;
    contextFunction = configuration.getContextFunction();
    chatProvider = configuration.getChatProvider();
    modelName = configuration.getModelConfiguration().getString(MODEL_PROVIDER_KEY) + "-" + configuration.getModelConfiguration().getString(MODEL_PREFIX);
  }

  public void run() {
    String fileName = modelName + "-" + getCurrentTime();
    String jsonFileName = fileName + ".json";
    String txtFileName = fileName + ".txt";
    log.info("Running session with userId: {}", userId.get());
    Map<String, Object> context = contextFunction.apply(Integer.toString(userId.get()));
    SessionLog sessionLog = runChatSession(testSession, context, txtFileName);
    writeToFile(sessionLog, jsonFileName);
  }

  private void writeToFile(String s, String fileName) {
    File file = new File(fileName);
    try (FileWriter fileWriter = new FileWriter(file, true)) {
      fileWriter.write(s);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeToFile(SessionLog sessionLog, String fileName) {
    File file = new File(fileName);
    try (FileWriter fileWriter = new FileWriter(file, true)) {
      SequenceWriter seqWriter = mapper.writer().writeValuesAsArray(fileWriter);
      seqWriter.write(sessionLog);
      seqWriter.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String getCurrentTime() {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    return sdf.format(timestamp);
  }

  private SessionLog runChatSession(TestChatSession session, Map<String, Object> context, String fileName) {
    List<SessionLog.LogEntry> sessions = new ArrayList<>();
    session.queries().forEach(query -> {
      log.info("Query: {}", query.query());
      GenericChatMessage response = chatProvider.chat(query.query(), context);
      log.info("Response: {}", response.getContent());
      SessionLog.LogEntry logEntry = logInteraction(query, response);
      String logString = serializeLogEntry(logEntry);
      writeToFile(logString, fileName);
      sessions.add(logEntry);
    });
    SessionLog logs = new SessionLog(sessions);
//    log.info("SessionLog: {}", logs);
    return logs;
  }

  private String serializeLogEntry(SessionLog.LogEntry logEntry) {
    return "Query: " + logEntry.query() + "\n"
        + "Expected Response: " + logEntry.expectedResponse() + "\n"
        + "Actual Response: " + logEntry.actualResponse() + "\n"
        + "Expected Answer Type: " + logEntry.expectedAnswerType() + "\n"
        + "Actual Answer Type: " + logEntry.actualAnswerType() + "\n\n\n";

  }

  private SessionLog.LogEntry logInteraction(TestChatSession.ChatQuery query, GenericChatMessage response) {
    TestChatSession.ChatQuery.AnswerType answerType =
        response.getFunctionCall() == null ? TestChatSession.ChatQuery.AnswerType.TEXT : TestChatSession.ChatQuery.AnswerType.FUNCTION_CALL;
    return new SessionLog.LogEntry(query.query(), query.expectedAnswer(), response.getContent(), query.expectedAnswerType(), answerType);
  }
}
