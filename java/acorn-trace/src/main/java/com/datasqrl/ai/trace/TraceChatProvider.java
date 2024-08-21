package com.datasqrl.ai.trace;

import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.GenericFunctionCall;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Value
public class TraceChatProvider implements ChatProvider {

  public TraceChatProvider(ChatProvider chatProvider, Trace.TraceBuilder traceBuilder) {
    this(chatProvider, traceBuilder, RequestThrottler.NONE);
  }

  public TraceChatProvider(ChatProvider chatProvider, Trace.TraceBuilder traceBuilder, RequestThrottler requestThrottler) {
    this.chatProvider = chatProvider;
    this.traceBuilder = traceBuilder;
    this.requestThrottler = requestThrottler;
  }

  ChatProvider chatProvider;
  Trace.TraceBuilder traceBuilder;
  RequestThrottler requestThrottler;

  @Override
  public GenericChatMessage chat(String message, Context context) {
    TraceContext tContext = TraceContext.convert(context);
    traceBuilder.entry(new Trace.Message(tContext.getRequestId(), message));
    GenericChatMessage result;
    try {
      result = chatProvider.chat(message, context);
    } catch (Exception e) {
        log.error("Chat Query failed", e);
        result = new GenericChatMessage("", "{\"error\": \"" + e.getMessage() + "\"}", "", null, null, null, null, null);
    }
      if (result.getFunctionCall() != null) {
        GenericFunctionCall fcall = result.getFunctionCall();
        traceBuilder.entry(new Trace.FunctionCall(tContext.getRequestId(), tContext.getInvocationId(),
            fcall.getName(), false, fcall.getArguments(), ""));
      } else {
        traceBuilder.entry(new Trace.Response(tContext.getRequestId(), result.getContent(), ""));
      }
    requestThrottler.observe(context);
    return result;
  }

  @Override
  public List<GenericChatMessage> getHistory(Context sessionContext, boolean includeFunctionCalls) {
    throw new UnsupportedOperationException("Should not retrieve history during trace generation");
  }
}
