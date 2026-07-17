package com.sherlock.tda.parser;

import com.sherlock.tda.model.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.*;

public class WebSphereParser implements ThreadDumpParser {
    
    @Override
    public ThreadDump parse(String content, String fileName) throws Exception {
        ThreadDump.Builder builder = ThreadDump.builder()
            .fileName(fileName)
            .timestamp(LocalDateTime.now())
            .rawContent(content)
            .jvmVersion(extractJvmVersion(content))
            .serverType("IBM WebSphere");

        List<ThreadInfo> threads = new ArrayList<>();
        String[] lines = content.split("\n");
        
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            
            // WebSphere format often has thread IDs at start
            if (line.contains("Thread:") || line.startsWith("\"")) {
                String threadName = extractThreadName(line);
                boolean isDaemon = line.contains("daemon");
                
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
                    
                    if (currentLine.contains("State:")) {
                        String stateStr = currentLine.substring(
                            currentLine.indexOf(":") + 1).trim();
                        state = ThreadState.fromString(stateStr);
                    }
                    
                    if (currentLine.trim().startsWith("at ")) {
                        parseStackFrame(currentLine, stackTrace);
                    }
                    
                    i++;
                }
                
                String poolName = detectWebSpherePool(threadName);
                threads.add(new ThreadInfo(
                    threadName, threads.size(), state, poolName,
                    stackTrace, isDaemon, 5, "", locksHeld, locksWaiting
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
        return content.contains("WebSphere") || 
               content.contains("IBM J9 VM");
    }

    private String extractThreadName(String line) {
        int quoteStart = line.indexOf('"');
        int quoteEnd = line.indexOf('"', quoteStart + 1);
        if (quoteStart >= 0 && quoteEnd > quoteStart) {
            return line.substring(quoteStart + 1, quoteEnd);
        }
        return line.trim();
    }

    private String extractJvmVersion(String content) {
        Pattern p = Pattern.compile("(IBM J9 VM [^\n]+|Java HotSpot[^,]+)");
        Matcher m = p.matcher(content);
        return m.find() ? m.group(1) : "Unknown";
    }

    private String detectWebSpherePool(String threadName) {
        if (threadName.contains("WebContainer")) return "WebContainer";
        if (threadName.contains("ORB")) return "ORB";
        if (threadName.contains("Alarm")) return "Alarm";
        if (threadName.contains("Session")) return "Session";
        return "WebSphere";
    }

    private void parseStackFrame(String line, List<ThreadInfo.StackFrame> stackTrace) {
        String trimmed = line.trim().substring(3);
        int parenIdx = trimmed.indexOf('(');
        if (parenIdx > 0) {
            String classAndMethod = trimmed.substring(0, parenIdx);
            int lastDot = classAndMethod.lastIndexOf('.');
            if (lastDot > 0) {
                String className = classAndMethod.substring(0, lastDot);
                String method = classAndMethod.substring(lastDot + 1);
                stackTrace.add(new ThreadInfo.StackFrame(className, method, "Unknown", -1));
            }
        }
    }
}
