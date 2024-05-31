package com.datasqrl.ai.models.bedrock;

import com.datasqrl.ai.backend.ChatSession;
import com.datasqrl.ai.backend.ContextWindow;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionValidation;
import com.datasqrl.ai.backend.GenericChatMessage;
import com.datasqrl.ai.backend.RuntimeFunctionDefinition;
import com.datasqrl.ai.models.ChatClientProvider;
import com.datasqrl.ai.models.ChatMessageEncoder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class BedrockChatProvider extends ChatClientProvider<BedrockChatMessage, BedrockFunctionCall> {

  private final BedrockChatModel model;
  private final BedrockRuntimeClient client;
  private final ChatMessageEncoder<BedrockChatMessage> encoder;
  private final String systemPrompt;

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

  public BedrockChatProvider(BedrockChatModel model, FunctionBackend backend, String systemPrompt) {
    super(backend, new BedrockModelBindings(model));
    this.model = model;
    this.systemPrompt = combineSystemPromptAndFunctions(systemPrompt);
    EnvironmentVariableCredentialsProvider credentialsProvider = EnvironmentVariableCredentialsProvider.create();
    this.client = BedrockRuntimeClient.builder()
        .region(Region.US_WEST_2)
        .credentialsProvider(credentialsProvider)
        .build();
    this.encoder = switch (model) {
      case LLAMA3_70B, LLAMA3_8B -> new Llama3MessageEncoder();
    };
  }

  @Override
  public GenericChatMessage chat(String message, Map<String, Object> context) {
    ChatSession<BedrockChatMessage,BedrockFunctionCall> session = new ChatSession<>(backend, context, systemPrompt, bindings);
    BedrockChatMessage chatMessage = new BedrockChatMessage(BedrockChatRole.USER, message, "");
    session.addMessage(chatMessage);

    while (true) {
      ContextWindow<BedrockChatMessage> contextWindow = session.getContextWindow();
      String prompt = contextWindow.getMessages().stream()
          .map(this.encoder::encodeMessage)
          .collect(Collectors.joining("\n"));
      log.debug("Calling Bedrock with model {}", model.getModelName());
      JSONObject responseAsJson = promptBedrock(client, model.getModelName(), prompt, model.getCompletionLength());
      BedrockChatMessage responseMessage = encoder.decodeMessage(responseAsJson.get("generation").toString(), BedrockChatRole.ASSISTANT.getRole());
      GenericChatMessage genericResponse = session.addMessage(responseMessage);
      BedrockFunctionCall functionCall = responseMessage.getFunctionCall();
      if (functionCall != null) {
        FunctionValidation<BedrockChatMessage> fctValid = session.validateFunctionCall(functionCall);
        if (fctValid.isValid()) {
          if (fctValid.isPassthrough()) { //return as is - evaluated on frontend
            return genericResponse;
          } else {
            log.debug("Executing {} with arguments {}", functionCall.getFunctionName(),
                functionCall.getArguments().toPrettyString());
            BedrockChatMessage functionResponse = session.executeFunctionCall(functionCall, context);
            log.debug("Executed {} with results: {}" ,functionCall.getFunctionName(),functionResponse.getTextContent());
            session.addMessage(functionResponse);
          }
        } //TODO: add retry in case of invalid function call
      } else {
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

  private JSONObject promptBedrock(BedrockRuntimeClient client, String modelId, String prompt, int maxTokens) {
    JSONObject request = new JSONObject()
        .put("prompt", prompt)
        .put("max_gen_len", maxTokens)
        .put("temperature", 0F);
    InvokeModelRequest invokeModelRequest = InvokeModelRequest.builder()
        .modelId(modelId)
        .body(SdkBytes.fromUtf8String(request.toString()))
        .build();
    InvokeModelResponse invokeModelResponse = client.invokeModel(invokeModelRequest);
    JSONObject jsonObject = new JSONObject(invokeModelResponse.body().asUtf8String());
    log.debug("🤖Bedrock Response: {}", jsonObject);
    return jsonObject;
  }
}
