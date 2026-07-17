package com.sherlock.tda.parser;

import com.sherlock.tda.model.ThreadDump;
import java.io.*;
import java.nio.file.*;

public interface ThreadDumpParser {
    ThreadDump parse(String content, String fileName) throws Exception;
    boolean canParse(String content);

    static ThreadDumpParser detectParser(String content) {
        if (content.contains("WebLogic") || content.contains("ExecuteThread")) {
            return new WebLogicParser();
        } else if (content.contains("WebSphere") || content.contains("WS-Addressing")) {
            return new WebSphereParser();
        } else {
            return new HotSpotParser();
        }
    }

    static ThreadDump parseFile(Path path) throws Exception {
        String content = Files.readString(path);
        ThreadDumpParser parser = detectParser(content);
        return parser.parse(content, path.getFileName().toString());
    }
}
