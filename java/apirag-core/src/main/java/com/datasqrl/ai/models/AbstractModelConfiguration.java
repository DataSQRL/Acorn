package com.datasqrl.ai.models;

import com.datasqrl.ai.util.ErrorHandling;
import lombok.AllArgsConstructor;
import org.apache.commons.configuration2.Configuration;

@AllArgsConstructor
public abstract class AbstractModelConfiguration implements ModelConfiguration {

  public static final String MODEL_NAME_KEY = "name";
  public static final String MAX_INPUT_TOKENS_KEY = "max_input_tokens";
  public static final String MAX_OUTPUT_TOKENS_KEY = "max_output_tokens";
  public static final String TEMPERATURE_KEY = "temperature";
  public static final double TEMPERATURE_DEFAULT = 0.5;
  public static final String TOP_P_KEY = "top_p";
  public static final double TOP_P_DEFAULT = 0.9;
  public static final String TOKENIZER_KEY = "tokenizer";

  public static final double OUTPUT_TOKEN_RATIO = 0.3;

  protected final Configuration configuration;

  public String getModelName() {
    ErrorHandling.checkArgument(configuration.containsKey(MODEL_NAME_KEY), "Need to configure model %s", MODEL_NAME_KEY);
    return configuration.getString(MODEL_NAME_KEY);
  }

  @Override
  public double getTemperature() {
    return configuration.getDouble(TEMPERATURE_KEY, TEMPERATURE_DEFAULT);
  }

  @Override
  public double getTopP() {
    return configuration.getDouble(TOP_P_KEY, TOP_P_DEFAULT);
  }

  protected abstract int getMaxTokensForModel();

  @Override
  public int getMaxInputTokens() {
    if (configuration.containsKey(AbstractModelConfiguration.MAX_INPUT_TOKENS_KEY)) {
      return configuration.getInt(AbstractModelConfiguration.MAX_INPUT_TOKENS_KEY);
    } else {
      return (int)Math.round(getMaxTokensForModel()*(1-OUTPUT_TOKEN_RATIO));
    }
  }

  @Override
  public int getMaxOutputTokens() {
    if (configuration.containsKey(AbstractModelConfiguration.MAX_OUTPUT_TOKENS_KEY)) {
      return configuration.getInt(AbstractModelConfiguration.MAX_OUTPUT_TOKENS_KEY);
    } else {
      return (int)Math.round(getMaxTokensForModel()*(OUTPUT_TOKEN_RATIO));
    }
  }

}
