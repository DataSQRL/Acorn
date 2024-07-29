package com.datasqrl.ai.comparison.trace;

import com.datasqrl.ai.comparison.trace.Trace.FunctionCall;
import com.datasqrl.ai.comparison.trace.Trace.Message;
import com.datasqrl.ai.comparison.trace.Trace.Response;
import com.datasqrl.ai.models.AbstractChatProvider;
import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.GenericFunctionCall;
import com.google.common.base.Preconditions;
import java.util.List;

public class TraceChatProvider<Message, FunctionCall> implements ChatProvider {

  AbstractChatProvider<Message, FunctionCall> chatProvider;
  Trace.TraceBuilder traceBuilder;

  @Override
  public GenericChatMessage chat(String message, Context context) {
    TraceContext tContext = TraceContext.convert(context);
    traceBuilder.entry(new Trace.Message(tContext.getRequestId(), message));
    GenericChatMessage result = chatProvider.chat(message, context);
    if (result.getFunctionCall()!=null) {
      GenericFunctionCall fcall = result.getFunctionCall();
      traceBuilder.entry(new Trace.FunctionCall(tContext.getRequestId(), tContext.getInvocationId(),
          fcall.getName(), false, fcall.getArguments(), List.of()));
    } else {
      traceBuilder.entry(new Response(tContext.getRequestId(), result.getContent(), List.of()));
    }
    return result;
  }

  @Override
  public List<GenericChatMessage> getHistory(Context sessionContext, boolean includeFunctionCalls) {
    throw new UnsupportedOperationException("Should not retrieve history during trace generation");
  }
}
