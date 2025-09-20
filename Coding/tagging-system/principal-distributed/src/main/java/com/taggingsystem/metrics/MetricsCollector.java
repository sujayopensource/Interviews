package com.taggingsystem.metrics;

import com.taggingsystem.TagOperation;

public interface MetricsCollector {
    void recordTagOperation(TagOperation operation);
    void recordRead();
    void recordError(String operation, Exception error);
    String getMetrics();
    void reset();
}