package com.datasqrl.ai.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpringJDBCExecutorTest {

  private static SpringJDBCExecutor executor;

  @BeforeAll
  static void setUp() {
    executor = new SpringJDBCExecutor("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "org.h2.Driver", "sa", "");
    NamedParameterJdbcTemplate jdbcTemplate = executor.jdbcTemplate;

    String createTableQuery = "CREATE TABLE USERS (ID INT PRIMARY KEY, NAME VARCHAR(255))";
    jdbcTemplate.getJdbcTemplate().execute(createTableQuery);

    String[] insertDataQueries = {
        "INSERT INTO USERS (ID, NAME) VALUES (1, 'John Doe')",
        "INSERT INTO USERS (ID, NAME) VALUES (2, 'Jane Smith')",
        "INSERT INTO USERS (ID, NAME) VALUES (3, 'Alice Johnson')",
        "INSERT INTO USERS (ID, NAME) VALUES (4, 'Bob Brown')"
    };
    for (String query : insertDataQueries) {
      jdbcTemplate.getJdbcTemplate().execute(query);
    }
  }

  @Test
  void validate_nullQuery() {
    APIQuery query = new APIQuery();
    query.setQuery(null);
    assertThrows(IllegalArgumentException.class, () -> executor.validate(query));
  }

  @Test
  void validate_emptyQuery() {
    APIQuery query = new APIQuery();
    query.setQuery("");
    assertThrows(IllegalArgumentException.class, () -> executor.validate(query));
  }

  @Test
  void executeQuery_OneArg() {
    APIQuery query = new APIQuery();
    query.setQuery("SELECT * FROM USERS WHERE ID = :id");

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode args = mapper.createObjectNode();
    args.put("id", 1);

    String result = executor.executeQuery(query, args);
    assertNotNull(result);
    assertTrue(result.contains("\"ID\":1"));
    assertTrue(result.contains("\"NAME\":\"John Doe\""));
  }

  @Test
  void executeQuery_TwoArgsOutOfOrder() {
    APIQuery query = new APIQuery();
    query.setQuery("SELECT * FROM USERS WHERE ID >= :id AND NAME = :name AND ID = :id");

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode args = mapper.createObjectNode();
    args.put("id", 1);
    args.put("name", "John Doe");

    String result = executor.executeQuery(query, args);
    assertNotNull(result);
    assertTrue(result.contains("\"ID\":1"));
    assertTrue(result.contains("\"NAME\":\"John Doe\""));
  }

  @Test
  @SneakyThrows
  void executeQuery_allUsersQuery() {
    APIQuery query = new APIQuery();
    query.setQuery("SELECT * FROM USERS ORDER BY ID");
    ObjectNode args = new ObjectMapper().createObjectNode();
    String result = executor.executeQuery(query, args);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode nodes = mapper.readTree(result);

    assertTrue(nodes.isArray());
    assertEquals(4, nodes.size());

    JsonNode user1 = nodes.get(0);
    assertEquals(1, user1.get("ID").asInt());
    assertEquals("John Doe", user1.get("NAME").asText());

    JsonNode user2 = nodes.get(1);
    assertEquals(2, user2.get("ID").asInt());
    assertEquals("Jane Smith", user2.get("NAME").asText());

    JsonNode user3 = nodes.get(2);
    assertEquals(3, user3.get("ID").asInt());
    assertEquals("Alice Johnson", user3.get("NAME").asText());

    JsonNode user4 = nodes.get(3);
    assertEquals(4, user4.get("ID").asInt());
    assertEquals("Bob Brown", user4.get("NAME").asText());
  }
}