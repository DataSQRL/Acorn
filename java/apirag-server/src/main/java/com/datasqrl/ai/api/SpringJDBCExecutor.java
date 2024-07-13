package com.datasqrl.ai.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

public class SpringJDBCExecutor implements APIExecutor {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  final NamedParameterJdbcTemplate jdbcTemplate;

  public SpringJDBCExecutor(String url, String driverClass, String username, String password) {
    DataSource dataSource = createDataSource(url, driverClass, username, password);
    this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
  }

  private DataSource createDataSource(String url, String driverClass, String username, String password) {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName(driverClass);
    dataSource.setUrl(url);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    return dataSource;
  }

  @Override
  public void validate(APIQuery query) throws IllegalArgumentException {
    if (query.getQuery() == null || query.getQuery().isEmpty()) {
      throw new IllegalArgumentException("`query` cannot be empty");
    }
  }

  @Override
  public String executeQuery(APIQuery query, JsonNode arguments) {
    Map<String, Object> paramMap = objectMapper.convertValue(arguments, Map.class);
    List<Map<String, Object>> rows = jdbcTemplate.query(query.getQuery(), paramMap, new ColumnMapRowMapper());
    ArrayNode arrayNode = objectMapper.createArrayNode();

    for (Map<String, Object> row : rows) {
      ObjectNode jsonObject = objectMapper.convertValue(row, ObjectNode.class);
      arrayNode.add(jsonObject);
    }

    return arrayNode.toString();
  }
}