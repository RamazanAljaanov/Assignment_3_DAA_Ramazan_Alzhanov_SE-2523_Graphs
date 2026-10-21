package edu.daa.graphs;

public final class DisjointSetUnion {
    private final int[] parent, rank;
    private int components;
    private long findCalls, successfulUnions;

    public DisjointSetUnion(int n) {
        if (n < 0) throw new IllegalArgumentException("size must be non-negative");
        parent = new int[n]; rank = new int[n]; components = n;
        for (int i = 0; i < n; i++) parent[i] = i;
    }
    public int find(int x) {
        check(x); findCalls++;
        if (parent[x] != x) parent[x] = findCompress(parent[x]);
        return parent[x];
    }
    private int findCompress(int x) {
        if (parent[x] != x) parent[x] = findCompress(parent[x]);
        return parent[x];
    }
    public boolean union(int x, int y) {
        int a = find(x), b = find(y);
        if (a == b) return false;
        if (rank[a] < rank[b]) parent[a] = b;
        else if (rank[a] > rank[b]) parent[b] = a;
        else { parent[b] = a; rank[a]++; }
        components--; successfulUnions++; return true;
    }
    public int components() { return components; }
    public long findCalls() { return findCalls; }
    public long successfulUnionCalls() { return successfulUnions; }
    private void check(int x) { if (x < 0 || x >= parent.length) throw new IndexOutOfBoundsException("element " + x); }
}
