package com.datasqrl.ai.comparison.api;

import com.datasqrl.ai.api.APIExecutor;
import com.datasqrl.ai.api.APIQuery;
import com.datasqrl.ai.util.ErrorHandling;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Value
public class StaticAPIExecutor implements APIExecutor {

  Map<String, Map<String, String>> localResults;
  String useCase;

  public StaticAPIExecutor(String useCase) {
    this.useCase = useCase;
    localResults = new HashMap<>();
    Map<String, String> creditCardResults = new HashMap<>();
    creditCardResults.put("GetTransactions", "{\"data\":{\"Transactions\":[{\"time\":\"2024-06-02T20:28:21.780Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":339.7,\"merchantName\":\"Kuhlman, Luettgen and Hahn\",\"category\":\"Education\"},{\"time\":\"2024-06-02T20:10:48.319Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":316.26,\"merchantName\":\"Kuphal Inc\",\"category\":\"Restaurants & Dining\"},{\"time\":\"2024-06-02T18:12:56.884Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":220.5,\"merchantName\":\"Kuphal Inc\",\"category\":\"Restaurants & Dining\"},{\"time\":\"2024-06-02T14:41:33.975Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":112.99,\"merchantName\":\"Paucek, Pfeffer and Nolan\",\"category\":\"Clothing & Apparel\"},{\"time\":\"2024-06-02T07:12:19.291Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":230.11,\"merchantName\":\"Sanford and Sons\",\"category\":\"Restaurants & Dining\"},{\"time\":\"2024-06-02T06:06:46.321Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":743.51,\"merchantName\":\"Kutch and Sons\",\"category\":\"Groceries\"},{\"time\":\"2024-06-02T05:11:50.048Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":225.64,\"merchantName\":\"Kulas Group\",\"category\":\"Communication\"},{\"time\":\"2024-06-02T02:36:19.692Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":221.17,\"merchantName\":\"Schaden and Sons\",\"category\":\"Communication\"},{\"time\":\"2024-06-02T00:42:13.873Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":2772.48,\"merchantName\":\"Howe and Sons\",\"category\":\"Housing & Utilities\"},{\"time\":\"2024-06-01T15:38:09.333Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":282.79,\"merchantName\":\"Considine Inc\",\"category\":\"Groceries\"},{\"time\":\"2024-06-01T09:59:44.147Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":46.85,\"merchantName\":\"McKenzie, Harris and Casper\",\"category\":\"Entertainment\"},{\"time\":\"2024-06-01T06:31:18.855Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":784.49,\"merchantName\":\"Batz-Boyle\",\"category\":\"Transportation\"},{\"time\":\"2024-06-01T04:28:40.675Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":376.71,\"merchantName\":\"Gibson and Sons\",\"category\":\"Transportation\"},{\"time\":\"2024-06-01T01:00:38.614Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":2620.14,\"merchantName\":\"Beahan-Fahey\",\"category\":\"Housing & Utilities\"},{\"time\":\"2024-05-31T22:24:55.690Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":132.95,\"merchantName\":\"Sanford and Sons\",\"category\":\"Restaurants & Dining\"},{\"time\":\"2024-05-31T15:40:22.865Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":234.38,\"merchantName\":\"Grant LLC\",\"category\":\"Clothing & Apparel\"},{\"time\":\"2024-05-31T15:08:34.895Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":442.09,\"merchantName\":\"Ebert, Okuneva and McKenzie\",\"category\":\"Travel & Vacations\"},{\"time\":\"2024-05-31T14:58:28.770Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":173.01,\"merchantName\":\"Stroman, Torp and Lockman\",\"category\":\"Restaurants & Dining\"},{\"time\":\"2024-05-31T13:08:36.590Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":73.9,\"merchantName\":\"Block, Huels and Windler\",\"category\":\"Groceries\"},{\"time\":\"2024-05-31T09:53:11.984Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":68.17,\"merchantName\":\"Hayes Group\",\"category\":\"Restaurants & Dining\"},{\"time\":\"2024-05-31T08:59:52.286Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":314.46,\"merchantName\":\"Balistreri-Schiller\",\"category\":\"Groceries\"},{\"time\":\"2024-05-31T08:13:40.897Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":461.71,\"merchantName\":\"Gleason-King\",\"category\":\"Education\"},{\"time\":\"2024-05-31T03:49:23.637Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":185.03,\"merchantName\":\"Grant and Sons\",\"category\":\"Clothing & Apparel\"},{\"time\":\"2024-05-31T03:15:49.200Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":853.18,\"merchantName\":\"Corwin, Erdman and Koelpin\",\"category\":\"Travel & Vacations\"},{\"time\":\"2024-05-30T19:06:53.015Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":320.83,\"merchantName\":\"Sanford and Sons\",\"category\":\"Restaurants & Dining\"},{\"time\":\"2024-05-30T17:20:29.777Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":394.74,\"merchantName\":\"Block, Huels and Windler\",\"category\":\"Groceries\"},{\"time\":\"2024-05-30T17:08:07.250Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":266.45,\"merchantName\":\"Ledner, Brown and Torp\",\"category\":\"Groceries\"},{\"time\":\"2024-05-30T14:30:13.135Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":111.69,\"merchantName\":\"Wolff Inc\",\"category\":\"Restaurants & Dining\"},{\"time\":\"2024-05-30T12:32:23.483Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":479.01,\"merchantName\":\"Lockman-Langosh\",\"category\":\"Travel & Vacations\"},{\"time\":\"2024-05-30T12:07:45.001Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":633.62,\"merchantName\":\"Gleason-King\",\"category\":\"Education\"},{\"time\":\"2024-05-30T04:48:59.258Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":2313.07,\"merchantName\":\"Beahan-Fahey\",\"category\":\"Housing & Utilities\"},{\"time\":\"2024-05-30T04:09:37.331Z\",\"cardNo\":\"4.680990142304E12\",\"amount\":810.85,\"merchantName\":\"Schaden-Goldner\",\"category\":\"Housing & Utilities\"}]}}");
    creditCardResults.put("SpendingByCategory", "{\"data\":{\"SpendingByCategory\":[{\"timeWeek\":\"2024-05-29T23:59:59.999Z\",\"category\":\"Childcare\",\"spending\":6990.1},{\"timeWeek\":\"2024-05-29T23:59:59.999Z\",\"category\":\"Clothing & Apparel\",\"spending\":2118.11},{\"timeWeek\":\"2024-05-29T23:59:59.999Z\",\"category\":\"Communication\",\"spending\":119.07},{\"timeWeek\":\"2024-05-29T23:59:59.999Z\",\"category\":\"Education\",\"spending\":979.14},{\"timeWeek\":\"2024-05-29T23:59:59.999Z\",\"category\":\"Entertainment\",\"spending\":225.36},{\"timeWeek\":\"2024-05-29T23:59:59.999Z\",\"category\":\"Groceries\",\"spending\":1244.27},{\"timeWeek\":\"2024-05-29T23:59:59.999Z\",\"category\":\"Health & Wellness\",\"spending\":1678.88},{\"timeWeek\":\"2024-05-29T23:59:59.999Z\",\"category\":\"Housing & Utilities\",\"spending\":12226.2},{\"timeWeek\":\"2024-05-29T23:59:59.999Z\",\"category\":\"Miscellaneous\",\"spending\":3410.91},{\"timeWeek\":\"2024-05-29T23:59:59.999Z\",\"category\":\"Restaurants & Dining\",\"spending\":778.7},{\"timeWeek\":\"2024-05-29T23:59:59.999Z\",\"category\":\"Transportation\",\"spending\":480.91},{\"timeWeek\":\"2024-05-29T23:59:59.999Z\",\"category\":\"Travel & Vacations\",\"spending\":7485.72}]}}");
    creditCardResults.put("SpendingByDay", "{\"data\":{\"SpendingByDay\":[{\"timeDay\":\"2024-06-02T23:59:59.999Z\",\"spending\":5182.36},{\"timeDay\":\"2024-06-01T23:59:59.999Z\",\"spending\":4110.98},{\"timeDay\":\"2024-05-31T23:59:59.999Z\",\"spending\":2938.88},{\"timeDay\":\"2024-05-30T23:59:59.999Z\",\"spending\":5330.26},{\"timeDay\":\"2024-05-29T23:59:59.999Z\",\"spending\":11531.37},{\"timeDay\":\"2024-05-28T23:59:59.999Z\",\"spending\":7146.92}]}}");
    localResults.put("finance", creditCardResults);
  }

  @Override
  public void validate(APIQuery query) {
    ErrorHandling.checkNotNullOrEmpty(query.getQuery(), "`query` cannot be empty");
  }

  @Override
  public String executeQuery(APIQuery query, JsonNode arguments) {
    String functionName = extractFunctionName(query);
    if (functionName == null) {
      throw new IllegalArgumentException("Could not extract function name from query: " + query.getQuery());
    }
    log.info("Extracted function name: {}", functionName);
    if (!localResults.get(useCase).containsKey(functionName)) {
      throw new IllegalArgumentException("Unknown function: " + functionName + " in query: " + query.getQuery());
    } else {
      log.info("Executing query: {} with arguments: {}", query, arguments);
      return localResults.get(useCase).get(functionName);
    }
  }

  private String extractFunctionName(APIQuery query) {
    Pattern pattern = Pattern.compile("\\s*query\\s*([a-zA-Z]+)\\(");
    Matcher matcher = pattern.matcher(query.getQuery());
    if (matcher.find()) {
      return matcher.group(1).trim();
    } else {
      return null;
    }
  }

  @Override
  public CompletableFuture<String> executeQueryAsync(APIQuery query, JsonNode arguments) {
    return CompletableFuture.completedFuture("mock write");
  }
}
