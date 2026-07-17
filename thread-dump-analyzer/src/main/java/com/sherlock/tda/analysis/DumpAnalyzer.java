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
    
    // Thread name group analysis (e.g. "http-nio-", "pool-", "GC")
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
    
    // ===== ACTIONABLE SUMMARY & REPORT =====
    
    public static String generateSummary(ThreadDump dump) {
        StringBuilder sb = new StringBuilder();
        sb.append("THREAD DUMP ANALYSIS REPORT\n");
        sb.append("============================\n\n");
        
        sb.append("SOURCE: ").append(dump.getFileName()).append("\n");
        sb.append("SERVER: ").append(dump.getServerType()).append("\n");
        sb.append("JVM: ").append(dump.getJvmVersion()).append("\n");
        sb.append("TOTAL THREADS: ").append(dump.getThreads().size()).append("\n\n");
        
        // --- KEY FINDINGS ---
        sb.append("KEY FINDINGS\n");
        sb.append("------------\n");
        
        Map<ThreadState, Long> states = analyzeStates(dump);
        long blockedCount = states.getOrDefault(ThreadState.BLOCKED, 0L);
        long waitingCount = states.getOrDefault(ThreadState.WAITING, 0L) 
                          + states.getOrDefault(ThreadState.TIMED_WAITING, 0L);
        long runnableCount = states.getOrDefault(ThreadState.RUNNABLE, 0L);
        long total = dump.getThreads().size();
        
        sb.append("* Thread State Distribution:\n");
        for (ThreadState state : ThreadState.values()) {
            long count = states.getOrDefault(state, 0L);
            if (count > 0) {
                double pct = (count * 100.0) / total;
                sb.append(String.format("  %-15s: %3d threads (%5.1f%%)\n", 
                    state.getLabel(), count, pct));
            }
        }
        
        if (blockedCount > 0) {
            sb.append("\n  WARNING: ").append(blockedCount).append(" thread(s) are BLOCKED.\n");
            sb.append("  This may indicate lock contention or deadlocks.\n");
        }
        
        if (waitingCount > total * 0.5) {
            sb.append("\n  NOTE: Over 50% of threads are in WAITING/TIMED_WAITING state.\n");
            sb.append("  This is typical for idle thread pools but may indicate I/O bottlenecks.\n");
        }
        
        if (!dump.getDeadlocks().isEmpty()) {
            sb.append("\n  CRITICAL: ").append(dump.getDeadlocks().size())
              .append(" DEADLOCK(S) DETECTED.\n");
            for (DeadlockInfo dl : dump.getDeadlocks()) {
                sb.append("  Involved: ").append(String.join(", ", dl.getInvolvedThreads())).append("\n");
            }
        }
        
        // --- HOT METHODS ---
        sb.append("\n\nHOT METHODS (Top 10)\n");
        sb.append("--------------------\n");
        List<String> hotMethods = detectHotMethods(dump, 10);
        if (hotMethods.isEmpty()) {
            sb.append("  No significant hotspots detected.\n");
        } else {
            for (String method : hotMethods) {
                sb.append("  ").append(method).append("\n");
            }
        }
        
        // --- POOL ANALYSIS ---
        sb.append("\n\nPOOL DISTRIBUTION (Top 5)\n");
        sb.append("-------------------------\n");
        Map<String, Long> pools = dump.getThreadsByPool();
        pools.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .forEach(e -> sb.append("  ")
                .append(e.getKey()).append(": ")
                .append(e.getValue()).append(" threads\n"));
        
        // --- IDENTICAL STACKS ---
        Map<String, List<ThreadInfo>> identical = findIdenticalStacks(dump);
        if (!identical.isEmpty()) {
            int totalDuplicateThreads = identical.values().stream().mapToInt(List::size).sum();
            sb.append("\n\nIDENTICAL STACKS\n");
            sb.append("----------------\n");
            sb.append("  ").append(identical.size()).append(" unique stack signatures shared by ")
              .append(totalDuplicateThreads).append(" threads.\n");
            sb.append("  This indicates thread pool contention or saturation.\n");
            identical.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(3)
                .forEach(e -> {
                    String sig = e.getKey().split("\n")[0];
                    sb.append("  [").append(e.getValue().size()).append(" threads] ")
                      .append(sig).append("\n");
                });
        }
        
        // --- STACK DEPTH ANALYSIS ---
        sb.append("\n\nSTACK DEPTH DISTRIBUTION\n");
        sb.append("------------------------\n");
        Map<String, Long> depthDist = analyzeStackDepthDistribution(dump);
        depthDist.forEach((range, count) -> {
            if (count > 0) {
                sb.append(String.format("  %-15s: %3d threads\n", range, count));
            }
        });
        
        // --- RECOMMENDATIONS ---
        sb.append("\n\nRECOMMENDATIONS\n");
        sb.append("---------------\n");
        
        if (!dump.getDeadlocks().isEmpty()) {
            sb.append("[CRITICAL] Resolve deadlocks immediately. Review lock ordering.\n");
        }
        if (blockedCount > 0) {
            sb.append("[HIGH] Investigate BLOCKED threads. Check for synchronized blocks/monitors.\n");
        }
        if (!identical.isEmpty() && identical.values().stream().anyMatch(l -> l.size() > 5)) {
            sb.append("[MEDIUM] Large groups of identical stacks detected. Consider increasing pool size.\n");
        }
        if (runnableCount > total * 0.7) {
            sb.append("[MEDIUM] High percentage of RUNNABLE threads. May indicate CPU saturation.\n");
        }
        if (waitingCount > 0 && waitingCount == total) {
            sb.append("[INFO] All threads are waiting. System is likely idle or I/O bound.\n");
        }
        if (sb.toString().endsWith("---------------\n")) {
            sb.append("No immediate issues detected. Thread distribution appears normal.\n");
        }
        
        return sb.toString();
    }
    
    public static String generateMultiSummary(MultiComparisonResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("MULTI-DUMP COMPARISON REPORT\n");
        sb.append("============================\n\n");
        sb.append("Dumps analyzed: ").append(result.getDumps().size()).append("\n");
        for (int i = 0; i < result.getDumps().size(); i++) {
            ThreadDump d = result.getDumps().get(i);
            sb.append("  [").append(i + 1).append("] ")
              .append(d.getFileName()).append(" (").append(d.getThreads().size()).append(" threads)\n");
        }
        sb.append("\n");
        
        // Key findings
        sb.append("KEY FINDINGS\n");
        sb.append("------------\n");
        sb.append("* Persistent threads (present in ALL dumps): ")
          .append(result.getPersistentThreads().size()).append("\n");
        sb.append("* Long-running threads (RUNNABLE + same frame in ALL dumps): ")
          .append(result.getLongRunningThreads().size()).append("\n");
        sb.append("* State transitions detected: ")
          .append(result.getStateTransitions().size()).append("\n\n");
        
        if (!result.getLongRunningThreads().isEmpty()) {
            sb.append("[CRITICAL] Long-running threads detected. These threads have been\n");
            sb.append("RUNNABLE at the same code location across ALL dumps.\n");
            sb.append("This strongly suggests CPU-bound infinite loops or stuck computation.\n\n");
            for (ThreadInfo t : result.getLongRunningThreads()) {
                sb.append("  TID=").append(t.getId()).append(" ").append(t.getName()).append("\n");
                if (!t.getStackTrace().isEmpty()) {
                    sb.append("    at ").append(t.getStackTrace().get(0)).append("\n");
                }
            }
            sb.append("\n");
        }
        
        if (!result.getStateTransitions().isEmpty()) {
            sb.append("* Threads that changed state between dumps:\n");
            Map<String, Long> transitionSummary = new HashMap<>();
            for (ThreadStateTransition t : result.getStateTransitions()) {
                String key = t.getFromState().getLabel() + " -> " + t.getToState().getLabel();
                transitionSummary.merge(key, 1L, Long::sum);
            }
            transitionSummary.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> sb.append("    ").append(e.getKey()).append(": ").append(e.getValue()).append("\n"));
            sb.append("\n");
        }
        
        sb.append("PAIRWISE COMPARISON SUMMARY\n");
        sb.append("---------------------------\n");
        for (int i = 0; i < result.getPairwiseResults().size(); i++) {
            ComparisonResult cr = result.getPairwiseResults().get(i);
            sb.append("[").append(i + 1).append("] ")
              .append(cr.getBaseline().getFileName())
              .append(" -> ").append(cr.getCompareTo().getFileName()).append("\n");
            sb.append("    New threads: ").append(cr.getNewThreads().size()).append("\n");
            sb.append("    Removed threads: ").append(cr.getRemovedThreads().size()).append("\n");
            long stateChanges = cr.getChanges().stream()
                .filter(c -> c.getType() == ComparisonResult.ChangeType.STATE_CHANGED).count();
            sb.append("    State changes: ").append(stateChanges).append("\n");
            
            // Show significant state deltas
            cr.getStateDelta().forEach((state, delta) -> {
                if (delta != 0) {
                    sb.append("    ").append(state.getLabel()).append(": ")
                      .append(delta > 0 ? "+" : "").append(delta).append("\n");
                }
            });
            sb.append("\n");
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
