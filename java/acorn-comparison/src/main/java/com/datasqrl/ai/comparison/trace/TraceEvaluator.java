package com.datasqrl.ai.comparison.trace;

import com.datasqrl.ai.comparison.eval.Equality;
import com.datasqrl.ai.comparison.eval.Evaluation;
import com.datasqrl.ai.comparison.eval.EvaluationFactory;
import com.datasqrl.ai.comparison.trace.Trace.Entry;
import com.datasqrl.ai.comparison.trace.Trace.FunctionCall;
import com.datasqrl.ai.comparison.trace.Trace.Response;
import com.datasqrl.ai.util.ConfigurationUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class TraceEvaluator {

  private final Evaluation defaultEval = new Equality();

  public boolean evaluate(Trace expected, Trace actual) {
    List<Entry> expectedEntries = expected.getEntries();
    List<Entry> actualEntries = actual.getEntries();
    if (expectedEntries.size() != actualEntries.size()) {
      return false;
    }
    for (int i = 0; i < expectedEntries.size(); i++) {
      Entry expectedEntry = expectedEntries.get(i);
      Entry actualEntry = actualEntries.get(i);
      if (!expectedEntry.getClass().equals(actualEntry.getClass())) {
        return false;
      }
      boolean eval = true;
      if (expectedEntry instanceof Response) {
        eval = evaluate((Response) expectedEntry, (Response) actualEntry);
      } else if (expectedEntry instanceof FunctionCall) {
        eval = evaluate((FunctionCall) expectedEntry, (FunctionCall) actualEntry);
      } //else we don't need to compare

      if (!eval) {
        return false;
      }
    }
    return true;
  }

  public boolean evaluate(Response expected, Response actual) {
    List<Evaluation> evals = expected.evals().stream().map(conf -> createEvaluation(conf.type(), conf.settings())).toList();
    if (evals.isEmpty()) evals = List.of(defaultEval);
    for (Evaluation eval : evals) {
      if (!eval.evaluate(expected.content(), actual.content())) {
        return false;
      }
    }
    return true;
  }

  public boolean evaluate(FunctionCall expected, FunctionCall actual) {
    //Compare by field
    Multimap<String, Evaluation> evalsByField = HashMultimap.create();
    expected.evals().forEach(conf -> {
      evalsByField.put(conf.field(), createEvaluation(conf.type(), conf.settings()));
    });
    Iterator<Map.Entry<String, JsonNode>> fields = expected.arguments().fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      String fieldName = field.getKey();
      JsonNode expectedValue = field.getValue();
      JsonNode providedValue = actual.arguments().get(fieldName);
      if (providedValue == null) {
        return false;
      }
      Collection<Evaluation> evals = evalsByField.get(fieldName);
      if (evals.isEmpty()) evals = List.of(defaultEval);
      for (Evaluation eval : evals) {
        if (!eval.evaluate(expectedValue, providedValue)) {
          return false;
        }
      }
    }
    return true;
  }

  private static Iterable<EvaluationFactory> evalFactories = ServiceLoader.load(EvaluationFactory.class);

  public static EvaluationFactory getEvalFactory(String type) {
    for (EvaluationFactory factory : evalFactories) {
      if (factory.getType().equalsIgnoreCase(type)) {
        return factory;
      }
    }
    throw new IllegalArgumentException("Could not find evaluation factory for type: " + type);
  }

  public static Evaluation createEvaluation(String type, JsonNode settings) {
    return getEvalFactory(type).create(ConfigurationUtil.jsonToConfiguration(settings));
  }

}
