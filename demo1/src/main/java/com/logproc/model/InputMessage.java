package com.logproc.model;

public final class InputMessage {
    private final String line;
    private final boolean poison;

    public static final InputMessage POISON_PILL = new InputMessage(null, true);

    private InputMessage(String line, boolean poison) {
        this.line = line;
        this.poison = poison;
    }

    public static InputMessage of(String line) {
        return new InputMessage(line, false);
    }

    public boolean isPoison() { return poison; }

    public String getLine() { return line; }
}
