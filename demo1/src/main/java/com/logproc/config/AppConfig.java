package com.logproc.config;

import com.logproc.model.LogEntry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync // Crucial: This enables the @Async multithreading
public class AppConfig {

    // The "Input Belt": Raw JSON strings from the Reader
    @Bean
    public BlockingQueue<String> inputQueue() {
        return new LinkedBlockingQueue<>(2000); // Buffer size of 2000 lines
    }

    // The "Output Belt": Processed LogEntry objects for the Writer
    @Bean
    public BlockingQueue<LogEntry> outputQueue() {
        return new LinkedBlockingQueue<>(2000);
    }

    // The "Worker Pool": Managing our processing threads
    @Bean(name = "workerExecutor")
    public Executor workerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);      // Minimum threads
        executor.setMaxPoolSize(20);      // Max threads under heavy load
        executor.setQueueCapacity(1000);   // Waiting room for tasks
        executor.setThreadNamePrefix("LogWorker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
