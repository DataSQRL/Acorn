package com.datasqrl.ai.trace;

import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.GenericFunctionCall;
import lombok.Value;
import org.apache.commons.configuration2.Configuration;

import java.util.List;
import java.util.Optional;

import static com.datasqrl.ai.models.ChatProviderFactory.MODEL_PROVIDER_KEY;

@Value
public class TraceChatProvider implements ChatProvider {

  public TraceChatProvider(ChatProvider chatProvider, Trace.TraceBuilder traceBuilder) {
    this(chatProvider, traceBuilder, RequestObserver.NONE);
  }

  public TraceChatProvider(ChatProvider chatProvider, Trace.TraceBuilder traceBuilder, RequestObserver requestObserver) {
    this.chatProvider = chatProvider;
    this.traceBuilder = traceBuilder;
    this.observer = requestObserver;
  }

  ChatProvider chatProvider;
  Trace.TraceBuilder traceBuilder;
  RequestObserver observer;

  @Override
  public GenericChatMessage chat(String message, Context context) {
    TraceContext tContext = TraceContext.convert(context);
    traceBuilder.entry(new Trace.Message(tContext.getRequestId(), message));
    GenericChatMessage result = chatProvider.chat(message, context);
    if (result.getFunctionCall()!=null) {
      GenericFunctionCall fcall = result.getFunctionCall();
      traceBuilder.entry(new Trace.FunctionCall(tContext.getRequestId(), tContext.getInvocationId(),
          fcall.getName(), false, fcall.getArguments(), ""));
    } else {
      traceBuilder.entry(new Trace.Response(tContext.getRequestId(), result.getContent(), ""));
    }
    observer.observe(context);
    return result;
  }

  @Override
  public List<GenericChatMessage> getHistory(Context sessionContext, boolean includeFunctionCalls) {
    throw new UnsupportedOperationException("Should not retrieve history during trace generation");
  }
}
