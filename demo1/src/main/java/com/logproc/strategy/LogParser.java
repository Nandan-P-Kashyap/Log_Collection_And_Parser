package com.logproc.strategy;

import com.logproc.model.LogEntry;

public interface LogParser {
    // Identifies if the parser can handle the specific format
    boolean canHandle(String rawLine);

    // The actual "Middle Task" logic using our index scanning
    LogEntry parse(String rawLine);
}
