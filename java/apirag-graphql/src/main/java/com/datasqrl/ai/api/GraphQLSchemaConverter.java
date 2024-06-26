package com.datasqrl.ai.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeDefinition;
import graphql.parser.Parser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.SchemaParser;

import java.io.IOException;
import java.util.*;

/**
 * Converts a given GraphQL Schema to a tools configuration for the function backend.
 * It extracts all queries and mutations and converts them into {@link com.datasqrl.ai.backend.RuntimeFunctionDefinition}.
 */
public class GraphQLSchemaConverter {

  public static void convert(String graphQLSchema) throws IOException {

    Parser parser = new Parser();
    TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(graphQLSchema);

    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(new com.fasterxml.jackson.databind.ObjectMapper());

    for (TypeDefinition typeDef : typeRegistry.types().values()) {
      if (typeDef instanceof ObjectTypeDefinition) {
        ObjectTypeDefinition objectTypeDef = (ObjectTypeDefinition) typeDef;
        for (FieldDefinition fieldDef : objectTypeDef.getFieldDefinitions()) {
          if (fieldDef.getInputValueDefinitions().isEmpty()) {
            continue;
          }


          // Convert the arguments to a JSON schema
          List<Map<String, Object>> argsList = new ArrayList<>();
          fieldDef.getInputValueDefinitions().forEach(arg -> {
            Map<String, Object> argMap = new HashMap<>();
            argMap.put(arg.getName(), arg.getType());
            argsList.add(argMap);
          });

          SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
          objectMapper.acceptJsonFormatVisitor(objectMapper.constructType(argsList.getClass()), visitor);

          JsonSchema jsonSchema = visitor.finalSchema();
          System.out.println("Query: " + fieldDef.getName() + ", JSON Schema of Arguments: " + objectMapper.writeValueAsString(jsonSchema));
        }
      }
    }
  }
}