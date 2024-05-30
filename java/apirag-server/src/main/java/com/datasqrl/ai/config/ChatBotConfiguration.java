package com.datasqrl.ai.config;

import com.datasqrl.ai.PlotFunction;
import com.datasqrl.ai.api.APIExecutor;
import com.datasqrl.ai.api.GraphQLExecutor;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionDefinition;
import com.datasqrl.ai.backend.FunctionType;
import com.datasqrl.ai.backend.RuntimeFunctionDefinition;
import com.datasqrl.ai.models.ChatProviderFactory;
import com.datasqrl.ai.util.ConfigurationUtil;
import com.datasqrl.ai.util.ErrorHandling;
import com.datasqrl.ai.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.function.Function;
import lombok.Value;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.JSONConfiguration;

@Value
public class ChatBotConfiguration {

  public static final String MODEL_PREFIX = "model";
  public static final String MODEL_PROVIDER_KEY = "provider";

  public static final String API_URL_KEY = "api-url";
  public static final String PLOT_FCT_KEY = "plot-function";
  public static final String PROMPT_KEY = "prompt";

  public static final String AUTH_FIELD_KEY = "auth-field";
  public static final String AUTH_FIELD_INTEGER_KEY = "auth-is-integer";

  Configuration baseConfiguration;
  Configuration modelConfiguration;
  String toolsDefinition;

  public FunctionBackend getFunctionBackend() {
    String graphQLEndpoint = baseConfiguration.getString(API_URL_KEY);
    APIExecutor apiExecutor = new GraphQLExecutor(graphQLEndpoint);
    FunctionBackend backend;
    try {
      backend = FunctionBackend.of(toolsDefinition, apiExecutor);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not parse tools definition", e);
    }
    PlotFunction plotFunction = getPlotFunction();
    if (plotFunction.isPresent()) {
      URL url = ChatBotConfiguration.class.getClassLoader().getResource(plotFunction.getResourceFile().get());
      ErrorHandling.checkArgument(url!=null, "Invalid url: %s", url);
      ObjectMapper objectMapper = new ObjectMapper();
      try {
        FunctionDefinition plotFunctionDef = objectMapper.readValue(url, FunctionDefinition.class);
        backend.addFunction(RuntimeFunctionDefinition.builder()
            .type(FunctionType.visualize)
            .function(plotFunctionDef)
            .context(List.of())
            .build());
      } catch (IOException e) {
        throw new IllegalArgumentException("Could not read plot function definition at: " + url, e);
      }
    }
    return backend;
  }

  public PlotFunction getPlotFunction() {
    return ConfigurationUtil.getEnumFromString(PlotFunction.class,baseConfiguration.getString(PLOT_FCT_KEY, PlotFunction.none.name()))
        .orElseThrow(() -> new IllegalArgumentException("Not a valid configuration value for ["+PLOT_FCT_KEY+"]. Expected one of: " + Arrays.toString(PlotFunction.values())));
  }

  public String getSystemPrompt() {
    String prompt = baseConfiguration.getString(PROMPT_KEY);
    ErrorHandling.checkArgument(prompt!=null, "Need to configure `["+PROMPT_KEY+"]` in configuration file.");
    ErrorHandling.checkArgument(!prompt.isBlank(), "`["+PROMPT_KEY+"]` cannot be empty.");
    return prompt;
  }

  public ChatProviderFactory getChatProviderFactory() {
    String provider = modelConfiguration.getString(MODEL_PROVIDER_KEY);
    ErrorHandling.checkArgument(provider!=null && !provider.isBlank(), "Need to configure `["+MODEL_PREFIX+"."+MODEL_PROVIDER_KEY+"]` in configuration file.");
    Optional<ChatProviderFactory> providerFact = ServiceLoader.load(ChatProviderFactory.class).stream()
        .map(Provider::get).filter(cpf -> cpf.getProviderName().equalsIgnoreCase(provider))
        .findFirst();
    ErrorHandling.checkArgument(providerFact.isPresent(), "Could not find model provider: " + provider);
    return providerFact.get();
  }

  public Function<String,Map<String,Object>> getContextFunction() {
    String fieldname = baseConfiguration.getString(AUTH_FIELD_KEY, "").trim();
    if (fieldname.isEmpty()) return s -> Map.of();
    boolean parseToInt = baseConfiguration.getBoolean(AUTH_FIELD_INTEGER_KEY, false);
    if (parseToInt) {
      return s -> Map.of(fieldname, Integer.parseInt(s));
    } else {
      return s -> Map.of(fieldname, s);
    }
  }

  public boolean hasAuth() {
    return baseConfiguration.containsKey(AUTH_FIELD_KEY);
  }

  public static ChatBotConfiguration fromFile(Path configPath, Path toolsPath) {
    ErrorHandling.checkArgument(Files.isRegularFile(configPath), "Cannot access configuration file: %s", configPath);
    ErrorHandling.checkArgument(Files.isRegularFile(toolsPath), "Cannot access tools file: %s", toolsPath);
    JSONConfiguration baseConfig = JsonUtil.getConfiguration(configPath);
    String tools;
    try {
      tools = Files.readString(toolsPath);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not read tools file: %s" + toolsPath, e);
    }
    return new ChatBotConfiguration(baseConfig, baseConfig.subset("model"), tools);
  }

}
