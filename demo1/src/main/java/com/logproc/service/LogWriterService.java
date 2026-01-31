package com.logproc.service;

import com.logproc.model.LogEntry;
import org.springframework.stereotype.Service;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

@Service
public class LogWriterService {

    private final BlockingQueue<LogEntry> outputQueue;

    public LogWriterService(BlockingQueue<LogEntry> outputQueue) {
        this.outputQueue = outputQueue;
    }

    // REMOVED @Async and @EventListener
    public void startWriting() {
        System.out.println("💾 WRITER THREAD STARTED..."); // Debug Print
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("processed_logs.txt", false))) {
            List<LogEntry> buffer = new ArrayList<>(500);

            while (true) {
                LogEntry first = outputQueue.take();

                // Poison Pill Check
                if (LogReaderService.EOF.equals(first.getMessage())) {
                    System.out.println("📝 WRITER RECEIVED EOF. Flushing buffer...");
                    for (LogEntry entry : buffer) {
                        writer.write(formatLog(entry));
                    }
                    writer.flush(); // Force final write
                    System.out.println("✅ WRITER FINISHED. File closed.");
                    break;
                }

                buffer.add(first);
                outputQueue.drainTo(buffer, 499);

                for (LogEntry entry : buffer) {
                    writer.write(formatLog(entry));
                }

                // TEMP FIX: Flush every batch so you can SEE the file growing
                writer.flush();

                buffer.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper method to keep code clean
    private String formatLog(LogEntry entry) {
        return "[" + entry.getTimestamp() + "] [Thread: " +
                (entry.getProcessedBy() != null ? entry.getProcessedBy() : "Unknown") +
                "] " + entry.getLevel() + ": " + entry.getMessage() + "\n";
    }
}