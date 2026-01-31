package com.logproc.strategy.impl;

import com.logproc.model.LogEntry;
import com.logproc.strategy.LogParser;
import org.springframework.stereotype.Component;

@Component
public class JsonParser implements LogParser {

    @Override
    public boolean canHandle(String rawLine) {
        return rawLine != null && rawLine.trim().startsWith("{");
    }

    @Override
    public LogEntry parse(String rawLine, String threadName) {
        LogEntry.Builder builder = LogEntry.builder();

        // Use the helper to extract each field accurately
        builder.level(extractValue(rawLine, "\"level\":\""));
        builder.message(extractValue(rawLine, "\"msg\":\""));
        builder.timestamp(extractValue(rawLine, "\"timestamp\":\""));

        // 🛑 NEW: Set the thread name here
        builder.processedBy(threadName);

        return builder.build();
    }

    private String extractValue(String raw, String key) {
        int k = raw.indexOf(key);
        if (k == -1) return null;

        int startValue = k + key.length();
        int endValue = startValue;

        // Loop through the string to find the closing quote
        while (endValue < raw.length()) {
            endValue = raw.indexOf("\"", endValue);

            if (endValue == -1) break;

            // Check if the quote is escaped: \"
            if (endValue > 0 && raw.charAt(endValue - 1) != '\\') {
                return raw.substring(startValue, endValue);
            }
            endValue++;
        }
        return null;
    }
}