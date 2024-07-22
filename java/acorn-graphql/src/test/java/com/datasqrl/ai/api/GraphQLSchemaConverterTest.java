package com.datasqrl.ai.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.ToolsBackendFactory;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.util.ConfigurationUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.junit.jupiter.api.Test;

public class GraphQLSchemaConverterTest {


  @Test
  @SneakyThrows
  public void testNutshopSchemaFunctions() {
    GraphQLSchemaConverter converter = new GraphQLSchemaConverter(new PropertiesConfiguration(), APIExecutorFactory.DEFAULT_NAME);
    String schemaString = ConfigurationUtil.getResourcesFileAsString(
        "graphql/nutshop-schema.graphqls");
    List<RuntimeFunctionDefinition> functions = converter.convert(schemaString);
    assertEquals(7, functions.size());
//    functions.forEach(System.out::println);

    ToolsBackend backend = ToolsBackendFactory.of(functions, Map.of(
        APIExecutorFactory.DEFAULT_NAME, new APIExecutor() {
          @Override
          public void validate(APIQuery query) throws IllegalArgumentException {
            assertFalse(query.getQuery().isBlank());
          }

          @Override
          public String executeQuery(APIQuery query, JsonNode arguments) throws IOException {
            return "";
          }
        }));
    //Save and get messages aren't counted as functions
    assertEquals(5, backend.getFunctions().size());
  }


  @Test
  @SneakyThrows
  public void testNutshopSchemaConversion() {
    String schemaString = ConfigurationUtil.getResourcesFileAsString(
        "graphql/nutshop-schema.graphqls");
    String result = GraphQLSchemaConverterExecutable.convert(schemaString, "default");
    String expectedResult = ConfigurationUtil.getResourcesFileAsString(
      "graphql/nutshop-schema.tools.json");
    assertEquals(expectedResult, result);
    List<RuntimeFunctionDefinition> functions = ToolsBackendFactory.readTools(result);
    assertEquals(7, functions.size());
  }


}