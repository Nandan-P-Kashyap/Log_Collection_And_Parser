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
            // 🛑 FIX: Check for EOF before parsing!
            if (rawLine.equals(LogReaderService.EOF)) {
                // Create a special "Poison Pill" entry to warn the Writer
                LogEntry eofEntry = LogEntry.builder()
                        .message(LogReaderService.EOF) // "EOF_SIGNAL"
                        .timestamp("END")
                        .level("INFO")
                        .build();
                outputQueue.put(eofEntry);
                return; // Stop processing this line
            }

            // Normal processing for real log lines...
            LogParser parser = parserFactory.getParser(rawLine);
            LogEntry entry = parser.parse(rawLine);
            outputQueue.put(entry);

            metrics.incrementProcessed();
        } catch (Exception e) {
            System.err.println("WORKER FAILED: " + e.getMessage());
            metrics.incrementError();
        }
    }
}