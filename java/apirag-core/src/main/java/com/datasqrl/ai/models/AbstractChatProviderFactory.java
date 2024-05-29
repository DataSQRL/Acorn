package com.datasqrl.ai.models;

import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.util.ConfigurationUtil;
import com.datasqrl.ai.util.ErrorHandling;
import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.configuration2.Configuration;

public abstract class AbstractChatProviderFactory implements ChatProviderFactory {

  public static final String MODEL_KEY = "model";

  public <M extends Enum<M>> M getModel(Configuration modelConfiguration, Class<M> modelClass) {
    String modelName = modelConfiguration.getString(MODEL_KEY);
    ErrorHandling.checkArgument(modelName!=null && !modelName.isBlank(), "Need to configure a model name.");
    Optional<M> model = ConfigurationUtil.getEnumFromString(modelClass, modelName);
    ErrorHandling.checkArgument(model.isPresent(), "Unrecognized model: %s. Supported models are: %s", modelName, Arrays.toString(modelClass.getEnumConstants()));
    return model.get();
  }

}
