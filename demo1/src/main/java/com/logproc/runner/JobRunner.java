package com.logproc.runner;

import com.logproc.service.LogReaderService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class JobRunner implements CommandLineRunner {

    private final LogReaderService readerService;

    public JobRunner(LogReaderService readerService) {
        this.readerService = readerService;
    }

    @Override
    public void run(String... args) throws Exception {
        // In a real app, this path could come from application.properties
        String path = "logs.jsonl";
        readerService.readLogFile(path);
    }
}