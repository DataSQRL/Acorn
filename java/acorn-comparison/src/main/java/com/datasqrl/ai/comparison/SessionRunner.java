package com.datasqrl.ai.comparison;

import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.trace.Trace;
import com.datasqrl.ai.trace.TraceContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SessionRunner {

  private final ChatProvider chatProvider;
  private TraceContext context;
  private final Trace referenceTrace;

  public SessionRunner(ChatProvider chatProvider, TraceContext context, Trace referenceTrace) {
    this.context = context;
    this.referenceTrace = referenceTrace;
    this.chatProvider = chatProvider;
  }

  public void run() {
    referenceTrace.getMessages().forEach(message -> {
      log.info("Query: {}", message.content());
      GenericChatMessage response;
      try {
         response = chatProvider.chat(message.content(), context);
         context = context.nextRequest();
        log.info("Response: {}", response.getContent());
      } catch (Exception e) {
        log.error("Query failed", e);
        context.nextRequest();
      }
    });
  }
}
