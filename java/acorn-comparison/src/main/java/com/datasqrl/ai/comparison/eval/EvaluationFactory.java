package com.datasqrl.ai.comparison.eval;

import org.apache.commons.configuration2.Configuration;

public interface EvaluationFactory {

  String getType();

  Evaluation create(Configuration settings);

}
