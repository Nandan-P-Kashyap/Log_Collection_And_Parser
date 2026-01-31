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

                    // --- ADD THIS GUARD HERE ---
                    // If a single line exceeds 1MB, it means the JSON is malformed
                    // (missing a closing brace). We stop it here before it eats all your RAM.
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

                    // --- EDITION MADE HERE: Non-blocking backpressure ---
                    // Instead of forcing it in, we try to "offer" it to the queue.
                    // If the queue is full, we wait 1ms and try again.
                    // This prevents the Reader from flooding the JVM memory.
                    while (!inputQueue.offer(line)) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    // ----------------------------------------------------

                    count++;

                    // Log progress every 500 lines so you can see it moving in the console
                    if (count % 500 == 0) {
                        System.out.println("!!! PRODUCER PROGRESS: " + count + " lines processed !!!");
                    }

                    logBuffer.setLength(0);
                }
            }
            //System.out.println("!!! READER HEARTBEAT: Sent " + count + " lines to InputQueue !!!");
            inputQueue.put(EOF);
        } catch (Exception e) {
            System.err.println("READER ERROR: " + e.getMessage());
        }
    }
}