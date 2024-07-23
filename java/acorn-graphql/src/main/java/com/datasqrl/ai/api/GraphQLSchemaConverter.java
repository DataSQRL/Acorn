package com.datasqrl.ai.api;

import static graphql.Scalars.GraphQLString;

import com.datasqrl.ai.tool.FunctionDefinition;
import com.datasqrl.ai.tool.FunctionDefinition.Argument;
import com.datasqrl.ai.tool.FunctionDefinition.Parameters;
import com.datasqrl.ai.tool.FunctionType;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.util.ErrorHandling;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.SchemaPrinter;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.scalars.ExtendedScalars;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Converts a given GraphQL Schema to a tools configuration for the function backend.
 * It extracts all queries and mutations and converts them into {@link com.datasqrl.ai.tool.RuntimeFunctionDefinition}.
 */
@Value
@Slf4j
public class GraphQLSchemaConverter {

  Configuration configuration;
  String apiName;

  SchemaPrinter schemaPrinter = new SchemaPrinter(SchemaPrinter.Options.defaultOptions().descriptionsAsHashComments(true));

  public List<RuntimeFunctionDefinition> convert(String schemaString) {
    TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(schemaString);
    RuntimeWiring.Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring();
    getExtendedScalars().forEach(runtimeWiringBuilder::scalar);
    GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiringBuilder.build());

    List<RuntimeFunctionDefinition> functions = new ArrayList<>();

    GraphQLObjectType queryType = graphQLSchema.getQueryType();
    GraphQLObjectType mutationType = graphQLSchema.getMutationType();
    Stream.concat(queryType.getFieldDefinitions().stream().map(fieldDef -> Pair.of("query", fieldDef))
            ,mutationType.getFieldDefinitions().stream().map(fieldDef -> Pair.of("mutation", fieldDef)))
        .flatMap(input -> {
      try {
        return Stream.of(convert(input.getKey(), input.getValue()));
      } catch (Exception e) {
        log.error("Error converting query: {}", input.getValue(), e);
        return Stream.of();
      }
    }).forEach(functions::add);
    return functions;
  }

  public static List<GraphQLScalarType> getExtendedScalars() {
    List<GraphQLScalarType> scalars = new ArrayList<>();

    Field[] fields = ExtendedScalars.class.getFields();
    for (Field field : fields) {
      if (Modifier.isPublic(field.getModifiers()) &&
          Modifier.isStatic(field.getModifiers()) &&
          GraphQLScalarType.class.isAssignableFrom(field.getType())) {
        try {
          scalars.add((GraphQLScalarType) field.get(null));
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    }

    return scalars;
  }

  private record Context(String prefix, int numArgs) {}

  public RuntimeFunctionDefinition convert(String prefix, GraphQLFieldDefinition fieldDef) {
    FunctionDefinition funcDef = new FunctionDefinition();
    Parameters params = new Parameters();
    params.setType("object");
    params.setProperties(new HashMap<>());
    params.setRequired(new ArrayList<>());
    funcDef.setDescription(fieldDef.getDescription());
    funcDef.setName(fieldDef.getName());
    funcDef.setParameters(params);

    StringBuilder queryHeader = new StringBuilder(prefix).append(" ").append(fieldDef.getName()).append("(");
    StringBuilder queryBody = new StringBuilder();

    visit(fieldDef, queryBody, queryHeader, params, new Context("", 0));

    queryHeader.append(") {\n").append(queryBody).append("\n}");
    APIQuery apiQuery = new APIQuery();
    apiQuery.setQuery(queryHeader.toString());
    apiQuery.setName(apiName);
    return RuntimeFunctionDefinition.builder()
        .type(FunctionType.api)
        .function(funcDef)
        .api(apiQuery)
        .build();
  }

  private static String combineStrings(String prefix, String suffix) {
    return prefix + (prefix.isBlank()? "" : "_") + suffix;
  }


  private record UnwrappedType(GraphQLInputType type, boolean required) {}

  private UnwrappedType convertRequired(GraphQLInputType type) {
    boolean required = false;
    if (type instanceof GraphQLNonNull) {
      required = true;
      type = (GraphQLInputType) ((GraphQLNonNull) type).getWrappedType();
    }
    return new UnwrappedType(type, required);
  }

  private Argument convert(GraphQLInputType graphQLInputType) {
    Argument argument = new Argument();
    if (graphQLInputType instanceof GraphQLScalarType) {
      argument.setType(convertScalarTypeToJsonType((GraphQLScalarType) graphQLInputType));
    } else if (graphQLInputType instanceof GraphQLEnumType enumType) {
      argument.setType("string");
      argument.setEnumValues(enumType.getValues().stream().map(GraphQLEnumValueDefinition::getName).collect(Collectors.toSet()));
    } else if (graphQLInputType instanceof GraphQLList) {
      argument.setType("array");
      argument.setItems(convert(convertRequired((GraphQLInputType) ((GraphQLList) graphQLInputType).getWrappedType()).type()));
    } else {
      throw new UnsupportedOperationException("Unsupported type: " + graphQLInputType);
    }
    return argument;
  }

  public String convertScalarTypeToJsonType(GraphQLScalarType scalarType) {
    return switch (scalarType.getName()) {
      case "Int" -> "integer";
      case "Float" -> "number";
      case "String" -> "string";
      case "Boolean" -> "boolean";
      case "ID" -> "string"; // Typically treated as a string in JSON Schema
      default -> "string"; //We assume that type can be cast from string.
    };
  }


  public void visit(GraphQLFieldDefinition fieldDef, StringBuilder queryBody, StringBuilder queryHeader,
      Parameters params, Context ctx) {
    queryBody.append(fieldDef.getName());
    int numArgs = 0;
    if (!fieldDef.getArguments().isEmpty()) {
      queryBody.append("(");
      for (GraphQLArgument arg : fieldDef.getArguments()) {
        UnwrappedType unwrappedType = convertRequired(arg.getType());
        if (unwrappedType.type() instanceof GraphQLInputObjectType inputType) {
          queryBody.append(arg.getName()).append(": { ");
          for (GraphQLInputObjectField nestedField : inputType.getFieldDefinitions()) {
            String argName = combineStrings(ctx.prefix(), nestedField.getName());
            unwrappedType = convertRequired(nestedField.getType());
            argName = processField(queryBody, queryHeader, params, ctx, numArgs, unwrappedType,
                argName, nestedField.getName(), nestedField.getDescription());
            String typeString = printFieldType(nestedField);
            queryHeader.append(argName).append(": ").append(typeString);
            numArgs++;
          }
          queryBody.append(" }");
        } else {
          String argName = combineStrings(ctx.prefix(), arg.getName());
          argName = processField(queryBody, queryHeader, params, ctx, numArgs, unwrappedType, argName,
              arg.getName(), arg.getDescription());
          String typeString = printArgumentType(arg);
          queryHeader.append(argName).append(": ").append(typeString);
          numArgs++;
        }
      }
      queryBody.append(")");
    }
    GraphQLOutputType type = unwrapType(fieldDef.getType());
    if (type instanceof GraphQLObjectType) {
      queryBody.append(" {\n");
      for (GraphQLFieldDefinition nestedField : ((GraphQLObjectType)type).getFieldDefinitions()) {
        visit(nestedField, queryBody, queryHeader, params, new Context(combineStrings(ctx.prefix(), nestedField.getName()), ctx.numArgs() + numArgs));
      }
      queryBody.append("}\n");
    } else {
      queryBody.append("\n");
    }
  }

  private String processField(StringBuilder queryBody, StringBuilder queryHeader, Parameters params,
      Context ctx, int numArgs, UnwrappedType unwrappedType, String argName, String originalName,
      String description) {
    Argument argDef = convert(unwrappedType.type());
    argDef.setDescription(description);
    if (numArgs>0) queryBody.append(", ");
    if (ctx.numArgs() + numArgs > 0) queryHeader.append(", ");
    if (unwrappedType.required()) params.getRequired().add(argName);
    params.getProperties().put(argName, argDef);
    argName = "$" + argName;
    queryBody.append(originalName).append(": ").append(argName);
    return argName;
  }

  private String printFieldType(GraphQLInputObjectField field) {
    GraphQLInputObjectType type = GraphQLInputObjectType.newInputObject()
        .name("DummyType")
        .field(field)
        .build();
    // Print argument as part of a dummy field in a dummy schema
    String output = schemaPrinter.print(type);
    return extractTypeFromDummy(output, field.getName());
  }

  private String printArgumentType(GraphQLArgument argument) {
    GraphQLArgument argumentWithoutDescription = argument.transform(builder -> builder.description(null));
    GraphQLObjectType type = GraphQLObjectType.newObject()
        .name("DummyType")
        .field(field -> field
            .name("dummyField")
            .type(GraphQLString)
            .argument(argumentWithoutDescription)
        )
        .build();
    // Print argument as part of a dummy field in a dummy schema
    String output = schemaPrinter.print(type);
    return extractTypeFromDummy(output, argument.getName());
  }

  private String extractTypeFromDummy(String output, String fieldName) {
    //Remove comments
    output = Arrays.stream(output.split("\n"))
        .filter(line -> !line.trim().startsWith("#")).collect(Collectors.joining("\n"));
    Pattern pattern = Pattern.compile(fieldName + "\\s*:\\s*([^)}]+)");
    // Print argument as part of a dummy field in a dummy schema
    Matcher matcher = pattern.matcher(output);
    ErrorHandling.checkArgument(matcher.find(), "Could not find type in: %s", output);
    return matcher.group(1).trim();
  }


  private static GraphQLOutputType unwrapType(GraphQLOutputType type) {
    if (type instanceof GraphQLList) {
      return unwrapType((GraphQLOutputType) ((GraphQLList) type).getWrappedType());
    } else if (type instanceof GraphQLNonNull) {
      return unwrapType((GraphQLOutputType)((GraphQLNonNull) type).getWrappedType());
    } else {
      return type;
    }
  }

}