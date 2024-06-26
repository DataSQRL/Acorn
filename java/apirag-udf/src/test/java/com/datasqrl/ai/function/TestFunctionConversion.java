package com.datasqrl.ai.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.datasqrl.ai.backend.RuntimeFunctionDefinition;
import com.datasqrl.ai.function.builtin.CurrentTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class TestFunctionConversion {

  public static final ObjectMapper mapper = new ObjectMapper();

  @Test
  @SneakyThrows
  public void testUDFSchema() {
    RuntimeFunctionDefinition function = UDFConverter.getRuntimeFunctionDefinition(TestAddFunction.class);
    assertEquals("TestAddFunction", function.getName());
    //Create a Json node with two fields: arg1 and arg2
    JsonNode jsonNode = mapper.createObjectNode().put("arg1",2).put("arg2", 3);
    assertEquals(5.0, (double)function.getExecutable().apply(jsonNode), 0.00001);
    System.out.println(function);
  }

  @Test
  public void testCurrentTime() {
    RuntimeFunctionDefinition function = UDFConverter.getRuntimeFunctionDefinition(CurrentTime.class);
    String time = function.getExecutable().apply(mapper.createObjectNode()).toString();
    System.out.println(time);
    assertFalse(time.isBlank());
  }

}
