package com.logproc.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

@Component
public class LogOrchestrator implements CommandLineRunner {

    private final LogReaderService readerService;
    private final LogWorkerService workerService;
    private final LogWriterService writerService;
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
    public void run(String... args) throws Exception {
        System.out.println("🚀 STARTING VORTEX ENGINE (DEDICATED THREAD MODE)...");

        // 0. SAFETY CHECK: Does the file exist?
        File f = new File("logs.jsonl");
        if (!f.exists()) {
            System.err.println("❌ CRITICAL ERROR: 'logs.jsonl' NOT FOUND in: " + f.getAbsolutePath());
            System.exit(1); // Stop immediately if file is missing
        }
        System.out.println("📂 File found: " + f.getAbsolutePath());

        // We use a Latch to keep the main thread alive until the Writer finishes
        CountDownLatch shutdownLatch = new CountDownLatch(1);

        // 1. START WRITER (Dedicated Thread)
        Thread writerThread = new Thread(() -> {
            writerService.startWriting();
            shutdownLatch.countDown(); // Unblock main thread when Writer finishes
        }, "Orchestrator-Writer");
        writerThread.start();
        Thread.sleep(500); // Wait 500ms to ensure Writer is ready

        // 2. START CONSUMER (Dedicated Thread)
        Thread consumerThread = new Thread(() -> {
            try {
                System.out.println("⚙️ WORKER DISTRIBUTION STARTED...");
                while (true) {
                    String line = inputQueue.take();
                    workerService.processLine(line); // Pass to Async Worker

                    if (LogReaderService.EOF.equals(line)) {
                        System.out.println("🛑 ORCHESTRATOR: EOF received. Stopping distribution.");
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Orchestrator-Consumer");
        consumerThread.start();

        // 3. START READER (Dedicated Thread)
        Thread readerThread = new Thread(() -> {
            System.out.println("📖 READER STARTED...");
            readerService.readLogFile("logs.jsonl");
        }, "Orchestrator-Reader");
        readerThread.start();

        // 4. KEEP ALIVE
        // This prevents the application from exiting immediately.
        // It waits here until the Writer thread signals it is done.
        shutdownLatch.await();
        System.out.println("🏁 ENGINE SHUTDOWN COMPLETE.");
    }
}