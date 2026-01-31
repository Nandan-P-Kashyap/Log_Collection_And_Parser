package com.logproc.strategy;

import com.logproc.model.LogEntry;

public interface LogParser {
    // Identifies if the parser can handle the specific format
    boolean canHandle(String rawLine);

    // ⚠️ UPDATE: Now accepts threadName to maintain immutability & performance
    LogEntry parse(String rawLine, String threadName);
}