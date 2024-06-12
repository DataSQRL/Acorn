package com.datasqrl.ai.models.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.Part;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

public class ProtobufUtils {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static JsonNode convertStructToJsonNode(Struct struct) {
    return convertMapToJsonNode(struct.getFieldsMap());
  }

  public static Struct convertJsonNodeToStruct(JsonNode node) {
    return Struct.newBuilder().putAllFields(convertJsonNodeToValueMap(node)).build();
  }

  private static JsonNode convertMapToJsonNode(Map<String, Value> map) {
    ObjectNode jsonNode = objectMapper.createObjectNode();
    for (Map.Entry<String, Value> entry : map.entrySet()) {
      String key = entry.getKey();
      Value value = entry.getValue();
      jsonNode.set(key, convertValueToJsonNode(value));
    }
    return jsonNode;
  }

  private static JsonNode convertValueToJsonNode(Value value) {
    switch (value.getKindCase()) {
      case NULL_VALUE:
        return objectMapper.nullNode();
      case BOOL_VALUE:
        return objectMapper.valueToTree(value.getBoolValue());
      case NUMBER_VALUE:
        return objectMapper.valueToTree(value.getNumberValue());
      case STRING_VALUE:
        return objectMapper.valueToTree(value.getStringValue());
      case STRUCT_VALUE:
        ObjectNode objectNode = objectMapper.createObjectNode();
        Struct struct = value.getStructValue();
        for (Map.Entry<String, Value> entry : struct.getFieldsMap().entrySet()) {
          objectNode.set(entry.getKey(), convertValueToJsonNode(entry.getValue()));
        }
        return objectNode;
      case LIST_VALUE:
        ArrayNode arrayNode = objectMapper.createArrayNode();
        ListValue listValue = value.getListValue();
        for (Value elementValue : listValue.getValuesList()) {
          arrayNode.add(convertValueToJsonNode(elementValue));
        }
        return arrayNode;
      default:
        throw new IllegalArgumentException("Unsupported Value kind: " + value.getKindCase());
    }
  }

  private static Map<String, Value> convertJsonNodeToValueMap(JsonNode node) {
    Map<String, Value> result = new HashMap<>();

    if (node.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        result.put(field.getKey(), convertJsonNodeToValue(field.getValue()));
      }
      return result;
    } else if (node.isArray()) {
      ListValue.Builder listBuilder = ListValue.newBuilder();
      for (JsonNode element : node) {
        listBuilder.addValues(convertJsonNodeToValue(element));
      }
      return Map.of("", Value.newBuilder().setListValue(listBuilder.build()).build());
    } else {
      throw new IllegalArgumentException("Unexpected JsonNode type: " + node);
    }
  }

  private static Value convertJsonNodeToValue(JsonNode node) {
    if (node.isNull()) {
      return Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
    } else if (node.isBoolean()) {
      return Value.newBuilder().setBoolValue(node.asBoolean()).build();
    } else if (node.isNumber()) {
      return Value.newBuilder().setNumberValue(node.asDouble()).build();
    } else if (node.isTextual()) {
      return Value.newBuilder().setStringValue(node.asText()).build();
    } else if (node.isObject()) {
      Struct.Builder structBuilder = Struct.newBuilder();
      structBuilder.putAllFields(convertJsonNodeToValueMap(node));
      return Value.newBuilder().setStructValue(structBuilder.build()).build();
    } else if (node.isArray()) {
      ListValue.Builder listBuilder = ListValue.newBuilder();
      for (JsonNode element : node) {
        listBuilder.addValues(convertJsonNodeToValue(element));
      }
      return Value.newBuilder().setListValue(listBuilder.build()).build();
    } else {
      throw new IllegalArgumentException("Unexpected JsonNode type: " + node);
    }
  }

  public static String contentToString(Content content) {
    return content.getPartsList().stream().map(Part::getText).collect(Collectors.joining("\n"));
  }

}
