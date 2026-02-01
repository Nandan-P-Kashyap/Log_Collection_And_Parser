package com.logproc.service;

import com.logproc.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class LogWriterService {

    private static final Logger logger = LoggerFactory.getLogger(LogWriterService.class);

    private final BlockingQueue<LogEntry> outputQueue;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public LogWriterService(BlockingQueue<LogEntry> outputQueue) {
        this.outputQueue = outputQueue;
    }

    public void startWriting() {
        if (isRunning.getAndSet(true)) {
            return;
        }

        // 1. LOCK THE FILE PATH
        File outputFile = new File("processed_logs.txt");
        logger.info("WRITER STARTED. Saving to path: {}", outputFile.getAbsolutePath());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false))) {
            List<LogEntry> buffer = new ArrayList<>(500);

            while (true) {
                LogEntry first = outputQueue.take();

                if (first == LogEntry.POISON_PILL) {
                    logger.info("WRITER RECEIVED POISON_PILL. Flushing buffer...");
                    for (LogEntry entry : buffer) {
                        writer.write(formatLog(entry));
                    }
                    writer.flush();
                    logger.info("WRITER FINISHED.");
                    break;
                }

                buffer.add(first);
                outputQueue.drainTo(buffer, 499);

                for (LogEntry entry : buffer) {
                    writer.write(formatLog(entry));
                }
                writer.flush();
                buffer.clear();
            }
        } catch (Exception e) {
            logger.error("Writer failed: {}", e.getMessage(), e);
        }

        // 2. PROOF OF LIFE CHECK (Run this AFTER file is closed)
        if (outputFile.exists()) {
            logger.info("VERIFICATION: File exists at {}", outputFile.getAbsolutePath());
            logger.info("FINAL SIZE: {} bytes", outputFile.length());
            logger.info("LINE COUNT CHECK: {}", outputFile.length() > 0 ? "SUCCESS" : "FAILURE");
        } else {
            logger.error("ERROR: File was NOT created!");
        }
    }

    private String formatLog(LogEntry entry) {
        return String.format("[%s] [Thread: %s] %s: %s%n",
                entry.getTimestamp(),
                entry.getProcessedBy() != null ? entry.getProcessedBy() : "Unknown",
                entry.getLevel(),
                entry.getMessage());
    }
}