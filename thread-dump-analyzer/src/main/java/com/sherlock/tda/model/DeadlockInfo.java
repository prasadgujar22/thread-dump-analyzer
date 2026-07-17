package com.sherlock.tda.model;

import java.util.*;

public class DeadlockInfo {
    private final List<String> involvedThreads;
    private final String description;
    private final List<String> cycle;

    public DeadlockInfo(List<String> involvedThreads, String description, List<String> cycle) {
        this.involvedThreads = Collections.unmodifiableList(new ArrayList<>(involvedThreads));
        this.description = description;
        this.cycle = Collections.unmodifiableList(new ArrayList<>(cycle));
    }

    public List<String> getInvolvedThreads() { return involvedThreads; }
    public String getDescription() { return description; }
    public List<String> getCycle() { return cycle; }
}
