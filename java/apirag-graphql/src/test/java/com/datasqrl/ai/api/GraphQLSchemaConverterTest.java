package com.datasqrl.ai.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionBackendFactory;
import com.datasqrl.ai.backend.RuntimeFunctionDefinition;
import com.datasqrl.ai.util.ConfigurationUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class GraphQLSchemaConverterTest {


  @Test
  @SneakyThrows
  public void testNutshopSchemaConversion() {
    GraphQLSchemaConverter converter = new GraphQLSchemaConverter(Set.of());
    String schemaString = ConfigurationUtil.getResourcesFileAsString("nutshop-schema.graphqls");
    List<RuntimeFunctionDefinition> functions = converter.convert(schemaString);
    assertEquals(6, functions.size());
//    functions.forEach(System.out::println);

    FunctionBackend backend = FunctionBackendFactory.of(functions, Map.of(
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
    assertEquals(4, backend.getFunctions().size());
  }



}
