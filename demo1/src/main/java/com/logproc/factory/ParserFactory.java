package com.logproc.factory;

import com.logproc.strategy.LogParser;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ParserFactory {

    // Spring Boot automatically finds all @Component LogParsers and injects them here
    private final List<LogParser> strategies;

    public ParserFactory(List<LogParser> strategies) {
        this.strategies = strategies;
    }

    public LogParser getParser(String rawLine) {
        return strategies.stream()
                .filter(strategy -> strategy.canHandle(rawLine))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No suitable parser found for line: " + rawLine));
    }
}