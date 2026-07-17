package com.sherlock.tda.parser;

import com.sherlock.tda.model.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.*;

public class WebLogicParser implements ThreadDumpParser {
    
    @Override
    public ThreadDump parse(String content, String fileName) throws Exception {
        ThreadDump.Builder builder = ThreadDump.builder()
            .fileName(fileName)
            .timestamp(LocalDateTime.now())
            .rawContent(content)
            .jvmVersion(extractJvmVersion(content))
            .serverType("Oracle WebLogic");

        List<ThreadInfo> threads = new ArrayList<>();
        String[] lines = content.split("\n");
        
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            
            // WebLogic format: "[STANDBY] ExecuteThread: '1' for queue: 'weblogic.kernel.Default (self-tuning)'"
            if (line.contains("ExecuteThread") || line.contains("Thread-")) {
                String threadName = line.trim();
                boolean isDaemon = line.contains("daemon");
                int priority = 5;
                
                i++;
                ThreadState state = ThreadState.UNKNOWN;
                List<ThreadInfo.StackFrame> stackTrace = new ArrayList<>();
                List<String> locksHeld = new ArrayList<>();
                List<String> locksWaiting = new ArrayList<>();
                
                while (i < lines.length) {
                    String currentLine = lines[i];
                    if (currentLine.trim().isEmpty() && !stackTrace.isEmpty()) {
                        i++;
                        break;
                    }
                    
                    if (currentLine.contains("java.lang.Thread.State:")) {
                        String stateStr = currentLine.substring(
                            currentLine.indexOf(":") + 1).trim();
                        state = ThreadState.fromString(stateStr.split("\\s+")[0]);
                    }
                    
                    if (currentLine.trim().startsWith("at ")) {
                        parseStackFrame(currentLine, stackTrace);
                    }
                    
                    i++;
                }
                
                String poolName = detectWebLogicPool(threadName);
                threads.add(new ThreadInfo(
                    threadName, threads.size(), state, poolName,
                    stackTrace, isDaemon, priority, "", locksHeld, locksWaiting
                ));
            } else {
                i++;
            }
        }
        
        builder.threads(threads);
        builder.deadlocks(new ArrayList<>());
        return builder.build();
    }

    @Override
    public boolean canParse(String content) {
        return content.contains("ExecuteThread") || 
               content.contains("weblogic.kernel");
    }

    private String extractJvmVersion(String content) {
        Pattern p = Pattern.compile("(Java HotSpot\\(TM\\) [^,]+|OpenJDK [^,]+)");
        Matcher m = p.matcher(content);
        return m.find() ? m.group(1) : "Unknown";
    }

    private String detectWebLogicPool(String threadName) {
        if (threadName.contains("weblogic.kernel.Default")) return "WLS Default";
        if (threadName.contains("weblogic.admin")) return "WLS Admin";
        if (threadName.contains("ExecuteThread")) return "WLS Execute";
        return "WebLogic";
    }

    private void parseStackFrame(String line, List<ThreadInfo.StackFrame> stackTrace) {
        String trimmed = line.trim().substring(3); // Remove "at "
        int parenIdx = trimmed.indexOf('(');
        if (parenIdx > 0) {
            String classAndMethod = trimmed.substring(0, parenIdx);
            int lastDot = classAndMethod.lastIndexOf('.');
            if (lastDot > 0) {
                String className = classAndMethod.substring(0, lastDot);
                String method = classAndMethod.substring(lastDot + 1);
                String file = "Unknown";
                int lineNum = -1;
                
                int colonIdx = trimmed.indexOf(':', parenIdx);
                int closeParen = trimmed.indexOf(')', parenIdx);
                if (colonIdx > 0 && closeParen > colonIdx) {
                    file = trimmed.substring(parenIdx + 1, colonIdx);
                    try {
                        lineNum = Integer.parseInt(trimmed.substring(colonIdx + 1, closeParen));
                    } catch (NumberFormatException ignored) {}
                } else if (closeParen > parenIdx) {
                    file = trimmed.substring(parenIdx + 1, closeParen);
                }
                
                stackTrace.add(new ThreadInfo.StackFrame(className, method, file, lineNum));
            }
        }
    }
}
