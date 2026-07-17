package com.sherlock.tda.model;

import com.sherlock.tda.analysis.DumpAnalyzer;
import java.util.*;

public class MultiComparisonResult {
    private final List<ThreadDump> dumps;
    private final List<ComparisonResult> pairwiseResults;
    private final List<ThreadInfo> persistentThreads;
    private final List<ThreadInfo> longRunningThreads;
    private final List<DumpAnalyzer.ThreadStateTransition> stateTransitions;
    private final Map<Long, List<ThreadInfo>> trackedThreads;
    private final Map<ThreadState, List<Long>> stateHistory;

    public MultiComparisonResult(List<ThreadDump> dumps, 
                                  List<ComparisonResult> pairwiseResults,
                                  List<ThreadInfo> persistentThreads,
                                  List<ThreadInfo> longRunningThreads,
                                  List<DumpAnalyzer.ThreadStateTransition> stateTransitions,
                                  Map<Long, List<ThreadInfo>> trackedThreads,
                                  Map<ThreadState, List<Long>> stateHistory) {
        this.dumps = Collections.unmodifiableList(new ArrayList<>(dumps));
        this.pairwiseResults = Collections.unmodifiableList(new ArrayList<>(pairwiseResults));
        this.persistentThreads = Collections.unmodifiableList(new ArrayList<>(persistentThreads));
        this.longRunningThreads = Collections.unmodifiableList(new ArrayList<>(longRunningThreads));
        this.stateTransitions = Collections.unmodifiableList(new ArrayList<>(stateTransitions));
        this.trackedThreads = Collections.unmodifiableMap(new HashMap<>(trackedThreads));
        this.stateHistory = Collections.unmodifiableMap(new EnumMap<>(stateHistory));
    }

    public List<ThreadDump> getDumps() { return dumps; }
    public List<ComparisonResult> getPairwiseResults() { return pairwiseResults; }
    public List<ThreadInfo> getPersistentThreads() { return persistentThreads; }
    public List<ThreadInfo> getLongRunningThreads() { return longRunningThreads; }
    public List<DumpAnalyzer.ThreadStateTransition> getStateTransitions() { return stateTransitions; }
    public Map<Long, List<ThreadInfo>> getTrackedThreads() { return trackedThreads; }
    public Map<ThreadState, List<Long>> getStateHistory() { return stateHistory; }
}
