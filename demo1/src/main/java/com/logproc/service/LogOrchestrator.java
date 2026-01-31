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

        File f = new File("logs.jsonl");
        if (!f.exists()) {
            System.err.println("❌ CRITICAL ERROR: 'logs.jsonl' NOT FOUND at: " + f.getAbsolutePath());
            System.exit(1);
        }

        CountDownLatch shutdownLatch = new CountDownLatch(1);

        // 1. START WRITER
        Thread writerThread = new Thread(() -> {
            writerService.startWriting();
            shutdownLatch.countDown();
        }, "Orchestrator-Writer");
        writerThread.start();
        Thread.sleep(500);

        // 2. START CONSUMER
        Thread consumerThread = new Thread(() -> {
            try {
                System.out.println("⚙️ WORKER DISTRIBUTION STARTED...");
                while (true) {
                    String line = inputQueue.take();

                    // 🛑 SAFETY NET: Catch errors here so the consumer doesn't die!
                    try {
                        workerService.processLine(line);
                    } catch (Throwable t) {
                        System.err.println("❌ CRITICAL WORKER ERROR: " + t.getMessage());
                        t.printStackTrace();
                    }

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

        // 3. START READER
        Thread readerThread = new Thread(() -> {
            System.out.println("📖 READER STARTED...");
            readerService.readLogFile("logs.jsonl");
        }, "Orchestrator-Reader");
        readerThread.start();

        // 4. WAIT FOR COMPLETION
        shutdownLatch.await();
        System.out.println("🏁 ENGINE SHUTDOWN COMPLETE. FORCING EXIT.");
        System.exit(0);
    }
}