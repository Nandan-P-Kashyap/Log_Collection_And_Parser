package com.logproc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // This is vital for our multithreaded workers later
public class LogProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogProcessorApplication.class, args);
    }
}