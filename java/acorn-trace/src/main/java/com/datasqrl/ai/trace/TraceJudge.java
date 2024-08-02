package com.datasqrl.ai.trace;

import com.datasqrl.ai.trace.TraceJudge.Result;
import java.util.ArrayList;
import java.util.List;

public interface TraceJudge<R extends Result> {

  R judge(Trace.Response reference, Trace.Response given);

  R judge(Trace.FunctionCall reference, Trace.FunctionCall given);

  default List<R> judgeAllResponses(Trace reference, Trace given) {
    List<R> results = new ArrayList<>();
    for (Trace.Response response: reference.getAll(Trace.Response.class)) {
      results.add(judge(response, given.getResponse(response.requestId())));
    }
    return results;
  }

  interface Result {

    boolean isCorrect();

  }

}
