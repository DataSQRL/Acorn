package com.datasqrl.ai.trace;

import com.datasqrl.ai.trace.Trace.FunctionCall;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterators;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

public class TraceAssertions {

  public static final Double DELTA = 0.0001;

  public static void assertContainsIgnoreCase(String message, String... words) {
    String messageLower = message.toLowerCase();
    List<String> nomatch = Arrays.stream(words).map(String::toLowerCase).filter(w -> !messageLower.contains(w))
        .collect(Collectors.toUnmodifiableList());
    if (!nomatch.isEmpty()) throw new AssertionError("Message does not contain: " + nomatch);
  }

  public static void assertEquals(FunctionCall expected, FunctionCall given, boolean strict) {
    if (given == null || expected == null) throw new AssertionError("Function call not found");
    if (!expected.name().equalsIgnoreCase(given.name())) {
      throwAssertion("Function names are different", expected.name(), given.name());
    } else if (expected.internal()!=given.internal()) {
      throwAssertion("Function types are different", expected.internal(), given.internal());

    }
    assertEquals(expected.arguments(), given.arguments(), strict);
  }

  public static void assertEquals(JsonNode expectedArguments, JsonNode givenArguments, boolean strict) {
    Iterator<String> fieldNames = expectedArguments.fieldNames();
    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      if (!fieldEquals(expectedArguments.get(fieldName),givenArguments.get(fieldName))) {
        throwAssertion("Argument field " + fieldName + " is different", expectedArguments.get(fieldName), givenArguments.get(fieldName));
      }
    }
    if (strict && expectedArguments.size()!=givenArguments.size()) {
      throwAssertion("Given arguments contains extra fields", expectedArguments, givenArguments);
    }
  }

  public static void assertEquals(Map<String, Object> expectedArguments, JsonNode givenArguments, boolean strict) {
    for (String fieldName : expectedArguments.keySet()) {
      if (!fieldEquals(expectedArguments.get(fieldName),givenArguments.get(fieldName))) {
        throwAssertion("Argument field " + fieldName + " is different", expectedArguments.get(fieldName), givenArguments.get(fieldName));
      }
    }
    if (strict && expectedArguments.size()!=givenArguments.size()) {
      throwAssertion("Given arguments contains extra fields", expectedArguments, givenArguments);
    }
  }

  private static void throwAssertion(String message, Object expected, Object given) {
    throw new AssertionError(message + ": " + Objects.toString(expected) + " vs " + Objects.toString(given));
  }

  private static boolean fieldEquals(JsonNode expected, JsonNode provided) {
    if (expected.isNumber()) {
      if (!provided.isNumber()) return false;
      return Math.abs(expected.asDouble() - provided.asDouble()) < DELTA;
    } else if (expected.isArray()) {
      if (!provided.isArray()) return false;
      if (provided.size()!=expected.size()) return false;
      for (int i = 0; i < expected.size(); i++) {
        if (!fieldEquals(expected.get(i), provided.get(i))) return false;
      }
      return true;
    } else {
      return expected.equals(provided);
    }
  }

  private static boolean fieldEquals(Object expected, JsonNode provided) {
    if (expected instanceof Number expectedNumber) {
      if (!provided.isNumber()) return false;
      return Math.abs(expectedNumber.doubleValue() - provided.asDouble()) < DELTA;
    } else if (expected instanceof List expectedList) {
      if (!provided.isArray()) return false;
      if (provided.size()!=expectedList.size()) return false;
      for (int i = 0; i < expectedList.size(); i++) {
        if (!fieldEquals(expectedList.get(i), provided.get(i))) return false;
      }
      return true;
    } else {
      return expected.equals(provided);
    }
  }

  public static void assertSameFunctionCalls(Iterable<FunctionCall> expectedCalls, Trace given, boolean strict) {
    for (FunctionCall expectedCall : expectedCalls) {
      FunctionCall givenCall = given.getFunctionCall(expectedCall.requestId(),
          expectedCall.invocationId());
      if (givenCall==null) throw new AssertionError("Could not find function call: " + expectedCall);
      assertEquals(expectedCall, givenCall, strict);
    }
  }




}
