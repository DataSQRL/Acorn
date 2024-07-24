package com.datasqrl.ai.models.bedrock;

import com.datasqrl.ai.models.ChatSession;
import com.datasqrl.ai.models.ContextWindow;
import com.datasqrl.ai.models.ModelAnalyzer;
import com.datasqrl.ai.tool.ModelObservability;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.models.ChatMessageEncoder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@Slf4j
public class BedrockChatProvider extends ChatProvider<BedrockChatMessage, BedrockFunctionCall> {

  private final BedrockModelConfiguration config;
  private final BedrockRuntimeClient client;
  private final ChatMessageEncoder<BedrockChatMessage> encoder;
  private final String systemPrompt;
  ModelObservability.Trace modeltrace;

  private final String FUNCTION_CALLING_PROMPT = "To call a function, respond only with JSON text in the following format: "
      + "{\"function\": \"$FUNCTION_NAME\","
      + "  \"parameters\": {"
      + "  \"$PARAMETER_NAME1\": \"$PARAMETER_VALUE1\","
      + "  \"$PARAMETER_NAME2\": \"$PARAMETER_VALUE2\","
      + "  ..."
      + "}. "
      + "You will get a function response from the user in the following format: "
      + "{\"function_response\" : \"$FUNCTION_NAME\","
      + "  \"data\": { \"$RESULT_TYPE\": [$RESULTS] }"
      + "}\n"
      + "Here are the functions you can use:";

  public BedrockChatProvider(BedrockModelConfiguration config, ToolsBackend backend, String systemPrompt, ModelObservability observability) {
    super(backend, new BedrockModelBindings(config), observability);
    this.config = config;
    this.systemPrompt = combineSystemPromptAndFunctions(systemPrompt);
    EnvironmentVariableCredentialsProvider credentialsProvider = EnvironmentVariableCredentialsProvider.create();
    this.client = BedrockRuntimeClient.builder()
        .region(Region.of(config.getRegion()))
        .credentialsProvider(credentialsProvider)
        .build();
    this.encoder = switch (config.getModelType()) {
      case LLAMA3_70B, LLAMA3_8B -> new Llama3MessageEncoder();
    };
  }

  @Override
  public GenericChatMessage chat(String message, Map<String, Object> context) {
    ModelAnalyzer<BedrockChatMessage> tokenCounter = bindings.getTokenCounter();
    ChatSession<BedrockChatMessage, BedrockFunctionCall> session = new ChatSession<>(backend, context, systemPrompt, bindings);
    BedrockChatMessage chatMessage = new BedrockChatMessage(BedrockChatRole.USER, message, "");
    session.addMessage(chatMessage);

    int retryCount = 0;
    while (true) {
      ContextWindow<BedrockChatMessage> contextWindow = session.getContextWindow();
      String prompt = contextWindow.getMessages().stream()
          .map(this.encoder::encodeMessage)
          .collect(Collectors.joining("\n"));
      log.info("Calling Bedrock with model {}", config.getModelName());
      JSONObject responseAsJson = promptBedrock(client, config.getModelName(), prompt);
      String generatedResponse = responseAsJson.get("generation").toString();
      BedrockChatMessage responseMessage = encoder.decodeMessage(generatedResponse, BedrockChatRole.ASSISTANT.getRole());
      GenericChatMessage genericResponse = session.addMessage(responseMessage);
      BedrockFunctionCall functionCall = responseMessage.getFunctionCall();
      if (functionCall != null) {
        ChatSession.FunctionExecutionOutcome<BedrockChatMessage> outcome = session.validateAndExecuteFunctionCall(functionCall, true);
        switch (outcome.status()) {
          case EXECUTE_ON_CLIENT -> {
            modeltrace.complete(tokenCounter.countTokens(prompt), tokenCounter.countTokens(generatedResponse), false);
            return genericResponse;
          }
          case VALIDATION_ERROR_RETRY -> {
            modeltrace.complete(tokenCounter.countTokens(prompt), tokenCounter.countTokens(generatedResponse), true);
            if (retryCount >= ChatProvider.FUNCTION_CALL_RETRIES_LIMIT) {
              throw new RuntimeException("Too many function call retries for the same function.");
            } else {
              retryCount++;
              log.debug("Failed function call: {}", functionCall);
              log.info("Function call failed. Retrying ...");
            }
          }
        }
      } else {
        modeltrace.complete(tokenCounter.countTokens(prompt), tokenCounter.countTokens(generatedResponse), false);
        //The text answer
        return genericResponse;
      }
    }
  }

  private String combineSystemPromptAndFunctions(String systemPrompt) {
    ObjectMapper objectMapper = new ObjectMapper();
//    Note: This approach does not take into account the context window for the system prompt
    String functionText = this.FUNCTION_CALLING_PROMPT
        + this.backend.getFunctions().values().stream()
        .map(RuntimeFunctionDefinition::getChatFunction)
        .map(f ->
            objectMapper.createObjectNode()
                .put("type", "function")
                .set("function", objectMapper.valueToTree(f))
        )
        .map(value -> {
          try {
            return objectMapper.writeValueAsString(value);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        })
        .collect(Collectors.joining("\n"));
    return systemPrompt + "\n" + functionText + "\n";
  }

  private JSONObject promptBedrock(BedrockRuntimeClient client, String modelId, String prompt) {
    JSONObject request = new JSONObject()
        .put("prompt", prompt)
        .put("max_gen_len", config.getMaxOutputTokens())
        .put("top_p", config.getTopP())
        .put("temperature", config.getTemperature());
    InvokeModelRequest invokeModelRequest = InvokeModelRequest.builder()
        .modelId(modelId)
        .body(SdkBytes.fromUtf8String(request.toString()))
        .build();
    log.debug("Bedrock prompt: {}", prompt);
    modeltrace = observability.start();
    InvokeModelResponse invokeModelResponse = client.invokeModel(invokeModelRequest);
    modeltrace.stop();
    JSONObject jsonObject = new JSONObject(invokeModelResponse.body().asUtf8String());
    log.debug("Bedrock Response: {}", jsonObject);
    return jsonObject;
  }
}
