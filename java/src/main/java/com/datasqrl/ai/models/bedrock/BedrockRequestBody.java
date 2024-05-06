package com.datasqrl.ai.models.bedrock;

import java.util.Map;
import java.util.HashMap;

import org.json.JSONObject;

import software.amazon.awssdk.annotations.NotNull;

public class BedrockRequestBody {

  private BedrockRequestBody() {
  }

  public static BedrockRequestBodyBuilder builder() {
    return new BedrockRequestBodyBuilder();
  }

  public static class BedrockRequestBodyBuilder {

    @NotNull
    private String modelId;
    @NotNull
    private String prompt;
    private Map<String, Object> inferenceParameters;

    public BedrockRequestBodyBuilder withModelId(String modelId) {
      this.modelId = modelId;
      return this;
    }

    public BedrockRequestBodyBuilder withPrompt(String prompt) {
      this.prompt = prompt;
      return this;
    }

    public BedrockRequestBodyBuilder withInferenceParameter(String paramName, Object paramValue) {
      if (inferenceParameters == null) {
        inferenceParameters = new HashMap<>();
      }
      inferenceParameters.put(paramName, paramValue);
      return this;
    }

    public String build() {
      if (modelId == null) {
        throw new IllegalArgumentException("'modelId' is a required parameter");
      }
      if (prompt == null) {
        throw new IllegalArgumentException("'prompt' is a required parameter");
      }
      BedrockBodyCommand bedrockBodyCommand = null;
      switch (modelId) {
        case "amazon.titan-tg1-large":
        case "amazon.titan-text-express-v1":
          bedrockBodyCommand = new AmazonTitanCommand(prompt, inferenceParameters);
          break;
        case "ai21.j2-mid-v1":
        case "ai21.j2-ultra-v1":
          bedrockBodyCommand = new AI21LabsCommand(prompt, inferenceParameters);
          break;
        case "anthropic.claude-instant-v1":
        case "anthropic.claude-v1":
        case "anthropic.claude-v2":
          bedrockBodyCommand = new AnthropicCommand(prompt, inferenceParameters);
          break;
        case "cohere.command-text-v14":
          bedrockBodyCommand = new CohereCommand(prompt, inferenceParameters);
          break;
        case "stability.stable-diffusion-xl-v0":
          bedrockBodyCommand = new StabilityAICommand(prompt, inferenceParameters);
          break;
        case "meta.llama3-8b-instruct-v1:0":
          bedrockBodyCommand = new MetaAICommand(prompt, inferenceParameters);
          break;
      }
      return bedrockBodyCommand.execute();
    }
  }

  abstract class BedrockBodyCommand {

    protected String prompt;
    protected Map<String, Object> inferenceParameters;

    public BedrockBodyCommand(String prompt, Map<String, Object> inferenceParameters) {
      this.prompt = prompt;
      this.inferenceParameters = inferenceParameters;
    }

    protected void updateMap(Map<String, Object> existingMap, Map<String, Object> newEntries) {
      newEntries.forEach((newEntryKey, newEntryValue) -> {
        updateMap(existingMap, newEntryKey, newEntryValue);
      });
    }

    protected void updateMap(Map<String, Object> existingMap, String key, Object newValue) {
      if (existingMap.containsKey(key)) {
        existingMap.put(key, newValue);
      } else {
        existingMap.values().forEach(existingValue -> {
          if (existingValue instanceof Map) {
            @SuppressWarnings("unchecked")
            var valueAsMap = (Map<String, Object>) existingValue;
            updateMap(valueAsMap, key, newValue);
          }
        });
      }
    }

    public abstract String execute();

  }

  class AmazonTitanCommand extends BedrockBodyCommand {

    public AmazonTitanCommand(String prompt, Map<String, Object> inferenceParameters) {
      super(prompt, inferenceParameters);
    }

    @Override
    public String execute() {

      final Map<String, Object> textGenerationConfig = new HashMap<>(4);

      textGenerationConfig.put("maxTokenCount", 512);
      textGenerationConfig.put("stopSequences", new String[]{});
      textGenerationConfig.put("temperature", 0);
      textGenerationConfig.put("topP", 0.9f);

      final Map<String, Object> jsonMap = new HashMap<>(2);

      jsonMap.put("inputText", this.prompt);
      jsonMap.put("textGenerationConfig", textGenerationConfig);

      if (this.inferenceParameters != null && !this.inferenceParameters.isEmpty()) {
        updateMap(jsonMap, inferenceParameters);
      }
      return new JSONObject(jsonMap).toString();
    }

  }

  class AI21LabsCommand extends BedrockBodyCommand {

    public AI21LabsCommand(String prompt, Map<String, Object> inferenceParameters) {
      super(prompt, inferenceParameters);
    }

    @Override
    public String execute() {

      final Map<String, Object> countPenalty = Map.of("scale", 0);
      final Map<String, Object> presencePenalty = Map.of("scale", 0);
      final Map<String, Object> frequencyPenalty = Map.of("scale", 0);
      final Map<String, Object> jsonMap = new HashMap<>(8);

      jsonMap.put("prompt", this.prompt);
      jsonMap.put("maxTokens", 200);
      jsonMap.put("temperature", 0.7);
      jsonMap.put("topP", 1);
      jsonMap.put("stopSequences", new String[]{});
      jsonMap.put("countPenalty", countPenalty);
      jsonMap.put("presencePenalty", presencePenalty);
      jsonMap.put("frequencyPenalty", frequencyPenalty);

      if (this.inferenceParameters != null && !this.inferenceParameters.isEmpty()) {
        updateMap(jsonMap, inferenceParameters);
      }
      return new JSONObject(jsonMap).toString();
    }

  }

  class AnthropicCommand extends BedrockBodyCommand {

    public AnthropicCommand(String prompt, Map<String, Object> inferenceParameters) {
      super(prompt, inferenceParameters);
    }

    @Override
    public String execute() {

      final String promptTemplate = "Human: \n Human: ##PROMPT## \n nAssistant:";
      final String actualPrompt = promptTemplate.replace("##PROMPT##", this.prompt);

      Map<String, Object> jsonMap = new HashMap<>(7);

      jsonMap.put("prompt", actualPrompt);
      jsonMap.put("max_tokens_to_sample", 300);
      jsonMap.put("temperature", 1);
      jsonMap.put("top_k", 250);
      jsonMap.put("top_p", 0.999);
      jsonMap.put("stop_sequences", new String[]{});
      jsonMap.put("anthropic_version", "bedrock-2023-05-31");

      if (this.inferenceParameters != null && !this.inferenceParameters.isEmpty()) {
        updateMap(jsonMap, inferenceParameters);
      }
      return new JSONObject(jsonMap).toString();
    }

  }

  class CohereCommand extends BedrockBodyCommand {

    public CohereCommand(String prompt, Map<String, Object> inferenceParameters) {
      super(prompt, inferenceParameters);
    }

    @Override
    public String execute() {

      Map<String, Object> jsonMap = new HashMap<>(7);

      jsonMap.put("prompt", this.prompt);
      jsonMap.put("max_tokens", 400);
      jsonMap.put("temperature", 0.75);
      jsonMap.put("p", 0.01);
      jsonMap.put("k", 0);
      jsonMap.put("stop_sequences", new String[]{});
      jsonMap.put("return_likelihoods", "NONE");

      if (this.inferenceParameters != null && !this.inferenceParameters.isEmpty()) {
        updateMap(jsonMap, inferenceParameters);
      }
      return new JSONObject(jsonMap).toString();
    }

  }

  class StabilityAICommand extends BedrockBodyCommand {

    public StabilityAICommand(String prompt, Map<String, Object> inferenceParameters) {
      super(prompt, inferenceParameters);
    }

    @Override
    public String execute() {

      Map<String, Object> jsonMap = new HashMap<>(4);

      jsonMap.put("text_prompts", new Map[]{
          Map.of("text", this.prompt)
      });
      jsonMap.put("cfg_scale", 10);
      jsonMap.put("seed", 0);
      jsonMap.put("steps", 50);

      if (this.inferenceParameters != null && !this.inferenceParameters.isEmpty()) {
        updateMap(jsonMap, inferenceParameters);
      }
      return new JSONObject(jsonMap).toString();
    }
  }

  private class MetaAICommand extends BedrockBodyCommand {
    public MetaAICommand(String prompt, Map<String, Object> inferenceParameters) {
      super(prompt, inferenceParameters);
    }

    @Override
    public String execute() {

      Map<String, Object> jsonMap = new HashMap<>(7);

      jsonMap.put("prompt", this.prompt);
      jsonMap.put("max_gen_len", 512);
      jsonMap.put("temperature", 0.75);
      jsonMap.put("top_p", 0.2);

      if (this.inferenceParameters != null && !this.inferenceParameters.isEmpty()) {
        updateMap(jsonMap, inferenceParameters);
      }
      return new JSONObject(jsonMap).toString();
    }
  }
}


