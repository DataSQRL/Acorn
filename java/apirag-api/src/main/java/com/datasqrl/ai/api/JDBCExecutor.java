package com.datasqrl.ai.api;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.Properties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.commons.collections.IteratorUtils;

@AllArgsConstructor
@Builder
public class JDBCExecutor implements APIExecutor {

  private final String url;
  private final String driver;
  private final String user;
  private final String password;
  private final Properties additionalProperties;

  private Properties combinedProperties;

  private synchronized Properties getJdbcProperties() {
    if (combinedProperties == null) {
      try {
        Class.forName(driver); // Load JDBC driver
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("Could not load JDBC driver", e);
      }
      combinedProperties = new Properties();
      combinedProperties.setProperty("user", user);
      combinedProperties.setProperty("password", password);
      combinedProperties.putAll(additionalProperties);
    }
    return combinedProperties;

  }

  @Override
  public String executeQuery(APIQuery query, JsonNode arguments) throws IOException {
    Map<String, String> parameters = convertJsonNodeToMap(arguments);
    try (Connection connection = DriverManager.getConnection(url, getJdbcProperties());
        PreparedStatement preparedStatement = createPreparedStatement(connection, query.getQuery(), parameters)) {
      preparedStatement.execute();
    } catch (SQLException e) {
      throw new IOException("Failed to query database", e);
    }
    return null;
  }

//  private PreparedStatement createPreparedStatement(Connection connection, String query, JsonNode parameters)
//      throws SQLException {
//    for (String key : IteratorUtils.toList(parameters.fieldNames())) {
//      query = query.replace(":" + key, "?");
//    }
//
//    PreparedStatement preparedStatement = connection.prepareStatement(query);
//    int index = 1;
//    for (String key : IteratorUtils.toList(parameters.fieldNames())) {
//      preparedStatement.setObject(index, parameters.get(key).asText());
//      index++;
//    }
//    return preparedStatement;
//  }

  @Override
  public void validate(APIQuery query) throws IllegalArgumentException {

  }

  public static Map<String, String> convertJsonNodeToMap(JsonNode jsonNode) {
    Map<String, String> map = new HashMap<>();
    jsonNode.fields().forEachRemaining(entry -> map.put(entry.getKey(), entry.getValue().asText()));
    return map;
  }


}