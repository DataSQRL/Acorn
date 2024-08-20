package com.datasqrl.ai.comparison;

public record AggregatedComparisonResult(
    int runs,
    int noResponses,
    int noFunctionCalls,
    double avgCorrect,
    double avgCorrectResponses,
    double avgCorrectFunctionCalls,
    double avgJudgeScore
) {
}
