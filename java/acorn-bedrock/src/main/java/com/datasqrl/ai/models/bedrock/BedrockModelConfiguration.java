package com.datasqrl.ai.models.bedrock;

import com.datasqrl.ai.models.AbstractModelConfiguration;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;

import java.util.Optional;

@Slf4j
@Value
public class BedrockModelConfiguration extends AbstractModelConfiguration {

  public static final BedrockModelType DEFAULT_MODEL = BedrockModelType.LLAMA3_8B;

  BedrockModelType modelType;
  public static final String REGION_KEY = "region";
  public static final String REGION_DEFAULT = "us-east-1";
  public static final String MAX_GENERATION_LENGTH_KEY = "max_gen_len";

  public BedrockModelConfiguration(Configuration configuration) {
    super(configuration);
    Optional<BedrockModelType> modelType = BedrockModelType.fromName(super.getModelName());
    if (modelType.isEmpty()) {
      log.warn("Unrecognized model name: {}. Using [{}] model for token sizing.", super.getModelName(), DEFAULT_MODEL.getModelName());
      this.modelType = DEFAULT_MODEL;
    } else {
      this.modelType = modelType.get();
    }
  }

  @Override
  protected int getMaxTokensForModel() {
    return modelType.getContextWindowLength();
  }

  @Override
  public String getTokenizerName() {
    return configuration.getString(AbstractModelConfiguration.TOKENIZER_KEY, modelType.getTokenizerName());
  }

  @Override
  public Integer getMaxOutputTokens() {
    if (configuration.containsKey(MAX_GENERATION_LENGTH_KEY)) return configuration.getInt(MAX_GENERATION_LENGTH_KEY);
    return super.getMaxOutputTokens();
  }

  public String getRegion() {
    return configuration.getString(REGION_KEY, REGION_DEFAULT);
  }


}
