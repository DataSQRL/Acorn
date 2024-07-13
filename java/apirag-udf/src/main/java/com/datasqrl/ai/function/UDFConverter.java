package com.datasqrl.ai.function;

import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.FunctionDefinition;
import com.datasqrl.ai.tool.FunctionType;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.util.ErrorHandling;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;


public class UDFConverter {

  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Adds a user defined function to the provided repository.
   *
   * @param backend The backend
   * @param clazz
   */
  public static void addUserDefinedFunction(ToolsBackend backend, Class<? extends UserDefinedFunction> clazz) {
    backend.addFunction(getRuntimeFunctionDefinition(clazz));
  }

  public static void addClientFunction(ToolsBackend backend, URL functionDefinitionURL) {
    ErrorHandling.checkArgument(functionDefinitionURL!=null, "Invalid url: %s", functionDefinitionURL);
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      FunctionDefinition plotFunctionDef = objectMapper.readValue(functionDefinitionURL, FunctionDefinition.class);
      addClientFunction(backend, plotFunctionDef);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not read client function definition at: " + functionDefinitionURL, e);
    }
  }

  public static void addClientFunction(ToolsBackend backend, FunctionDefinition clientFunction) {
    backend.addFunction(RuntimeFunctionDefinition.builder()
        .type(FunctionType.client)
        .function(clientFunction)
        .context(List.of())
        .build());
  }

  public static RuntimeFunctionDefinition getRuntimeFunctionDefinition(Class<? extends UserDefinedFunction> clazz) {
    FunctionDefinition funcDef = null;
    try {
      funcDef = mapper.treeToValue(getFunctionDefinition(clazz),  FunctionDefinition.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    return RuntimeFunctionDefinition.builder()
        .type(FunctionType.local)
        .function(funcDef)
        .api(null)
        .context(List.of())
        .executable(getExecutableFunction(clazz))
        .build();
  }

  public static<T extends UserDefinedFunction> Function<JsonNode, Object> getExecutableFunction(Class<T> clazz) {
    return jsonNode -> {
      T parameters = null;
      try {
        parameters = mapper.treeToValue(jsonNode, clazz);
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Could not parse function parameters", e);
      }
      return parameters.execute();
    };
  }



  public static JsonNode getFunctionDefinition(Class<? extends UserDefinedFunction> clazz) {
    ObjectNode rootNode = mapper.createObjectNode();
    rootNode.put("name", clazz.getSimpleName());
    getFunctionDescription(clazz).ifPresent(desc -> rootNode.put("description", desc));
    rootNode.set("parameters", generateParameters(clazz));
    return rootNode;
  }

  public static JsonNode generateParameters(Class<? extends UserDefinedFunction> clazz) {
    JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
    JsonNode jsonSchema = null;
    try {
      JsonSchema schema = schemaGen.generateSchema(clazz);
      jsonSchema = mapper.valueToTree(schema);
      ((ObjectNode) jsonSchema).remove("id");
      //Add list of "required" properties based on @Nonnull annotation on clazz
      List<String> requiredFields = Arrays.stream(clazz.getDeclaredFields())
          .filter(field -> field.isAnnotationPresent(Nonnull.class))
          .map(Field::getName).toList();
      if (!requiredFields.isEmpty()) {
        ArrayNode array = ((ObjectNode) jsonSchema).putArray("required");
        requiredFields.forEach(array::add);
      }
    } catch (JsonMappingException e) {
      throw new IllegalArgumentException("Could not generate schema for class " + clazz, e);
    }
    return jsonSchema;
  }

  public static Optional<String> getFunctionDescription(Class<?> clazz) {
    if (clazz.isAnnotationPresent(FunctionDescription.class)) {
      FunctionDescription description = clazz.getAnnotation(FunctionDescription.class);
      return Optional.of(description.value());
    } else {
      return Optional.empty();
    }
  }

}
