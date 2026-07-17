package com.sherlock.tda.model;

import java.time.LocalDateTime;
import java.util.*;

public class ThreadDump {
    private final String fileName;
    private final LocalDateTime timestamp;
    private final String jvmVersion;
    private final String serverType;
    private final List<ThreadInfo> threads;
    private final List<DeadlockInfo> deadlocks;
    private final Map<String, String> systemProperties;
    private final String rawContent;

    private ThreadDump(Builder builder) {
        this.fileName = builder.fileName;
        this.timestamp = builder.timestamp;
        this.jvmVersion = builder.jvmVersion;
        this.serverType = builder.serverType;
        this.threads = Collections.unmodifiableList(new ArrayList<>(builder.threads));
        this.deadlocks = Collections.unmodifiableList(new ArrayList<>(builder.deadlocks));
        this.systemProperties = Collections.unmodifiableMap(new HashMap<>(builder.systemProperties));
        this.rawContent = builder.rawContent;
    }

    public String getFileName() { return fileName; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getJvmVersion() { return jvmVersion; }
    public String getServerType() { return serverType; }
    public List<ThreadInfo> getThreads() { return threads; }
    public List<DeadlockInfo> getDeadlocks() { return deadlocks; }
    public Map<String, String> getSystemProperties() { return systemProperties; }
    public String getRawContent() { return rawContent; }

    public long getThreadCountByState(ThreadState state) {
        return threads.stream().filter(t -> t.getState() == state).count();
    }

    public Map<String, Long> getThreadsByPool() {
        Map<String, Long> pools = new HashMap<>();
        for (ThreadInfo thread : threads) {
            String pool = thread.getPoolName();
            pools.merge(pool, 1L, Long::sum);
        }
        return pools;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String fileName;
        private LocalDateTime timestamp;
        private String jvmVersion;
        private String serverType;
        private List<ThreadInfo> threads = new ArrayList<>();
        private List<DeadlockInfo> deadlocks = new ArrayList<>();
        private Map<String, String> systemProperties = new HashMap<>();
        private String rawContent;

        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
        public Builder jvmVersion(String jvmVersion) { this.jvmVersion = jvmVersion; return this; }
        public Builder serverType(String serverType) { this.serverType = serverType; return this; }
        public Builder threads(List<ThreadInfo> threads) { this.threads = threads; return this; }
        public Builder deadlocks(List<DeadlockInfo> deadlocks) { this.deadlocks = deadlocks; return this; }
        public Builder systemProperties(Map<String, String> props) { this.systemProperties = props; return this; }
        public Builder rawContent(String rawContent) { this.rawContent = rawContent; return this; }

        public ThreadDump build() { return new ThreadDump(this); }
    }
}
