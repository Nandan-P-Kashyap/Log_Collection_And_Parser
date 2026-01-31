package com.logproc.metrics;

import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.LongAdder;

@Service // Spring ensures this is a Singleton by default
public class MetricsCollector {

    private final LongAdder processedCount = new LongAdder();
    private final LongAdder errorCount = new LongAdder();
    private final long startTime = System.currentTimeMillis();

    public void incrementProcessed() {
        processedCount.increment();
    }

    public void incrementError() {
        errorCount.increment();
    }

    public long getProcessedCount() {
        return processedCount.sum();
    }

    public double getThroughput() {
        long durationSeconds = (System.currentTimeMillis() - startTime) / 1000;
        return durationSeconds > 0 ? (double) getProcessedCount() / durationSeconds : 0;
    }
}