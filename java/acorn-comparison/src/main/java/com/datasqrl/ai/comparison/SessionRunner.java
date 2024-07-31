package com.datasqrl.ai.comparison;

import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.trace.Trace;
import com.datasqrl.ai.trace.TraceContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SessionRunner {

  private final ChatProvider chatProvider;
  private final String logName;
  private final AtomicInteger userId;
  private final ObjectMapper mapper = new ObjectMapper();
  private final TraceContext context;
  private final Trace trace;

  public SessionRunner(ChatProvider chatProvider, TraceContext context, Trace trace, AtomicInteger userId, String logName) {
    this.context = context;
    this.trace = trace;
    this.userId = userId;
    this.logName = logName;
    this.chatProvider = chatProvider;
  }

  public void run() {
    String jsonFileName = logName + ".json";
    String txtFileName = logName + ".txt";
    log.info("Running session with userId: {}", userId.get());
//    Context context = ContextConversion.getContextFromUserId(Integer.toString(userId.get()), contextKeys);
//    SessionLog sessionLog = runChatSession(trace, context, txtFileName);
    runChatSession(trace, context, txtFileName);
//    writeToFile(sessionLog, jsonFileName);
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

  private void runChatSession(Trace trace, TraceContext context, String fileName) {
//    List<SessionLog.LogEntry> sessions = new ArrayList<>();
    trace.getMessages().forEach(message -> {
      log.info("Query: {}", message.content());
      GenericChatMessage response;
      try {
         response = chatProvider.chat(message.content(), context);
         context.nextRequest();
        log.info("Response: {}", response.getContent());
      } catch (Exception e) {
        log.error("Query failed", e);
        response = GenericChatMessage.builder().content("Error: " + e.getMessage()).build();
      }
//      SessionLog.LogEntry logEntry = logInteraction(entry, response);
//      String logString = serializeLogEntry(logEntry);
//      writeToFile(logString, fileName);
//      sessions.add(logEntry);
    });
//    SessionLog logs = new SessionLog(sessions);
//    log.info("SessionLog: {}", logs);
//    return logs;
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
