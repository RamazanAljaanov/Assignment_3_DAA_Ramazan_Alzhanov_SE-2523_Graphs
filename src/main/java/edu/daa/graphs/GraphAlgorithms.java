package edu.daa.graphs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public final class GraphAlgorithms {
    private GraphAlgorithms() { }
    public record MstResult(long weight, int components, List<Edge> edges, long dsuOps, long edgeScans, long comparisons) { }
    public record SccResult(List<List<Integer>> components, long edgeScans, int maxDepth) { }
    public record TarjanResult(List<List<Integer>> components, long edgeScans, int maxDepth) { }
    public record TopologicalResult(List<Integer> order, boolean acyclic, long edgeScans) { }
    public record DagPathsResult(long[] distances, int[] parent, long edgeScans, long comparisons) { }
    public record CondensationResult(Graph graph, int[] componentOfVertex, List<List<Integer>> components) { }
    public record DagLongestPathResult(long[] distances, int[] parent, long edgeScans, long comparisons) { }
    private record QueueEdge(int vertex, int parent, int weight) { }

    public static MstResult kruskal(Graph g) {
        if (g.isDirected()) throw new IllegalArgumentException("MST requires an undirected graph");
        Metrics metrics = new Metrics();
        List<Edge> sorted = new ArrayList<>(g.edges());
        sorted.sort((a,b) -> { metrics.comparisons++; return Integer.compare(a.weight(), b.weight()); });
        DisjointSetUnion dsu = new DisjointSetUnion(g.vertexCount());
        List<Edge> tree = new ArrayList<>(); long total = 0;
        for (Edge e : sorted) {
            metrics.edgeScans++;
            if (dsu.union(e.from(), e.to())) { tree.add(e); total += e.weight(); }
            if (tree.size() == g.vertexCount() - 1) break;
        }
        return new MstResult(total, dsu.components(), List.copyOf(tree), dsu.findCalls()+dsu.successfulUnionCalls(), metrics.edgeScans, metrics.comparisons);
    }

    public static MstResult prim(Graph g) {
        if (g.isDirected()) throw new IllegalArgumentException("MST requires an undirected graph");
        int n = g.vertexCount(); boolean[] inTree = new boolean[n];
        long[] comparisons = {0};
        PriorityQueue<QueueEdge> queue = new PriorityQueue<>((a,b) -> { comparisons[0]++; return Integer.compare(a.weight(),b.weight()); });
        List<Edge> chosen = new ArrayList<>(); long total=0, scans=0; int components=0;
        for (int root=0; root<n; root++) {
            if (inTree[root]) continue;
            components++; inTree[root]=true;
            for (Edge e:g.outgoing(root)) { scans++; queue.add(new QueueEdge(e.to(),root,e.weight())); }
            while (!queue.isEmpty()) {
                QueueEdge q=queue.remove(); if (inTree[q.vertex()]) continue;
                inTree[q.vertex()]=true; total+=q.weight(); chosen.add(new Edge(q.parent(),q.vertex(),q.weight()));
                for (Edge e:g.outgoing(q.vertex())) { scans++; if (!inTree[e.to()]) queue.add(new QueueEdge(e.to(),q.vertex(),e.weight())); }
            }
        }
        return new MstResult(total,components,List.copyOf(chosen),0,scans,comparisons[0]);
    }

    public static SccResult kosaraju(Graph g) {
        int n=g.vertexCount(); boolean[] seen=new boolean[n]; List<Integer> finish=new ArrayList<>(n);
        int maxDepth=0; long scans=0;
        for (int start=0;start<n;start++) if (!seen[start]) {
            ArrayDeque<int[]> stack=new ArrayDeque<>(); seen[start]=true; stack.push(new int[]{start,0}); maxDepth=Math.max(maxDepth,stack.size());
            while(!stack.isEmpty()) {
                int[] frame=stack.peek(); List<Edge> out=g.outgoing(frame[0]);
                if(frame[1]<out.size()) { Edge e=out.get(frame[1]++); scans++; int v=e.to(); if(!seen[v]) {seen[v]=true;stack.push(new int[]{v,0});maxDepth=Math.max(maxDepth,stack.size());} }
                else {finish.add(frame[0]);stack.pop();}
            }
        }
        Graph rev=g.reversed(); Arrays.fill(seen,false); List<List<Integer>> groups=new ArrayList<>();
        for(int i=finish.size()-1;i>=0;i--) { int start=finish.get(i); if(seen[start])continue;
            List<Integer> group=new ArrayList<>(); ArrayDeque<Integer> stack=new ArrayDeque<>(); stack.push(start);seen[start]=true;maxDepth=Math.max(maxDepth,stack.size());
            while(!stack.isEmpty()) { int u=stack.pop();group.add(u); for(Edge e:rev.outgoing(u)){scans++;int v=e.to();if(!seen[v]){seen[v]=true;stack.push(v);maxDepth=Math.max(maxDepth,stack.size());}} }
            groups.add(List.copyOf(group));
        }
        return new SccResult(List.copyOf(groups),scans,maxDepth);
    }

    public static TarjanResult tarjan(Graph g) {
        if (!g.isDirected()) throw new IllegalArgumentException("SCC requires a directed graph");
        int n = g.vertexCount();
        int[] index = new int[n], low = new int[n];
        Arrays.fill(index, -1);
        boolean[] onStack = new boolean[n];
        ArrayDeque<Integer> active = new ArrayDeque<>();
        List<List<Integer>> groups = new ArrayList<>();
        int nextIndex = 0, maxDepth = 0;
        long scans = 0;

        for (int root = 0; root < n; root++) {
            if (index[root] >= 0) continue;
            ArrayDeque<int[]> dfs = new ArrayDeque<>();
            index[root] = low[root] = nextIndex++;
            active.push(root);
            onStack[root] = true;
            dfs.push(new int[]{root, 0, -1});
            maxDepth = Math.max(maxDepth, dfs.size());

            while (!dfs.isEmpty()) {
                int[] frame = dfs.peek();
                int vertex = frame[0];
                List<Edge> outgoing = g.outgoing(vertex);
                if (frame[1] < outgoing.size()) {
                    Edge edge = outgoing.get(frame[1]++);
                    scans++;
                    int next = edge.to();
                    if (index[next] < 0) {
                        index[next] = low[next] = nextIndex++;
                        active.push(next);
                        onStack[next] = true;
                        dfs.push(new int[]{next, 0, vertex});
                        maxDepth = Math.max(maxDepth, dfs.size());
                    } else if (onStack[next]) {
                        low[vertex] = Math.min(low[vertex], index[next]);
                    }
                } else {
                    dfs.pop();
                    int parent = frame[2];
                    if (parent >= 0) low[parent] = Math.min(low[parent], low[vertex]);
                    if (low[vertex] == index[vertex]) {
                        List<Integer> group = new ArrayList<>();
                        int member;
                        do {
                            member = active.pop();
                            onStack[member] = false;
                            group.add(member);
                        } while (member != vertex);
                        groups.add(List.copyOf(group));
                    }
                }
            }
        }
        return new TarjanResult(List.copyOf(groups), scans, maxDepth);
    }

    public static CondensationResult condensation(Graph g, List<List<Integer>> components) {
        if (!g.isDirected()) throw new IllegalArgumentException("condensation requires a directed graph");
        int[] componentOf = new int[g.vertexCount()];
        Arrays.fill(componentOf, -1);
        List<List<Integer>> immutableComponents = new ArrayList<>(components.size());
        for (int id = 0; id < components.size(); id++) {
            List<Integer> component = List.copyOf(components.get(id));
            if (component.isEmpty()) throw new IllegalArgumentException("components must be non-empty");
            for (int vertex : component) {
                if (vertex < 0 || vertex >= componentOf.length || componentOf[vertex] >= 0)
                    throw new IllegalArgumentException("components must partition the graph vertices");
                componentOf[vertex] = id;
            }
            immutableComponents.add(component);
        }
        for (int id : componentOf) if (id < 0)
            throw new IllegalArgumentException("components must cover every graph vertex");

        Graph condensed = new Graph(components.size(), true);
        for (Edge edge : g.edges()) {
            int from = componentOf[edge.from()], to = componentOf[edge.to()];
            if (from != to) condensed.addEdge(from, to, edge.weight());
        }
        return new CondensationResult(condensed, componentOf, List.copyOf(immutableComponents));
    }

    public static TopologicalResult kahn(Graph g) {
        if(!g.isDirected()) throw new IllegalArgumentException("topological sort requires a directed graph");
        int n=g.vertexCount();int[] indegree=new int[n];
        for(Edge e:g.edges())indegree[e.to()]++;
        ArrayDeque<Integer> queue=new ArrayDeque<>();for(int v=0;v<n;v++)if(indegree[v]==0)queue.add(v);
        List<Integer> order=new ArrayList<>(n);long scans=0;
        while(!queue.isEmpty()){int u=queue.remove();order.add(u);for(Edge e:g.outgoing(u)){scans++;if(--indegree[e.to()]==0)queue.add(e.to());}}
        return new TopologicalResult(List.copyOf(order),order.size()==n,scans);
    }

    public static DagPathsResult dagShortestPaths(Graph g) {
        TopologicalResult topo=kahn(g);if(!topo.acyclic())throw new IllegalArgumentException("DAG shortest paths require an acyclic graph");
        int n=g.vertexCount();long[] dist=new long[n];Arrays.fill(dist,Long.MAX_VALUE);int[] parent=new int[n];Arrays.fill(parent,-1);
        int[] indegree=new int[n];for(Edge e:g.edges())indegree[e.to()]++;
        
        for(int v=0;v<n;v++)if(indegree[v]==0)dist[v]=0;
        long scans=0, comparisons=0;
        for(int u:topo.order())for(Edge e:g.outgoing(u)){scans++;comparisons++;long candidate=dist[u]+e.weight();if(candidate<dist[e.to()]){dist[e.to()]=candidate;parent[e.to()]=u;}}
        return new DagPathsResult(dist,parent,scans,comparisons);
    }

    public static DagLongestPathResult dagLongestPaths(Graph g) {
        TopologicalResult topo = kahn(g);
        if (!topo.acyclic()) throw new IllegalArgumentException("longest paths require a DAG");
        int n = g.vertexCount();
        int[] indegree = new int[n], parent = new int[n];
        Arrays.fill(parent, -1);
        for (Edge edge : g.edges()) indegree[edge.to()]++;
        long[] negatedDistances = new long[n];
        Arrays.fill(negatedDistances, Long.MAX_VALUE);
        for (int vertex = 0; vertex < n; vertex++) if (indegree[vertex] == 0) negatedDistances[vertex] = 0;

        long scans = 0, comparisons = 0;
        for (int from : topo.order()) {
            if (negatedDistances[from] == Long.MAX_VALUE) continue;
            for (Edge edge : g.outgoing(from)) {
                scans++;
                comparisons++;
                long candidate = negatedDistances[from] - edge.weight();
                if (candidate < negatedDistances[edge.to()]) {
                    negatedDistances[edge.to()] = candidate;
                    parent[edge.to()] = from;
                }
            }
        }
        long[] distances = new long[n];
        for (int vertex = 0; vertex < n; vertex++) distances[vertex] = -negatedDistances[vertex];
        return new DagLongestPathResult(distances, parent, scans, comparisons);
    }


    public static List<Integer> reconstructPath(int target, int[] parent) {
        if (target < 0 || target >= parent.length) throw new IndexOutOfBoundsException("target " + target);
        ArrayDeque<Integer> reverse = new ArrayDeque<>();
        boolean[] seen = new boolean[parent.length];
        for (int v = target; v != -1; v = parent[v]) {
            if (v < 0 || v >= parent.length || seen[v]) throw new IllegalArgumentException("invalid parent chain");
            seen[v] = true; reverse.push(v);
        }
        return List.copyOf(reverse);
    }
}
