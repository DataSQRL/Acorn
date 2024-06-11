package com.datasqrl.ai.models.google;

import com.datasqrl.ai.backend.GenericChatMessage;
import com.datasqrl.ai.backend.GenericFunctionCall;
import com.datasqrl.ai.backend.ModelAnalyzer;
import com.datasqrl.ai.backend.ModelBindings;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.FunctionCall;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.protobuf.Struct;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class GoogleModelBindings implements ModelBindings<Content, FunctionCall> {

  public final GoogleChatModel model;
  public final GenerativeModel generativeModel;
  public final String projectId;
  public final String projectLocation;

  public GoogleModelBindings(GoogleChatModel model, String vertexProjectId, String vertexProjectLocation) {
    this.model = model;
    this.projectId = vertexProjectId;
    this.projectLocation = vertexProjectLocation;
    VertexAI vertexAI = new VertexAI(vertexProjectId, vertexProjectLocation);
    this.generativeModel = new GenerativeModel(model.modelName, vertexAI);
  }

  @Override
  public Content convertMessage(GenericChatMessage message) {
    return switch (message.getRole()) {
      case "user", default -> ContentMaker.forRole("user").fromString(message.getContent());
      case "model" -> {
        Content.Builder msgBuilder = Content.newBuilder().setRole("model");
        Part.Builder partBuilder = Part.newBuilder().setText(message.getContent());
        GenericFunctionCall functionCall = message.getFunctionCall();
        if (functionCall != null) {
          FunctionCall.Builder fctCallBuilder = FunctionCall.newBuilder()
              .setName(functionCall.getName())
              .setArgs(Struct.newBuilder().putAllFields(ProtobufUtils.convertJsonNodeToValueMap(functionCall.getArguments())));
          partBuilder.setFunctionCall(fctCallBuilder);

        }
        yield msgBuilder.addParts(partBuilder).build();
      }
//      TODO: Revisit!
      case "system" -> ContentMaker.forRole("system").fromString(message.getContent());
    };
  }

  @Override
  public GenericChatMessage convertMessage(Content content, Map<String, Object> sessionContext) {
    Optional<FunctionCall> functionCall = content.getPartsList().stream().filter(Part::hasFunctionCall).map(Part::getFunctionCall).findFirst();
    try {
      return GenericChatMessage.builder()
          .role(content.getRole())
          .content(functionCall.map(this::functionCall2String).orElseGet(content::toString))
          .functionCall(functionCall.map(call -> new GenericFunctionCall(call.getName(), ProtobufUtils.convertMapToJsonNode(call.getArgs().getFieldsMap()))).orElse(null))
          .name("")
          .context(sessionContext)
          .timestamp(Instant.now().toString())
          .numTokens(generativeModel.countTokens(partsToString(content.getPartsList())).getTotalTokens())
          .build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isUserOrAssistantMessage(Content content) {
    return false;
  }

  @Override
  public ModelAnalyzer<Content> getTokenCounter() {
    return null;
  }

  @Override
  public int getMaxInputTokens() {
    return 0;
  }

  @Override
  public Content createSystemMessage(String systemMessage) {
    return null;
  }

  @Override
  public String getFunctionName(FunctionCall functionCall) {
    return "";
  }

  @Override
  public JsonNode getFunctionArguments(FunctionCall functionCall) {
    return null;
  }

  @Override
  public Content newFunctionResultMessage(String functionName, String functionResult) {
    return null;
  }

  @Override
  public Content convertExceptionToMessage(String s) {
    return null;
  }

  @Override
  public String getTextContent(Content content) {
    return "";
  }

  @Override
  public Content newUserMessage(String text) {
    return null;
  }

  private String functionCall2String(FunctionCall fctCall) {
    return "{"
        + "\"function\": \"" + fctCall.getName() + "\", "
        + "\"parameters\": " + fctCall.getArgs()
        + "}";
  }

  private String partsToString(List<Part> parts) {
    return parts.stream().map(Part::getText).collect(Collectors.joining("\n"));
  }

}
