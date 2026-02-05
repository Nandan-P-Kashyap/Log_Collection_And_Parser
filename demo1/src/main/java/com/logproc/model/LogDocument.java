package com.logproc.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Map;

@Document(indexName = "logs")
public class LogDocument {

    @Id
    private String id;

    @Field(type = FieldType.Date)
    private String timestamp;

    @Field(type = FieldType.Keyword)
    private String level;

    @Field(type = FieldType.Text)
    private String message;

    @Field(type = FieldType.Keyword)
    private String processedBy;

    // Use Object type to store arbitrary map data or Keyword if flattening is
    // desired.
    // Specifying Enabled=false means we won't strictly index every key, or use
    // Flattened in newer versions.
    @Field(type = FieldType.Object)
    private Map<String, String> metadata;

    public LogDocument() {
    }

    public LogDocument(LogEntry entry) {
        this.timestamp = entry.getTimestamp();
        this.level = entry.getLevel();
        this.message = entry.getMessage();
        this.processedBy = entry.getProcessedBy();
        this.metadata = entry.getMetadata();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
