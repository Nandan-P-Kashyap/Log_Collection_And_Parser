package com.logproc.service;

import com.logproc.model.LogEntry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

@Service
public class LogWriterService {

    private final BlockingQueue<LogEntry> outputQueue;
    public static final String EOF = "EOF_SIGNAL"; // Match this to your Producer's EOF if you pass it through

    public LogWriterService(BlockingQueue<LogEntry> outputQueue) {
        this.outputQueue = outputQueue;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void startWriting() {
        // 'false' in FileWriter constructor overwrites the file each run (cleaner for testing)
        // Change to 'true' if you want to append.
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("processed_logs.txt", false))) {

            List<LogEntry> buffer = new ArrayList<>(500); // Temporary batch buffer

            while (true) {
                // 1. Get the first item (Blocks if empty)
                LogEntry first = outputQueue.take();

                // 2. Poison Pill Check (Optional: If you propagate EOF)
                // if (first.getMessage().equals(EOF)) break;

                buffer.add(first);

                // 3. DRAIN the rest! (The Magic Line)
                // Grab up to 499 MORE items instantly without waiting
                outputQueue.drainTo(buffer, 499);

                // 4. Write the whole batch at once
                for (LogEntry entry : buffer) {
                    // Manual string concatenation is faster than String.format for loops
                    writer.write("[");
                    writer.write(entry.getTimestamp());
                    writer.write("] [Thread: ");
                    writer.write(entry.getProcessedBy() != null ? entry.getProcessedBy() : "Unknown");
                    writer.write("] ");
                    writer.write(entry.getLevel());
                    writer.write(": ");
                    writer.write(entry.getMessage());
                    writer.newLine();
                }

                // 5. Clear buffer for next batch
                buffer.clear();

                // DO NOT FLUSH HERE! Let BufferedWriter decide when to flush (usually 8kb).
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}