package com.logproc.strategy.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.logproc.model.LogEntry;
import com.logproc.strategy.LogParser;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
public class JsonParser implements LogParser {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean canHandle(String rawLine) {
        return rawLine != null && rawLine.trim().startsWith("{");
    }

    @Override
    public LogEntry parse(String rawLine, String threadName) {
        LogEntry.Builder builder = LogEntry.builder();
        Map<String, String> metadata = new HashMap<>();

        try {
            JsonNode root = mapper.readTree(rawLine);

            // 1. Extract Standard Fields
            if (root.has("level"))
                builder.level(root.get("level").asText());
            if (root.has("timestamp"))
                builder.timestamp(root.get("timestamp").asText());
            if (root.has("msg"))
                builder.message(root.get("msg").asText());
            else if (root.has("message"))
                builder.message(root.get("message").asText());

            // 2. Process all fields for Metadata & Nested JSON
            flattenJson(root, "", metadata);

        } catch (Exception e) {
            // Fallback for malformed JSON
            builder.message("FAILED_TO_PARSE: " + rawLine);
            builder.level("ERROR");
        }

        builder.processedBy(threadName);
        builder.metadata(metadata);
        return builder.build();
    }

    private void flattenJson(JsonNode node, String prefix, Map<String, String> map) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                String newKey = prefix.isEmpty() ? key : prefix + "." + key;

                if (value.isObject()) {
                    flattenJson(value, newKey, map);
                } else if (value.isTextual()) {
                    String text = value.asText();
                    // Check if the string itself is a nested JSON object
                    if (text.trim().startsWith("{") && text.trim().endsWith("}")) {
                        try {
                            JsonNode nested = mapper.readTree(text);
                            flattenJson(nested, newKey, map);
                        } catch (Exception e) {
                            // Not valid JSON, treat as regular string
                            map.put(newKey, text);
                        }
                    } else {
                        map.put(newKey, text);
                    }
                } else {
                    // Numbers, Booleans, etc.
                    map.put(newKey, value.asText());
                }
            }
        } else if (node.isArray()) {
            // For arrays, just store the string representation for now
            map.put(prefix, node.toString());
        }
    }
}