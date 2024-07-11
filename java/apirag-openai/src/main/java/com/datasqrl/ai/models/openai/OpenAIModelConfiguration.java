package com.datasqrl.ai.models.openai;

import com.datasqrl.ai.models.AbstractModelConfiguration;
import com.knuddels.jtokkit.api.ModelType;
import java.util.Optional;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;

@Slf4j
@Value
public class OpenAIModelConfiguration extends AbstractModelConfiguration {

  public static final ModelType DEFAULT_MODEL = ModelType.GPT_3_5_TURBO;

  ModelType modelType;

  public OpenAIModelConfiguration(Configuration configuration) {
    super(configuration);
    Optional<ModelType> modelType = ModelType.fromName(super.getConfiguredModelName());
    if (modelType.isEmpty()) {
      log.warn("Unrecognized model name: {}. Using [{}] model for token sizing.", super.getConfiguredModelName(), DEFAULT_MODEL.getName());
      this.modelType = DEFAULT_MODEL;
    } else {
      this.modelType = modelType.get();
    }
  }

  @Override
  protected int getMaxTokensForModel() {
    return modelType.getMaxContextLength();
  }


  @Override
  public String getModelName() {
    return modelType.getName();
  }
}
