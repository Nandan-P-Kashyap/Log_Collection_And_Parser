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
        // Note: We use a try-with-resources for the reader
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
                    if (logBuffer.length() > 1024 * 1024) {
                        logBuffer.setLength(0);
                        balance = 0;
                        continue;
                    }
                }

                if (ch == '}') balance--;

                if (balance == 0 && logBuffer.length() > 0) {
                    String line = logBuffer.toString().trim();

                    // 🚀 INDUSTRY FIX: No more while-sleep loops.
                    // .put() will BLOCK this thread automatically if the queue is full.
                    // This is 100% efficient backpressure.
                    inputQueue.put(line);

                    count++;
                    if (count % 500 == 0) {
                        System.out.println("!!! PRODUCER PROGRESS: " + count + " lines processed !!!");
                    }
                    logBuffer.setLength(0);
                }
            }
            // Signal EOF
            inputQueue.put(EOF);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Proper handling for interrupted threads
        } catch (Exception e) {
            System.err.println("READER ERROR: " + e.getMessage());
        }
    }
}