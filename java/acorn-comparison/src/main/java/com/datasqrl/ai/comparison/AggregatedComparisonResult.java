package com.datasqrl.ai.comparison;

public record AggregatedComparisonResult(
    int runs,
    int noMessages,
    int noFunctionCalls,
    double avgCorrect,
    double avgCorrectMessages,
    double avgCorrectFunctionCalls,
    double avgJudgeScore
) {
}
