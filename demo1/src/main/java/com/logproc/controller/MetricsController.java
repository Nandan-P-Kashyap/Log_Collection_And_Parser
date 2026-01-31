package com.logproc.controller;

import com.logproc.metrics.MetricsCollector;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class MetricsController {

    private final MetricsCollector metrics;

    // Spring injects the MetricsCollector singleton here
    public MetricsController(MetricsCollector metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/api/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("total_processed", metrics.getProcessedCount());
        stats.put("throughput_logs_per_sec", String.format("%.2f", metrics.getThroughput()));
        stats.put("status", "ACTIVE");

        return stats;
    }
}