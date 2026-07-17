package com.sherlock.tda.model;

import java.util.*;

public class ComparisonResult {
    private final ThreadDump baseline;
    private final ThreadDump compareTo;
    private final List<ThreadChange> changes;
    private final Map<ThreadState, Long> stateDelta;
    private final List<String> newThreads;
    private final List<String> removedThreads;

    public ComparisonResult(ThreadDump baseline, ThreadDump compareTo,
                           List<ThreadChange> changes, Map<ThreadState, Long> stateDelta,
                           List<String> newThreads, List<String> removedThreads) {
        this.baseline = baseline;
        this.compareTo = compareTo;
        this.changes = Collections.unmodifiableList(changes);
        this.stateDelta = Collections.unmodifiableMap(stateDelta);
        this.newThreads = Collections.unmodifiableList(newThreads);
        this.removedThreads = Collections.unmodifiableList(removedThreads);
    }

    public ThreadDump getBaseline() { return baseline; }
    public ThreadDump getCompareTo() { return compareTo; }
    public List<ThreadChange> getChanges() { return changes; }
    public Map<ThreadState, Long> getStateDelta() { return stateDelta; }
    public List<String> getNewThreads() { return newThreads; }
    public List<String> getRemovedThreads() { return removedThreads; }

    public static class ThreadChange {
        private final String threadName;
        private final ThreadState oldState;
        private final ThreadState newState;
        private final ChangeType type;

        public ThreadChange(String threadName, ThreadState oldState, ThreadState newState, ChangeType type) {
            this.threadName = threadName;
            this.oldState = oldState;
            this.newState = newState;
            this.type = type;
        }

        public String getThreadName() { return threadName; }
        public ThreadState getOldState() { return oldState; }
        public ThreadState getNewState() { return newState; }
        public ChangeType getType() { return type; }
    }

    public enum ChangeType {
        STATE_CHANGED, NEW_THREAD, REMOVED_THREAD, UNCHANGED
    }
}
