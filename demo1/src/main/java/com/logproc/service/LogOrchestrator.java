package com.logproc.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;

@Service
public class LogOrchestrator {

    private final BlockingQueue<String> inputQueue;
    private final LogWorkerService workerService;

    public LogOrchestrator(BlockingQueue<String> inputQueue, LogWorkerService workerService) {
        this.inputQueue = inputQueue;
        this.workerService = workerService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void startEngine() {
        try {
            while (true) {
                String rawLine = inputQueue.take();
                //System.out.println("!!! ENGINE HEARTBEAT: Took line from InputQueue !!!");

                if (LogReaderService.EOF.equals(rawLine)) {
                    //System.out.println("!!! ENGINE: Received EOF !!!");
                    break;
                }
                workerService.processLine(rawLine);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}