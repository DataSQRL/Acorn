package com.datasqrl.ai.models.vertex;

import com.datasqrl.ai.models.AbstractModelConfiguration;
import com.datasqrl.ai.util.ErrorHandling;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;

import java.util.Optional;

@Slf4j
@Value
public class VertexModelConfiguration extends AbstractModelConfiguration {

  public static final VertexModelType DEFAULT_MODEL = VertexModelType.GEMINI_1_5_FLASH;

  VertexModelType modelType;
  public static final String PROJECT_ID_KEY = "project_id";
  public static final String LOCATION_KEY = "location";
  public static final String TOP_K_KEY = "top_k";
  public static final int TOP_K_DEFAULT = 40;
  public static final int MAX_OUTPUT_TOKENS = 8192;

  public VertexModelConfiguration(Configuration configuration) {
    super(configuration);
    Optional<VertexModelType> modelType = VertexModelType.fromName(super.getModelName());
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
  public int getMaxOutputTokens() {
    if (configuration.containsKey(AbstractModelConfiguration.MAX_OUTPUT_TOKENS_KEY))
      return configuration.getInt(AbstractModelConfiguration.MAX_OUTPUT_TOKENS_KEY);
    return MAX_OUTPUT_TOKENS;
  }

  @Override
  public String getTokenizerName() {
    return configuration.getString(AbstractModelConfiguration.TOKENIZER_KEY, modelType.getModelName());
  }

  public String getProjectId() {
    ErrorHandling.checkArgument(configuration.containsKey(PROJECT_ID_KEY), "Need to configure vertex %s", MODEL_NAME_KEY);
    return configuration.getString(PROJECT_ID_KEY);
  }

  public String getLocation() {
    ErrorHandling.checkArgument(configuration.containsKey(LOCATION_KEY), "Need to configure vertex %s", LOCATION_KEY);
    return configuration.getString(LOCATION_KEY);
  }

  public int getTopK() {
    return configuration.getInt(TOP_K_KEY, TOP_K_DEFAULT);
  }

}
