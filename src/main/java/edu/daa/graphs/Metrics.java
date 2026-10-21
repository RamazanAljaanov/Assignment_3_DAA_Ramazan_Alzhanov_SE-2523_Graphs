package edu.daa.graphs;

public final class Metrics {
    public long dsuOps, edgeScans, comparisons;
    public int maxDepth;
    public void reset() { dsuOps = edgeScans = comparisons = 0; maxDepth = 0; }
}
