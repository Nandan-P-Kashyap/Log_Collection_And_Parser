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
    public LogEntry parse(String rawLine) {
        LogEntry.Builder builder = LogEntry.builder();

        // Use the helper to extract each field accurately
        builder.level(extractValue(rawLine, "\"level\":\""));
        builder.message(extractValue(rawLine, "\"msg\":\""));
        builder.timestamp(extractValue(rawLine, "\"timestamp\":\""));

        return builder.build();
    }
    // ADD THIS BLOCK SPECIFICALLY BELOW THE PARSE METHOD
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
            // We use raw.charAt(endValue - 1) to look back at the previous character
            if (endValue > 0 && raw.charAt(endValue - 1) != '\\') {
                return raw.substring(startValue, endValue);
            }

            // If it was escaped, skip it and keep searching
            endValue++;
        }
        return null;
    }


}