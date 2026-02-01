package com.logproc.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logproc.model.InputMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

@Service
public class LogReaderService {

    private static final Logger logger = LoggerFactory.getLogger(LogReaderService.class);

    private final BlockingQueue<InputMessage> inputQueue;

    public LogReaderService(BlockingQueue<InputMessage> inputQueue) {
        this.inputQueue = inputQueue;
    }

    /**
     * Robust reader using Jackson streaming parser.
     * Handles concatenated JSON objects, multi-line objects, and embedded escaped characters.
     */
    public void readLogFile(String filePath) {
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();
        int count = 0;

        Path path = Path.of(filePath);
        try (InputStream in = Files.newInputStream(path);
             JsonParser parser = factory.createParser(in)) {

            while (parser.nextToken() != null) {
                try {
                    JsonNode node = mapper.readTree(parser);
                    if (node != null) {
                        String json = mapper.writeValueAsString(node);
                        inputQueue.put(InputMessage.of(json));

                        count++;
                        if (count % 500 == 0) {
                            logger.info("PRODUCER PROGRESS: {} records processed", count);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to parse a JSON element, skipping", e);
                    // continue to next token
                }
            }

            // Signal POISON
            inputQueue.put(InputMessage.POISON_PILL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Reader interrupted, stopping: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("READER ERROR: {}", e.getMessage(), e);
        }
    }
}