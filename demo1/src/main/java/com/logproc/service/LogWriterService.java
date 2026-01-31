package com.logproc.service;

import com.logproc.model.LogEntry;
import org.springframework.stereotype.Service;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class LogWriterService {

    private final BlockingQueue<LogEntry> outputQueue;
    // 🛑 GUARD: Prevents the "Ghost Writer" from wiping your file
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public LogWriterService(BlockingQueue<LogEntry> outputQueue) {
        this.outputQueue = outputQueue;
    }

    public void startWriting() {
        // 1. CHECK GUARD
        if (isRunning.getAndSet(true)) {
            System.err.println("⚠️ WARNING: LogWriterService tried to start twice! Ignoring request.");
            return;
        }

        System.out.println("💾 WRITER THREAD STARTED...");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("processed_logs.txt", false))) {
            List<LogEntry> buffer = new ArrayList<>(500);

            while (true) {
                LogEntry first = outputQueue.take();

                if (LogReaderService.EOF.equals(first.getMessage())) {
                    System.out.println("📝 WRITER RECEIVED EOF. Flushing buffer...");
                    for (LogEntry entry : buffer) {
                        writer.write(formatLog(entry));
                    }
                    writer.flush();
                    System.out.println("✅ WRITER FINISHED. File closed.");
                    break;
                }

                buffer.add(first);
                outputQueue.drainTo(buffer, 499);

                for (LogEntry entry : buffer) {
                    writer.write(formatLog(entry));
                }
                writer.flush(); // Flush regularly so you can see results in real-time
                buffer.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatLog(LogEntry entry) {
        return "[" + entry.getTimestamp() + "] [Thread: " +
                (entry.getProcessedBy() != null ? entry.getProcessedBy() : "Unknown") +
                "] " + entry.getLevel() + ": " + entry.getMessage() + "\n";
    }
}