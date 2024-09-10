package com.datasqrl.ai.trace;

public record EntryComparisonEvaluation(
    Trace.Entry reference,
    Trace.Entry given,
    TraceComparisonResult comparisonResult
) {
}
