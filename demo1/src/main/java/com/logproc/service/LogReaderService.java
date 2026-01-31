package com.logproc.service;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.BlockingQueue;

@Service
public class LogReaderService {

    private final BlockingQueue<String> inputQueue;
    public static final String EOF = "EOF_SIGNAL";

    public LogReaderService(BlockingQueue<String> inputQueue) {
        this.inputQueue = inputQueue;
    }

    public void readLogFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder logBuffer = new StringBuilder();
            int balance = 0;
            int count = 0;
            int c;

            while ((c = reader.read()) != -1) {
                char ch = (char) c;
                if (ch == '{') balance++;

                if (balance > 0) {
                    logBuffer.append(ch);

                    // Guard for Malformed JSON (Already correct)
                    if (logBuffer.length() > 1024 * 1024) {
                        System.err.println("CRITICAL: Malformed JSON detected near line " + count + ". Buffer exceeded 1MB. Skipping...");
                        logBuffer.setLength(0);
                        balance = 0;
                        continue;
                    }
                }

                if (ch == '}') balance--;

                if (balance == 0 && logBuffer.length() > 0) {
                    String line = logBuffer.toString().trim();

                    // 🛑 NEW OPTIMIZATION: High-Water Mark Throttling 🛑
                    // Before we even try to add to the queue, check if it's too full.
                    // This forces the Reader to pause proactively, preventing the Memory Wall.
                    while (inputQueue.size() > 500) {
                        try {
                            Thread.sleep(2); // Sleep longer (2ms) to let Workers breathe
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    // Standard Backpressure (Existing logic)
                    while (!inputQueue.offer(line)) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    count++;

                    if (count % 500 == 0) {
                        System.out.println("!!! PRODUCER PROGRESS: " + count + " lines processed !!!");
                    }

                    logBuffer.setLength(0);
                }
            }
            inputQueue.put(EOF);
        } catch (Exception e) {
            System.err.println("READER ERROR: " + e.getMessage());
        }
    }
}