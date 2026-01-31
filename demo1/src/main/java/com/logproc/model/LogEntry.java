package com.logproc.model;

import java.util.Map;
import java.util.Collections;

public class LogEntry {
    private final String timestamp;
    private final String level;
    private final String source;
    private final String message;
    private final String processedBy;
    private final Map<String, String> metadata;

    private LogEntry(Builder builder) {
        this.timestamp = builder.timestamp;
        this.level = builder.level;
        this.source = builder.source;
        this.message = builder.message;
        this.processedBy = builder.processedBy;
        this.metadata = builder.metadata != null ?
                Collections.unmodifiableMap(builder.metadata) : null;
    }


    // Getters
    public String getTimestamp() { return timestamp; }
    public String getLevel() { return level; }
    public String getSource() { return source; }
    public String getMessage() { return message; }
    public String getProcessedBy() { return processedBy; }

    public static class Builder {
        private String timestamp;
        private String level;
        private String source;
        private String message;
        private Map<String, String> metadata;
        private String processedBy; // ADD THIS

        public Builder processedBy(String threadName) {
            this.processedBy = threadName;
            return this;
        }

        public Builder timestamp(String ts) { this.timestamp = ts; return this; }
        public Builder level(String lvl) { this.level = lvl; return this; }
        public Builder source(String src) { this.source = src; return this; }
        public Builder message(String msg) { this.message = msg; return this; }
        public Builder metadata(Map<String, String> meta) { this.metadata = meta; return this; }

        public LogEntry build() { return new LogEntry(this); }
    }

    public static Builder builder() { return new Builder(); }

    // A shared POISON_PILL instance used to signal shutdown between components
    public static final LogEntry POISON_PILL = new Builder()
            .message("__POISON_PILL__")
            .timestamp("END")
            .level("INFO")
            .processedBy("POISON")
            .build();
}