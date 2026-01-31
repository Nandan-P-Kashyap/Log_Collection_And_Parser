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
            // 1. Check for EOF before parsing
            if (rawLine.equals(LogReaderService.EOF)) {
                // Place the shared POISON_PILL onto the output queue to signal writer shutdown
                outputQueue.put(LogEntry.POISON_PILL);
                return;
            }

            // 2. Normal processing: Get the specific Parser Strategy
            LogParser parser = parserFactory.getParser(rawLine);

            // 🛑 THE FIX: Capture the current thread name and pass it as the 2nd argument
            // This ensures k is an index at the modified string/object properly
            String currentThreadName = Thread.currentThread().getName();
            LogEntry entry = parser.parse(rawLine, currentThreadName);

            outputQueue.put(entry);

            metrics.incrementProcessed();
        } catch (Exception e) {
            System.err.println("WORKER FAILED: " + e.getMessage());
            e.printStackTrace(); // Added for better debugging
            metrics.incrementError();
        }
    }
}