package com.logproc.service;

import com.logproc.model.LogDocument;
import com.logproc.model.LogEntry;
import com.logproc.repository.LogElasticsearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class LogWriterService {

    private static final Logger logger = LoggerFactory.getLogger(LogWriterService.class);

    private final BlockingQueue<LogEntry> outputQueue;
    private final LogElasticsearchRepository repository;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public LogWriterService(BlockingQueue<LogEntry> outputQueue, LogElasticsearchRepository repository) {
        this.outputQueue = outputQueue;
        this.repository = repository;
    }

    public void startWriting() {
        if (isRunning.getAndSet(true)) {
            return;
        }

        logger.info("ELASTICSEARCH WRITER STARTED. Indexing logs to 'logs' index...");

        List<LogEntry> buffer = new ArrayList<>(500);

        try {
            while (true) {
                LogEntry first = outputQueue.take();

                if (first == LogEntry.POISON_PILL) {
                    logger.info("WRITER RECEIVED POISON_PILL. Flushing remaining buffer...");
                    flushToElasticsearch(buffer);
                    logger.info("WRITER FINISHED.");
                    break;
                }

                buffer.add(first);
                outputQueue.drainTo(buffer, 499);

                flushToElasticsearch(buffer);
                buffer.clear();
            }
        } catch (Exception e) {
            logger.error("Writer failed: {}", e.getMessage(), e);
        }
    }

    private void flushToElasticsearch(List<LogEntry> buffer) {
        if (buffer.isEmpty())
            return;

        try {
            // Filter out any accidental POISON_PILLs that might have been drained
            List<LogDocument> docs = buffer.stream()
                    .filter(e -> e != LogEntry.POISON_PILL)
                    .map(LogDocument::new)
                    .collect(Collectors.toList());

            if (docs.isEmpty())
                return;

            repository.saveAll(docs);
            logger.info("Indexed batch of {} logs", docs.size());
        } catch (Exception e) {
            logger.error("FAILED TO INDEX BATCH: {}", e.getMessage());
        }
    }
}