package com.sherlock.tda.model;

import java.util.*;

public class ThreadInfo {
    private final String name;
    private final long id;
    private final ThreadState state;
    private final String poolName;
    private final List<StackFrame> stackTrace;
    private final boolean isDaemon;
    private final int priority;
    private final String nativeId;
    private final List<String> locksHeld;
    private final List<String> locksWaiting;

    public ThreadInfo(String name, long id, ThreadState state, String poolName,
                      List<StackFrame> stackTrace, boolean isDaemon, int priority,
                      String nativeId, List<String> locksHeld, List<String> locksWaiting) {
        this.name = name;
        this.id = id;
        this.state = state;
        this.poolName = poolName;
        this.stackTrace = Collections.unmodifiableList(new ArrayList<>(stackTrace));
        this.isDaemon = isDaemon;
        this.priority = priority;
        this.nativeId = nativeId;
        this.locksHeld = Collections.unmodifiableList(new ArrayList<>(locksHeld));
        this.locksWaiting = Collections.unmodifiableList(new ArrayList<>(locksWaiting));
    }

    public String getName() { return name; }
    public long getId() { return id; }
    public ThreadState getState() { return state; }
    public String getPoolName() { return poolName != null ? poolName : "Unknown"; }
    public List<StackFrame> getStackTrace() { return stackTrace; }
    public boolean isDaemon() { return isDaemon; }
    public int getPriority() { return priority; }
    public String getNativeId() { return nativeId; }
    public List<String> getLocksHeld() { return locksHeld; }
    public List<String> getLocksWaiting() { return locksWaiting; }

    public boolean isIdle() {
        if (stackTrace.isEmpty()) return false;
        String topFrame = stackTrace.get(0).getMethod();
        return topFrame.contains("wait") || topFrame.contains("sleep") || 
               topFrame.contains("park") || topFrame.contains("poll");
    }

    public static class StackFrame {
        private final String className;
        private final String method;
        private final String file;
        private final int line;

        public StackFrame(String className, String method, String file, int line) {
            this.className = className;
            this.method = method;
            this.file = file;
            this.line = line;
        }

        public String getClassName() { return className; }
        public String getMethod() { return method; }
        public String getFile() { return file; }
        public int getLine() { return line; }

        @Override
        public String toString() {
            return className + "." + method + "(" + file + ":" + line + ")";
        }
    }
}
