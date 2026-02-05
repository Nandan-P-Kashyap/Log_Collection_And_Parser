package com.logproc.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class LogGenerator {
    public static void main(String[] args) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("logs.jsonl"))) {
            for (int i = 1; i <= 100000; i++) { // Generate 100,000 complex logs
                String level = (i % 2 == 0) ? "ERROR" : "INFO";

                // Construct a nested JSON string for the message
                // This simulates "logs inside logs"
                String nestedJson = "{\\\"user\\\": \\\"admin" + i
                        + "\\\", \\\"action\\\": \\\"login\\\", \\\"details\\\": {\\\"ip\\\": \\\"192.168.1." + i
                        + "\\\"}}";

                String ts = "2026-01-25T21:00:" + (i < 10 ? "0" + i : i) + "Z";

                // Construct the JSON line
                String jsonLine = "{\"level\":\"" + level + "\",\"msg\":\"" + nestedJson + "\",\"timestamp\":\"" + ts
                        + "\", \"service\": \"auth-service\"}";

                writer.write(jsonLine);
                writer.newLine();
            }
        }
        org.slf4j.LoggerFactory.getLogger(LogGenerator.class).info("Generated COMPLEX logs.jsonl successfully.");
    }
}
