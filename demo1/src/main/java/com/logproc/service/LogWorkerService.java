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

    /**
     * @Async tells Spring to run this method in a separate thread.
     * This is the "Middle Task" where the CPU-heavy parsing happens.
     */
    @Async("workerExecutor")
    public void processLine(String rawLine) {
        try {
            //System.out.println("!!! WORKER HEARTBEAT: Parsing line...");

            // 1. Get the strategy
            LogParser parser = parserFactory.getParser(rawLine);

            // 2. Parse the line into an entry
            LogEntry entry = parser.parse(rawLine);

            // 3. --- EDITION MADE HERE: Attach Thread Name ---
            // We use the Thread.currentThread().getName() to see which pool member is working
            LogEntry entryWithThread = LogEntry.builder()
                    .timestamp(entry.getTimestamp())
                    .level(entry.getLevel())
                    .message(entry.getMessage())
                    .processedBy(Thread.currentThread().getName()) // Capture "LogWorker-1", etc.
                    .build();

            // 4. Send the enriched entry to the output queue
            outputQueue.put(entryWithThread);

            metrics.incrementProcessed();
            //System.out.println("!!! WORKER SUCCESS: Sent to OutputQueue by " + Thread.currentThread().getName() + " !!!");
        } catch (Exception e) {
            System.err.println("WORKER FAILED: " + e.getMessage());
            metrics.incrementError();
        }
    }
}