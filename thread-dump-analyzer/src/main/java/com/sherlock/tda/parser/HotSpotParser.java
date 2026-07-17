package com.sherlock.tda.parser;

import com.sherlock.tda.model.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

public class HotSpotParser implements ThreadDumpParser {
    
    private static final Pattern THREAD_HEADER = Pattern.compile(
        "^\"([^\"]+)\"\\s+(?:#\\d+\\s+)?(?:prio=(\\d+)\\s+)?(?:os_prio=\\d+\\s+)?(?:tid=0x[0-9a-f]+\\s+)?(?:nid=(0x[0-9a-f]+|\\d+)\\s+)?(?:\\[0x[0-9a-f]+\\])?");
    
    private static final Pattern THREAD_STATE = Pattern.compile(
        "^\\s+java\\.lang\\.Thread\\.State:\\s+(\\w+)(?:\\s+\\(([^)]+)\\))?");
    
    private static final Pattern STACK_FRAME = Pattern.compile(
        "^\\s+at\\s+([^(]+)\\.([^()]+)\\(([^:]*)");

    @Override
    public ThreadDump parse(String content, String fileName) throws Exception {
        ThreadDump.Builder builder = ThreadDump.builder()
            .fileName(fileName)
            .timestamp(LocalDateTime.now())
            .rawContent(content);

        // Detect JVM version
        if (content.contains("Full thread dump Java HotSpot")) {
            builder.jvmVersion(extractJvmVersion(content));
        } else {
            builder.jvmVersion("Unknown");
        }
        builder.serverType("Generic JVM (HotSpot)");

        List<ThreadInfo> threads = new ArrayList<>();
        String[] lines = content.split("\n");
        
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            Matcher headerMatcher = THREAD_HEADER.matcher(line);
            
            if (headerMatcher.find()) {
                String threadName = headerMatcher.group(1);
                int priority = headerMatcher.group(2) != null ? 
                    Integer.parseInt(headerMatcher.group(2)) : 5;
                String nativeId = headerMatcher.group(3) != null ? 
                    headerMatcher.group(3) : "";
                
                // Parse daemon status from header line continuation
                boolean isDaemon = line.contains(" daemon ");
                
                i++;
                ThreadState state = ThreadState.UNKNOWN;
                List<ThreadInfo.StackFrame> stackTrace = new ArrayList<>();
                List<String> locksHeld = new ArrayList<>();
                List<String> locksWaiting = new ArrayList<>();
                
                // Parse thread state and stack trace
                while (i < lines.length) {
                    String currentLine = lines[i];
                    
                    if (currentLine.trim().isEmpty()) {
                        i++;
                        break;
                    }
                    
                    Matcher stateMatcher = THREAD_STATE.matcher(currentLine);
                    if (stateMatcher.find()) {
                        state = ThreadState.fromString(stateMatcher.group(1));
                        if (stateMatcher.group(2) != null) {
                            String condition = stateMatcher.group(2);
                            if (condition.contains("parking")) locksWaiting.add("parker");
                            if (condition.contains("waiting on")) locksWaiting.add(condition);
                        }
                    }
                    
                    Matcher frameMatcher = STACK_FRAME.matcher(currentLine);
                    if (frameMatcher.find()) {
                        String className = frameMatcher.group(1);
                        String method = frameMatcher.group(2);
                        String file = frameMatcher.group(3);
                        int lineNum = -1;
                        if (i + 1 < lines.length) {
                            String nextLine = lines[i + 1].trim();
                            if (nextLine.startsWith("at ") || nextLine.isEmpty()) {
                                // Check if current line has line number
                                int colonIdx = currentLine.lastIndexOf(':');
                                int parenIdx = currentLine.lastIndexOf(')');
                                if (colonIdx > 0 && parenIdx > colonIdx) {
                                    try {
                                        lineNum = Integer.parseInt(
                                            currentLine.substring(colonIdx + 1, parenIdx));
                                    } catch (NumberFormatException e) {
                                        // Native method or unknown
                                    }
                                }
                            }
                        }
                        stackTrace.add(new ThreadInfo.StackFrame(className, method, file, lineNum));
                    }
                    
                    if (currentLine.contains("locked <")) {
                        locksHeld.add(extractLockId(currentLine));
                    }
                    if (currentLine.contains("waiting to lock <")) {
                        locksWaiting.add(extractLockId(currentLine));
                    }
                    
                    i++;
                }
                
                String poolName = detectPoolName(threadName);
                
                threads.add(new ThreadInfo(
                    threadName, threads.size(), state, poolName,
                    stackTrace, isDaemon, priority, nativeId, locksHeld, locksWaiting
                ));
            } else {
                i++;
            }
        }
        
        builder.threads(threads);
        builder.deadlocks(extractDeadlocks(content));
        
        return builder.build();
    }

    @Override
    public boolean canParse(String content) {
        return content.contains("java.lang.Thread.State") || 
               content.contains("Full thread dump");
    }

    private String extractJvmVersion(String content) {
        Pattern p = Pattern.compile("Full thread dump (Java HotSpot\\(TM\\) [^,]+)");
        Matcher m = p.matcher(content);
        return m.find() ? m.group(1) : "Unknown JVM";
    }

    private String detectPoolName(String threadName) {
        if (threadName.contains("pool-")) return "Common Pool";
        if (threadName.contains("ForkJoinPool")) return "ForkJoinPool";
        if (threadName.contains("Reference Handler")) return "JVM System";
        if (threadName.contains("Finalizer")) return "JVM System";
        if (threadName.contains("Signal Dispatcher")) return "JVM System";
        if (threadName.contains("Attach Listener")) return "JVM System";
        if (threadName.contains("DestroyJavaVM")) return "JVM System";
        if (threadName.contains("http-")) return "HTTP";
        if (threadName.contains("ajp-")) return "AJP";
        if (threadName.startsWith("ContainerBackgroundProcessor")) return "Container";
        return "Application";
    }

    private String extractLockId(String line) {
        int start = line.indexOf('<');
        int end = line.indexOf('>', start);
        if (start >= 0 && end > start) {
            return line.substring(start, end + 1);
        }
        return "unknown";
    }

    private List<DeadlockInfo> extractDeadlocks(String content) {
        List<DeadlockInfo> deadlocks = new ArrayList<>();
        if (!content.contains("Found one Java-level deadlock")) return deadlocks;
        
        String[] sections = content.split("Found one Java-level deadlock");
        for (int i = 1; i < sections.length; i++) {
            String section = sections[i];
            List<String> threads = new ArrayList<>();
            Pattern tp = Pattern.compile("\"([^\"]+)\"");
            Matcher tm = tp.matcher(section);
            while (tm.find()) threads.add(tm.group(1));
            
            deadlocks.add(new DeadlockInfo(threads, 
                "Java-level deadlock detected", Collections.emptyList()));
        }
        return deadlocks;
    }
}
