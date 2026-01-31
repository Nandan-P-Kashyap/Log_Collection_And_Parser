package com.logproc.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

@Component
public class LogOrchestrator implements CommandLineRunner {

    private final LogReaderService readerService;
    private final LogWorkerService workerService;
    private final LogWriterService writerService; // Inject Writer
    private final BlockingQueue<String> inputQueue;

    public LogOrchestrator(LogReaderService readerService,
                           LogWorkerService workerService,
                           LogWriterService writerService,
                           BlockingQueue<String> inputQueue) {
        this.readerService = readerService;
        this.workerService = workerService;
        this.writerService = writerService;
        this.inputQueue = inputQueue;
    }

    @Override
    public void run(String... args) {
        System.out.println("🚀 STARTING VORTEX ENGINE...");

        // 1. START WRITER (The Sink) - Must start first!
        CompletableFuture.runAsync(() -> writerService.startWriting());

        // 2. START CONSUMER (The Bridge)
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("⚙️ WORKER DISTRIBUTION STARTED...");
                while (true) {
                    String line = inputQueue.take();
                    workerService.processLine(line); // Async call to worker

                    if (LogReaderService.EOF.equals(line)) {
                        break; // Stop distributing
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 3. START READER (The Source) - Starts last
        CompletableFuture.runAsync(() -> {
            // DOUBLE CHECK: Ensure "logs.jsonl" is in your project root
            readerService.readLogFile("logs.jsonl");
        });
    }
}