package com.sherlock.tda.model;

public enum ThreadState {
    NEW("NEW", "⏳"),
    RUNNABLE("RUNNABLE", "▶️"),
    BLOCKED("BLOCKED", "🚫"),
    WAITING("WAITING", "⏸️"),
    TIMED_WAITING("TIMED_WAITING", "⏱️"),
    TERMINATED("TERMINATED", "✓"),
    UNKNOWN("UNKNOWN", "❓");

    private final String label;
    private final String icon;

    ThreadState(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }

    public static ThreadState fromString(String state) {
        if (state == null) return UNKNOWN;
        for (ThreadState ts : values()) {
            if (ts.label.equalsIgnoreCase(state.trim())) {
                return ts;
            }
        }
        return UNKNOWN;
    }
}
