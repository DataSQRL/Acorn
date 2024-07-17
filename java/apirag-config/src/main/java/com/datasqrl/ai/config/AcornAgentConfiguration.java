package com.datasqrl.ai.config;

import com.datasqrl.ai.api.APIExecutor;
import com.datasqrl.ai.api.APIExecutorFactory;
import com.datasqrl.ai.api.GraphQLSchemaConverter;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.ToolsBackendFactory;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.function.UDFConverter;
import com.datasqrl.ai.function.UserDefinedFunction;
import com.datasqrl.ai.function.builtin.BuiltinFunctions;
import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.models.ChatProviderFactory;
import com.datasqrl.ai.util.ConfigurationUtil;
import com.datasqrl.ai.util.ErrorHandling;
import com.datasqrl.ai.util.JsonUtil;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.Value;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.JSONConfiguration;

@Value
public class AcornAgentConfiguration {

  public static final String MODEL_PREFIX = "model";

  public static final String API_KEY = "apis";
  public static final String PLOT_FCT_KEY = "plot-function";
  public static final String PROMPT_KEY = "prompt";
  public static final String LOCAL_FUNCTIONS_KEY = "local-functions";
  public static final String CONTEXT_KEY = "context";

  public static final String CONVERTER_PREFIX = "converter";


  public static final String AUTH_FIELD_KEY = "auth-field";
  public static final String AUTH_FIELD_INTEGER_KEY = "auth-is-integer";

  Configuration baseConfiguration;
  Configuration modelConfiguration;
  List<RuntimeFunctionDefinition> toolFunctions;

  private RuntimeFunctionDefinition loadLocalFunction(String functionClassName) {
    if (!functionClassName.contains(".")) {
      //Assume it's a builtin function
      functionClassName = BuiltinFunctions.PACKAGE_NAME + "." + functionClassName;
    }
    Class<?> functionClass = null;
    try {
      functionClass = Class.forName(functionClassName);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Could not locate user defined function: " +  functionClassName, e);
    }
    ErrorHandling.checkArgument(UserDefinedFunction.class.isAssignableFrom(functionClass), "Not a user defined function: %s", functionClassName);
    return UDFConverter.getRuntimeFunctionDefinition((Class<? extends UserDefinedFunction>)functionClass);
  }

  public ToolsBackend getFunctionBackend() {
    Map<String,APIExecutor> apiExecutors = APIExecutorFactory.getAPIExecutors(baseConfiguration.subset(API_KEY));
    ErrorHandling.checkArgument(!apiExecutors.isEmpty(), "Need to configure at least one API in the configuration file under field `%s`", API_KEY);
    ToolsBackend backend = ToolsBackendFactory.of(toolFunctions, apiExecutors);
    DataVisualizationFunction dataVisualizationFunction = getDataVizFunction();
    if (dataVisualizationFunction.isPresent()) {
      URL url = ConfigurationUtil.getResourceFile(dataVisualizationFunction.getResourceFile().get());
      UDFConverter.addClientFunction(backend, url);
    }
    //Add local functions
    baseConfiguration.getList(LOCAL_FUNCTIONS_KEY).stream().map(String.class::cast)
        .map(this::loadLocalFunction).forEach(backend::addFunction);
    //Add global context
    List<String> context = getContext();
    if (!context.isEmpty()) {
      backend.setGlobalContext(Set.copyOf(context));
    }
    return backend;
  }

  public DataVisualizationFunction getDataVizFunction() {
    return ConfigurationUtil.getEnumFromString(
            DataVisualizationFunction.class,baseConfiguration.getString(PLOT_FCT_KEY, DataVisualizationFunction.none.name()))
        .orElseThrow(() -> new IllegalArgumentException("Not a valid configuration value for ["+PLOT_FCT_KEY+"]. Expected one of: " + Arrays.toString(
            DataVisualizationFunction.values())));
  }

  public String getSystemPrompt() {
    String prompt = baseConfiguration.getString(PROMPT_KEY);
    ErrorHandling.checkArgument(prompt!=null, "Need to configure `[%s]` in configuration file.", PROMPT_KEY);
    ErrorHandling.checkArgument(!prompt.isBlank(), "`[%s]` cannot be empty.", PROMPT_KEY);
    return prompt;
  }

  public ChatProvider getChatProvider() {
    ToolsBackend backend = getFunctionBackend();
    String systemPrompt = getSystemPrompt();
    return ChatProviderFactory.fromConfiguration(modelConfiguration).create(getModelConfiguration(), backend, systemPrompt);
  }

  public List<String> getContext() {
    if (baseConfiguration.containsKey(CONTEXT_KEY)) {
      return baseConfiguration.getList(String.class, CONTEXT_KEY);
    } else {
      return List.of();
    }
  }

  public boolean hasAuth() {
    return baseConfiguration.containsKey(AUTH_FIELD_KEY);
  }

  public static AcornAgentConfiguration fromFile(Path configPath, Path toolsPath) throws IOException {
    ErrorHandling.checkArgument(Files.isRegularFile(configPath), "Cannot access configuration file: %s", configPath);
    ErrorHandling.checkArgument(Files.isRegularFile(toolsPath), "Cannot access tools file: %s", toolsPath);
    JSONConfiguration baseConfig = JsonUtil.getConfiguration(configPath);
    String toolsContent;
    toolsContent = Files.readString(toolsPath);
    String extension = ConfigurationUtil.getFileExtension(toolsPath);
    List<RuntimeFunctionDefinition> tools;
    if (extension.equalsIgnoreCase("graphql") || extension.equalsIgnoreCase("graphqls")) {
      GraphQLSchemaConverter converter = new GraphQLSchemaConverter(
          baseConfig.subset(CONVERTER_PREFIX),
          APIExecutorFactory.DEFAULT_NAME);
      tools = converter.convert(toolsContent);
    } else {
      tools = ToolsBackendFactory.readTools(toolsContent);
    }
    return new AcornAgentConfiguration(baseConfig, baseConfig.subset("model"), tools);
  }

}