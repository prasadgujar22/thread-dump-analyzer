package com.sherlock.tda.analysis;

import com.sherlock.tda.model.*;
import java.util.*;
import java.util.stream.*;

public class DumpAnalyzer {
    
    public static Map<ThreadState, Long> analyzeStates(ThreadDump dump) {
        return dump.getThreads().stream()
            .collect(Collectors.groupingBy(
                ThreadInfo::getState,
                Collectors.counting()
            ));
    }
    
    public static List<ThreadInfo> findBlockedThreads(ThreadDump dump) {
        return dump.getThreads().stream()
            .filter(t -> t.getState() == ThreadState.BLOCKED)
            .collect(Collectors.toList());
    }
    
    // PROPER: Find long-running threads using TID-based multi-dump comparison
    // A thread is long-running if:
    // 1. It appears in ALL dumps (same TID)
    // 2. It is RUNNABLE in ALL dumps
    // 3. Its top stack frame is IDENTICAL across all dumps (same class, method, line number)
    public static List<ThreadInfo> findLongRunningThreads(List<ThreadDump> dumps) {
        if (dumps == null || dumps.size() < 2) {
            return Collections.emptyList();
        }
        
        // Start with threads from first dump
        Map<Long, ThreadInfo> candidates = new HashMap<>();
        for (ThreadInfo t : dumps.get(0).getThreads()) {
            if (t.getState() == ThreadState.RUNNABLE) {
                candidates.put(t.getId(), t);
            }
        }
        
        // Check each subsequent dump
        for (int i = 1; i < dumps.size(); i++) {
            Map<Long, ThreadInfo> currentDumpThreads = new HashMap<>();
            for (ThreadInfo t : dumps.get(i).getThreads()) {
                if (t.getState() == ThreadState.RUNNABLE) {
                    currentDumpThreads.put(t.getId(), t);
                }
            }
            
            // Retain only threads present in this dump with same top stack frame
            Iterator<Map.Entry<Long, ThreadInfo>> it = candidates.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, ThreadInfo> entry = it.next();
                long tid = entry.getKey();
                ThreadInfo candidate = entry.getValue();
                ThreadInfo current = currentDumpThreads.get(tid);
                
                if (current == null || !haveSameTopFrame(candidate, current)) {
                    it.remove();
                }
            }
        }
        
        return new ArrayList<>(candidates.values());
    }
    
    private static boolean haveSameTopFrame(ThreadInfo a, ThreadInfo b) {
        List<ThreadInfo.StackFrame> stackA = a.getStackTrace();
        List<ThreadInfo.StackFrame> stackB = b.getStackTrace();
        
        if (stackA.isEmpty() || stackB.isEmpty()) return false;
        
        ThreadInfo.StackFrame topA = stackA.get(0);
        ThreadInfo.StackFrame topB = stackB.get(0);
        
        return topA.getClassName().equals(topB.getClassName()) 
            && topA.getMethod().equals(topB.getMethod())
            && topA.getLine() == topB.getLine();
    }
    
    // TID-based comparison: Track ALL threads across dumps, not just RUNNABLE
    // Returns a map of TID -> list of thread appearances across dumps
    public static Map<Long, List<ThreadInfo>> trackThreadsByTid(List<ThreadDump> dumps) {
        Map<Long, List<ThreadInfo>> tracked = new HashMap<>();
        for (ThreadDump dump : dumps) {
            for (ThreadInfo t : dump.getThreads()) {
                tracked.computeIfAbsent(t.getId(), k -> new ArrayList<>()).add(t);
            }
        }
        return tracked;
    }
    
    // Find threads that persisted across ALL dumps (any state, using TID)
    public static List<ThreadInfo> findPersistentThreadsByTid(List<ThreadDump> dumps) {
        if (dumps == null || dumps.isEmpty()) return Collections.emptyList();
        
        Map<Long, ThreadInfo> firstDumpThreads = new HashMap<>();
        for (ThreadInfo t : dumps.get(0).getThreads()) {
            firstDumpThreads.put(t.getId(), t);
        }
        
        List<ThreadInfo> persistent = new ArrayList<>();
        for (Map.Entry<Long, ThreadInfo> entry : firstDumpThreads.entrySet()) {
            long tid = entry.getKey();
            boolean foundInAll = true;
            
            for (int i = 1; i < dumps.size(); i++) {
                boolean found = false;
                for (ThreadInfo t : dumps.get(i).getThreads()) {
                    if (t.getId() == tid) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    foundInAll = false;
                    break;
                }
            }
            
            if (foundInAll) {
                persistent.add(entry.getValue());
            }
        }
        return persistent;
    }
    
    // Find threads that changed state between dumps (using TID)
    public static List<ThreadStateTransition> findStateTransitions(List<ThreadDump> dumps) {
        if (dumps == null || dumps.size() < 2) return Collections.emptyList();
        
        List<ThreadStateTransition> transitions = new ArrayList<>();
        Map<Long, ThreadInfo> previousThreads = new HashMap<>();
        for (ThreadInfo t : dumps.get(0).getThreads()) {
            previousThreads.put(t.getId(), t);
        }
        
        for (int i = 1; i < dumps.size(); i++) {
            Map<Long, ThreadInfo> currentThreads = new HashMap<>();
            for (ThreadInfo t : dumps.get(i).getThreads()) {
                currentThreads.put(t.getId(), t);
            }
            
            // Find threads that changed state
            for (Map.Entry<Long, ThreadInfo> entry : currentThreads.entrySet()) {
                long tid = entry.getKey();
                ThreadInfo current = entry.getValue();
                ThreadInfo previous = previousThreads.get(tid);
                
                if (previous != null && previous.getState() != current.getState()) {
                    transitions.add(new ThreadStateTransition(
                        tid, current.getName(), 
                        previous.getState(), current.getState(),
                        i, previous.getPoolName()));
                }
            }
            
            previousThreads = currentThreads;
        }
        
        return transitions;
    }
    
    // Find threads that are in the same state across ALL dumps (using TID)
    public static List<ThreadInfo> findPersistentThreads(List<ThreadDump> dumps) {
        if (dumps == null || dumps.size() < 2) return Collections.emptyList();
        
        Map<Long, ThreadInfo> firstDumpThreads = new HashMap<>();
        for (ThreadInfo t : dumps.get(0).getThreads()) {
            firstDumpThreads.put(t.getId(), t);
        }
        
        List<ThreadInfo> persistent = new ArrayList<>();
        for (Map.Entry<Long, ThreadInfo> entry : firstDumpThreads.entrySet()) {
            long tid = entry.getKey();
            ThreadInfo firstThread = entry.getValue();
            boolean sameStateInAll = true;
            
            for (int i = 1; i < dumps.size(); i++) {
                ThreadInfo other = null;
                for (ThreadInfo t : dumps.get(i).getThreads()) {
                    if (t.getId() == tid) {
                        other = t;
                        break;
                    }
                }
                if (other == null || other.getState() != firstThread.getState()) {
                    sameStateInAll = false;
                    break;
                }
            }
            
            if (sameStateInAll) {
                persistent.add(firstThread);
            }
        }
        return persistent;
    }
    
    // Deprecated: single-dump stack depth heuristic (kept for backward compatibility)
    public static List<ThreadInfo> findLongRunningThreads(ThreadDump dump, long minStackDepth) {
        return dump.getThreads().stream()
            .filter(t -> t.getStackTrace().size() >= minStackDepth)
            .sorted((a, b) -> Integer.compare(b.getStackTrace().size(), a.getStackTrace().size()))
            .collect(Collectors.toList());
    }
    
    public static Map<String, List<ThreadInfo>> groupByPool(ThreadDump dump) {
        return dump.getThreads().stream()
            .collect(Collectors.groupingBy(ThreadInfo::getPoolName));
    }
    
    public static List<String> detectHotMethods(ThreadDump dump, int topN) {
        Map<String, Long> methodCounts = new HashMap<>();
        for (ThreadInfo thread : dump.getThreads()) {
            for (ThreadInfo.StackFrame frame : thread.getStackTrace()) {
                String key = frame.getClassName() + "." + frame.getMethod();
                methodCounts.merge(key, 1L, Long::sum);
            }
        }
        return methodCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(topN)
            .map(e -> e.getKey() + " (" + e.getValue() + " threads)")
            .collect(Collectors.toList());
    }
    
    // Stack length distribution for pie chart
    public static Map<String, Long> analyzeStackDepthDistribution(ThreadDump dump) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("1-10 frames", 0L);
        distribution.put("11-20 frames", 0L);
        distribution.put("21-30 frames", 0L);
        distribution.put("31-50 frames", 0L);
        distribution.put("50+ frames", 0L);
        
        for (ThreadInfo t : dump.getThreads()) {
            int size = t.getStackTrace().size();
            if (size <= 10) distribution.merge("1-10 frames", 1L, Long::sum);
            else if (size <= 20) distribution.merge("11-20 frames", 1L, Long::sum);
            else if (size <= 30) distribution.merge("21-30 frames", 1L, Long::sum);
            else if (size <= 50) distribution.merge("31-50 frames", 1L, Long::sum);
            else distribution.merge("50+ frames", 1L, Long::sum);
        }
        
        return distribution;
    }
    
    // Thread name group analysis (e.g., "http-nio-", "pool-", "GC")
    public static Map<String, Long> analyzeThreadNameGroups(ThreadDump dump) {
        Map<String, Long> groups = new HashMap<>();
        for (ThreadInfo t : dump.getThreads()) {
            String name = t.getName();
            String group = extractThreadGroup(name);
            groups.merge(group, 1L, Long::sum);
        }
        return groups.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .collect(Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue,
                (e1, e2) -> e1, LinkedHashMap::new));
    }
    
    private static String extractThreadGroup(String threadName) {
        if (threadName.startsWith("http-")) return "HTTP Threads";
        if (threadName.startsWith("pool-")) return "Thread Pools";
        if (threadName.startsWith("GC")) return "GC Threads";
        if (threadName.contains("DestroyJavaVM")) return "JVM";
        if (threadName.contains("Reference")) return "Reference Handlers";
        if (threadName.contains("Finalizer")) return "Finalizer";
        if (threadName.contains("Signal")) return "Signal Dispatcher";
        if (threadName.startsWith("RMI")) return "RMI Threads";
        if (threadName.startsWith("JMX")) return "JMX Threads";
        if (threadName.contains("AWT")) return "AWT/Swing";
        if (threadName.contains("Java2D")) return "Java2D";
        return "Application Threads";
    }
    
    // Find identical stack traces (threads with exactly same stack)
    public static Map<String, List<ThreadInfo>> findIdenticalStacks(ThreadDump dump) {
        Map<String, List<ThreadInfo>> identical = new HashMap<>();
        for (ThreadInfo t : dump.getThreads()) {
            String stackKey = stackTraceToString(t.getStackTrace());
            identical.computeIfAbsent(stackKey, k -> new ArrayList<>()).add(t);
        }
        // Remove entries with only 1 thread (no duplicates)
        identical.entrySet().removeIf(e -> e.getValue().size() <= 1);
        return identical;
    }
    
    private static String stackTraceToString(List<ThreadInfo.StackFrame> stack) {
        StringBuilder sb = new StringBuilder();
        for (ThreadInfo.StackFrame f : stack) {
            sb.append(f.getClassName()).append(".").append(f.getMethod()).append("\n");
        }
        return sb.toString();
    }
    
    // Multi-dump comparison with TID tracking
    public static MultiComparisonResult compareMultiple(List<ThreadDump> dumps) {
        if (dumps == null || dumps.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 dumps for comparison");
        }
        
        List<ComparisonResult> pairwiseResults = new ArrayList<>();
        for (int i = 0; i < dumps.size() - 1; i++) {
            pairwiseResults.add(compare(dumps.get(i), dumps.get(i + 1)));
        }
        
        List<ThreadInfo> persistentThreads = findPersistentThreadsByTid(dumps);
        List<ThreadInfo> longRunningThreads = findLongRunningThreads(dumps);
        List<ThreadStateTransition> stateTransitions = findStateTransitions(dumps);
        Map<Long, List<ThreadInfo>> trackedThreads = trackThreadsByTid(dumps);
        
        Map<ThreadState, List<Long>> stateHistory = new EnumMap<>(ThreadState.class);
        for (ThreadState state : ThreadState.values()) {
            List<Long> counts = new ArrayList<>();
            for (ThreadDump dump : dumps) {
                counts.add(analyzeStates(dump).getOrDefault(state, 0L));
            }
            stateHistory.put(state, counts);
        }
        
        return new MultiComparisonResult(dumps, pairwiseResults, persistentThreads, 
                                         longRunningThreads, stateTransitions, trackedThreads, stateHistory);
    }
    
    public static ComparisonResult compare(ThreadDump baseline, ThreadDump compareTo) {
        // Use TID for matching threads
        Map<Long, ThreadInfo> baselineMap = new HashMap<>();
        for (ThreadInfo t : baseline.getThreads()) {
            baselineMap.put(t.getId(), t);
        }
        Map<Long, ThreadInfo> compareMap = new HashMap<>();
        for (ThreadInfo t : compareTo.getThreads()) {
            compareMap.put(t.getId(), t);
        }
        
        List<ComparisonResult.ThreadChange> changes = new ArrayList<>();
        List<String> newThreads = new ArrayList<>();
        List<String> removedThreads = new ArrayList<>();
        
        for (Map.Entry<Long, ThreadInfo> entry : compareMap.entrySet()) {
            long tid = entry.getKey();
            ThreadInfo newThread = entry.getValue();
            ThreadInfo oldThread = baselineMap.get(tid);
            
            if (oldThread == null) {
                newThreads.add(newThread.getName() + " (TID=" + tid + ")");
                changes.add(new ComparisonResult.ThreadChange(
                    newThread.getName(), null, newThread.getState(), 
                    ComparisonResult.ChangeType.NEW_THREAD));
            } else if (oldThread.getState() != newThread.getState()) {
                changes.add(new ComparisonResult.ThreadChange(
                    newThread.getName(), oldThread.getState(), newThread.getState(),
                    ComparisonResult.ChangeType.STATE_CHANGED));
            }
        }
        
        for (Map.Entry<Long, ThreadInfo> entry : baselineMap.entrySet()) {
            long tid = entry.getKey();
            ThreadInfo t = entry.getValue();
            if (!compareMap.containsKey(tid)) {
                removedThreads.add(t.getName() + " (TID=" + tid + ")");
                changes.add(new ComparisonResult.ThreadChange(
                    t.getName(), t.getState(), null,
                    ComparisonResult.ChangeType.REMOVED_THREAD));
            }
        }
        
        Map<ThreadState, Long> stateDelta = new EnumMap<>(ThreadState.class);
        Map<ThreadState, Long> baseStates = analyzeStates(baseline);
        Map<ThreadState, Long> compareStates = analyzeStates(compareTo);
        
        for (ThreadState state : ThreadState.values()) {
            long delta = compareStates.getOrDefault(state, 0L) - 
                        baseStates.getOrDefault(state, 0L);
            stateDelta.put(state, delta);
        }
        
        return new ComparisonResult(baseline, compareTo, changes, stateDelta, 
                                    newThreads, removedThreads);
    }
    
    public static String generateSummary(ThreadDump dump) {
        StringBuilder sb = new StringBuilder();
        sb.append("Thread Dump Analysis Summary\n");
        sb.append("============================\n\n");
        sb.append("File: ").append(dump.getFileName()).append("\n");
        sb.append("Server: ").append(dump.getServerType()).append("\n");
        sb.append("JVM: ").append(dump.getJvmVersion()).append("\n");
        sb.append("Total Threads: ").append(dump.getThreads().size()).append("\n\n");
        
        sb.append("Thread States:\n");
        Map<ThreadState, Long> states = analyzeStates(dump);
        states.forEach((state, count) -> 
            sb.append("  ").append(state.getIcon()).append(" ")
              .append(state.getLabel()).append(": ").append(count).append("\n"));
        
        if (!dump.getDeadlocks().isEmpty()) {
            sb.append("\nDEADLOCKS DETECTED: ").append(dump.getDeadlocks().size()).append("\n");
        }
        
        sb.append("\nTop Pools:\n");
        dump.getThreadsByPool().entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .forEach(e -> sb.append("  ").append(e.getKey()).append(": ")
                .append(e.getValue()).append(" threads\n"));
        
        return sb.toString();
    }
    
    public static String generateMultiSummary(MultiComparisonResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Multi-Dump Comparison Summary\n");
        sb.append("=============================\n\n");
        sb.append("Dumps compared: ").append(result.getDumps().size()).append("\n\n");
        
        sb.append("Persistent threads (present in ALL dumps, any state): ")
          .append(result.getPersistentThreads().size()).append("\n");
        sb.append("Long-running threads (RUNNABLE + same top frame in ALL dumps): ")
          .append(result.getLongRunningThreads().size()).append("\n");
        sb.append("State transitions detected: ")
          .append(result.getStateTransitions().size()).append("\n\n");
        
        for (int i = 0; i < result.getPairwiseResults().size(); i++) {
            ComparisonResult cr = result.getPairwiseResults().get(i);
            sb.append("Comparison ").append(i + 1).append(": ")
              .append(cr.getBaseline().getFileName())
              .append(" -> ").append(cr.getCompareTo().getFileName()).append("\n");
            sb.append("  New threads: ").append(cr.getNewThreads().size()).append("\n");
            sb.append("  Removed threads: ").append(cr.getRemovedThreads().size()).append("\n");
            sb.append("  State changes: ").append(
                cr.getChanges().stream().filter(c -> c.getType() == ComparisonResult.ChangeType.STATE_CHANGED).count()
            ).append("\n\n");
        }
        
        return sb.toString();
    }
    
    // Inner class for state transitions
    public static class ThreadStateTransition {
        private final long tid;
        private final String threadName;
        private final ThreadState fromState;
        private final ThreadState toState;
        private final int dumpIndex; // which comparison this transition occurred in
        private final String poolName;
        
        public ThreadStateTransition(long tid, String threadName, 
                                     ThreadState fromState, ThreadState toState,
                                     int dumpIndex, String poolName) {
            this.tid = tid;
            this.threadName = threadName;
            this.fromState = fromState;
            this.toState = toState;
            this.dumpIndex = dumpIndex;
            this.poolName = poolName;
        }
        
        public long getTid() { return tid; }
        public String getThreadName() { return threadName; }
        public ThreadState getFromState() { return fromState; }
        public ThreadState getToState() { return toState; }
        public int getDumpIndex() { return dumpIndex; }
        public String getPoolName() { return poolName; }
    }
}
