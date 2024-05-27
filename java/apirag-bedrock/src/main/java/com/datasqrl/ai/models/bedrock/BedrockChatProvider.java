package com.datasqrl.ai.models.bedrock;

import com.datasqrl.ai.backend.ChatSessionComponents;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionValidation;
import com.datasqrl.ai.backend.RuntimeFunctionDefinition;
import com.datasqrl.ai.models.ChatMessageEncoder;
import com.datasqrl.ai.models.ChatClientProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BedrockChatProvider implements ChatClientProvider<BedrockChatMessage> {

  private final BedrockChatModel model;
  private final FunctionBackend backend;
  private final BedrockRuntimeClient client;
  private final ChatMessageEncoder<BedrockChatMessage> encoder;
  private final BedrockChatMessage systemPrompt;

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

  public BedrockChatProvider(BedrockChatModel model, String systemPrompt, FunctionBackend backend) {
    this.model = model;
    this.backend = backend;
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
  public List<BedrockChatMessage> getChatHistory(Map<String, Object> context) {
    BedrockChatSession session = new BedrockChatSession(model, systemPrompt, backend, context);
    List<BedrockChatMessage> messages = session.retrieveMessageHistory(50);
    return messages.stream().filter(m -> switch (m.getRole()) {
      case USER, ASSISTANT -> true;
      default -> false;
    }).toList();
  }

  @Override
  public BedrockChatMessage chat(String message, Map<String, Object> context) {
    BedrockChatSession session = new BedrockChatSession(model, systemPrompt, backend, context);
    int numMsg = session.retrieveMessageHistory(20).size();
    System.out.printf("Retrieved %d messages\n", numMsg);
    BedrockChatMessage chatMessage = new BedrockChatMessage(BedrockChatRole.USER, message, "");
    session.addMessage(chatMessage);

    while (true) {
      ChatSessionComponents<BedrockChatMessage> sessionComponents = session.getSessionComponents();
      String prompt = sessionComponents.getMessages().stream()
          .map(this.encoder::encodeMessage)
          .collect(Collectors.joining("\n"));
      System.out.println("Calling Bedrock with model " + model.getModelName());
      JSONObject responseAsJson = promptBedrock(client, model.getModelName(), prompt, model.getContextWindowLength());
      BedrockChatMessage responseMessage = (BedrockChatMessage) encoder.decodeMessage(responseAsJson.get("generation").toString(), BedrockChatRole.ASSISTANT.getRole());
      session.addMessage(responseMessage);
      BedrockFunctionCall functionCall = responseMessage.getFunctionCall();
      if (functionCall != null) {
        FunctionValidation<BedrockChatMessage> fctValid = session.validateFunctionCall(functionCall);
        if (fctValid.isValid()) {
          if (fctValid.isPassthrough()) { //return as is - evaluated on frontend
            return responseMessage;
          } else {
            System.out.println("Executing " + functionCall.getFunctionName() + " with arguments "
                + functionCall.getArguments().toPrettyString());
            BedrockChatMessage functionResponse = session.executeFunctionCall(functionCall);
            System.out.println("Executed " + functionCall.getFunctionName() + " with results: " + functionResponse.getTextContent());
            session.addMessage(functionResponse);
          }
        } //TODO: add retry in case of invalid function call
      } else {
        //The text answer
        return responseMessage;
      }
    }
  }

  private BedrockChatMessage combineSystemPromptAndFunctions(String systemPrompt) {
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
    return new BedrockChatMessage(BedrockChatRole.SYSTEM, systemPrompt + "\n" + functionText + "\n", "");
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
    System.out.println("🤖Bedrock Response:\n" + jsonObject);
    return jsonObject;
  }
}
