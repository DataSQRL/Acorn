package com.datasqrl.ai.trace;

public interface TraceJudge {

  Result judge(Trace.Response reference, Trace.Response given);

  Result judge(Trace.FunctionCall reference, Trace.FunctionCall given);

  interface Result {

    boolean isCorrect();

  }

}
