package com.datasqrl.ai.comparison.eval;

import com.datasqrl.ai.trace.Trace;
import com.datasqrl.ai.util.ConfigurationUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.SneakyThrows;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class TraceEvaluator {

  private final Evaluation defaultEval = new Equality();
  private final ObjectMapper mapper = new ObjectMapper();

  public boolean evaluate(String expectedTracePath, String actualTracePath) {
    return evaluate(loadTraceFromFile(expectedTracePath), loadTraceFromFile(actualTracePath));
  }

  public boolean evaluate(Trace expected, Trace actual) {
    List<Trace.Entry> expectedEntries = expected.getEntries();
    List<Trace.Entry> actualEntries = actual.getEntries();
    if (expectedEntries.size() != actualEntries.size()) {
      return false;
    }
    for (int i = 0; i < expectedEntries.size(); i++) {
      Trace.Entry expectedEntry = expectedEntries.get(i);
      Trace.Entry actualEntry = actualEntries.get(i);
      if (!expectedEntry.getClass().equals(actualEntry.getClass())) {
        return false;
      }
      boolean eval = true;
      if (expectedEntry instanceof Trace.Response) {
        eval = evaluate((Trace.Response) expectedEntry, (Trace.Response) actualEntry);
      } else if (expectedEntry instanceof Trace.FunctionCall) {
        eval = evaluate((Trace.FunctionCall) expectedEntry, (Trace.FunctionCall) actualEntry);
      } //else we don't need to compare

      if (!eval) {
        return false;
      }
    }
    return true;
  }

  public boolean evaluate(Trace.Response expected, Trace.Response actual) {
    List<Evaluation> evals = expected.evals().stream().map(conf -> createEvaluation(conf.type(), conf.settings())).toList();
    if (evals.isEmpty()) evals = List.of(defaultEval);
    for (Evaluation eval : evals) {
      if (!eval.evaluate(expected.content(), actual.content())) {
        return false;
      }
    }
    return true;
  }

  public boolean evaluate(Trace.FunctionCall expected, Trace.FunctionCall actual) {
    //Compare by field
    Multimap<String, Evaluation> evalsByField = HashMultimap.create();
    expected.evals().forEach(conf -> evalsByField.put(conf.field(), createEvaluation(conf.type(), conf.settings())));
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

  @SneakyThrows
  private Trace loadTraceFromFile(String fileName) {
    return mapper.readValue(Paths.get(fileName).toFile(), new TypeReference<Trace>() {
    });
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
