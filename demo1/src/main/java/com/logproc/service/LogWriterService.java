package com.logproc.service;

import com.logproc.model.LogEntry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.concurrent.BlockingQueue;

@Service
public class LogWriterService {

    private final BlockingQueue<LogEntry> outputQueue;

    public LogWriterService(BlockingQueue<LogEntry> outputQueue) {
        this.outputQueue = outputQueue;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void startWriting() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("processed_logs.txt", true))) {
            while (true) {
                LogEntry entry = outputQueue.take();

                // --- EDITION MADE HERE ---
                // We add entry.getProcessedBy() to the format string
                // so we can actually see which thread did the work.
                String line = String.format("[%s] [Thread: %s] %s: %s\n",
                        entry.getTimestamp(),
                        entry.getProcessedBy(), // This pulls the name we captured in the Worker
                        entry.getLevel(),
                        entry.getMessage());
                // -------------------------

                writer.write(line);
                writer.flush(); // Keep this so it writes immediately for your test
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}