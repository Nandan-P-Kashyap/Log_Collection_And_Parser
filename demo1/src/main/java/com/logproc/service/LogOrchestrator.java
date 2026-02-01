package com.logproc.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

@Component
public class LogOrchestrator implements CommandLineRunner {

    private final LogReaderService readerService;
    private final LogWorkerService workerService;
    private final LogWriterService writerService;
    private final BlockingQueue<com.logproc.model.InputMessage> inputQueue;
    private final Executor orchestratorExecutor;

    public LogOrchestrator(LogReaderService readerService,
                           LogWorkerService workerService,
                           LogWriterService writerService,
                           BlockingQueue<com.logproc.model.InputMessage> inputQueue,
                           Executor orchestratorExecutor) {
        this.readerService = readerService;
        this.workerService = workerService;
        this.writerService = writerService;
        this.inputQueue = inputQueue;
        this.orchestratorExecutor = orchestratorExecutor;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 STARTING VORTEX ENGINE (MANAGED MODE)...");

        File f = new File("logs.jsonl");
        if (!f.exists()) {
            System.err.println("ERROR: logs.jsonl not found. Exiting orchestrator run().");
            return;
        }

        // We use a latch to wait for the WRITER to finish its job
        CountDownLatch writerLatch = new CountDownLatch(1);


        // Submit the Writer using the Spring-managed orchestratorExecutor
        orchestratorExecutor.execute(() -> {
            try {
                writerService.startWriting();
            } finally {
                writerLatch.countDown();
            }
        });

        // Submit the Consumer (The Bridge)
        orchestratorExecutor.execute(() -> {
            try {
                System.out.println("⚙️ WORKER DISTRIBUTION STARTED...");
                while (true) {
                    com.logproc.model.InputMessage msg = inputQueue.take(); // Blocks until data is available

                    try {
                        workerService.processLine(msg);
                    } catch (Exception e) {
                        System.err.println("WORKER ERROR: " + e.getMessage());
                    }

                    if (msg.isPoison()) break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Submit the Reader
        orchestratorExecutor.execute(() -> readerService.readLogFile("logs.jsonl"));

        // 4. Graceful Wait
        writerLatch.await();

        System.out.println("🏁 ENGINE SHUTDOWN COMPLETE.");

        // Normal return to allow Spring to manage application lifecycle and threads
    }
}