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
    Optional<ModelType> modelType = ModelType.fromName(super.getModelName());
    if (modelType.isEmpty()) {
      log.warn("Unrecognized model name: {}. Using [{}] model for token sizing.", super.getModelName(), DEFAULT_MODEL.getName());
      this.modelType = DEFAULT_MODEL;
    } else {
      this.modelType = modelType.get();
    }
  }

  public static OpenAIModelConfiguration forModel(ModelType modelType) {
    return new OpenAIModelConfiguration(getConfigurationFor(modelType.getName()));
  }

  @Override
  protected int getMaxTokensForModel() {
    return modelType.getMaxContextLength();
  }


}
