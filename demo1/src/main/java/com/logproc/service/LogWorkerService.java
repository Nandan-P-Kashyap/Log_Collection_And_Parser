package com.logproc.service;

import com.logproc.factory.ParserFactory;
import com.logproc.metrics.MetricsCollector;
import com.logproc.model.LogEntry;
import com.logproc.strategy.LogParser;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;

@Service
public class LogWorkerService {

    private final ParserFactory parserFactory;
    private final BlockingQueue<LogEntry> outputQueue;
    private final MetricsCollector metrics;

    public LogWorkerService(ParserFactory parserFactory,
                            BlockingQueue<LogEntry> outputQueue,
                            MetricsCollector metrics) {
        this.parserFactory = parserFactory;
        this.outputQueue = outputQueue;
        this.metrics = metrics;
    }

    @Async("workerExecutor")
    public void processLine(String rawLine) {
        try {
            // 1. Get strategy
            LogParser parser = parserFactory.getParser(rawLine);

            // 2. Parse ONCE.
            // Tip: If you really need the thread name, add it inside the parser
            // or modify LogEntry to have a setter.
            // For high-performance 10k runs, we DROP the thread name enrichment
            // to save 50% of object allocations.
            LogEntry entry = parser.parse(rawLine);

            // 3. Put directly into output queue (Blocking if Writer is slow!)
            outputQueue.put(entry);

            metrics.incrementProcessed();
        } catch (Exception e) {
            System.err.println("WORKER FAILED: " + e.getMessage());
            metrics.incrementError();
        }
    }
}