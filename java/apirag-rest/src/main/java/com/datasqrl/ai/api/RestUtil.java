package com.datasqrl.ai.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.SneakyThrows;


public class RestUtil {

  static record RestCall(String path, String method, JsonNode body) {}

  @SneakyThrows
  public static RestCall createRestCall(APIQuery query, JsonNode arguments) {
    Set<String> pathParams = extractPathParameters(query.getPath());
    String expandedPath = query.getPath();
    for (String pathParam : pathParams) {
      String replaceValue = arguments.get(pathParam).asText();
      replaceValue = URLEncoder.encode(replaceValue, StandardCharsets.UTF_8);
      String placeholder = String.format("{%s}", pathParam);
      expandedPath = expandedPath.replace(placeholder, replaceValue);
    }

    JsonNode body = removeFields(arguments, pathParams);
    return new RestCall(expandedPath, query.getMethod().trim().toUpperCase(), body);
  }

  /**
   * Creates a deep copy of the json object but removes the specified fields
   *
   * @param json
   * @param fields
   * @return
   */
  static JsonNode removeFields(JsonNode json, Set<String> fields) {
    if (fields.isEmpty()) return json;
    ObjectNode copy = json.deepCopy();
    fields.forEach(copy::remove);
    return copy;
  }

  private static final Pattern PATH_PATTERN = Pattern.compile("\\{(.*?)}");

  public static Set<String> extractPathParameters(String path) {
    Matcher matcher = PATH_PATTERN.matcher(path);
    Set<String> parameters = new HashSet<>();
    while (matcher.find()) {
      parameters.add(matcher.group(1));
    }
    return parameters;
  }


}
