package edu.daa.graphs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public final class Graph {
    private final int vertexCount;
    private final boolean directed;
    private final List<List<Edge>> adjacency;
    private final List<Edge> edges;

    public Graph(int vertexCount, boolean directed) {
        if (vertexCount < 0) throw new IllegalArgumentException("vertex count must be non-negative");
        this.vertexCount = vertexCount;
        this.directed = directed;
        this.adjacency = new ArrayList<>(vertexCount);
        for (int i = 0; i < vertexCount; i++) adjacency.add(new ArrayList<>());
        this.edges = new ArrayList<>();
    }

    public void addEdge(int u, int v, int weight) {
        checkVertex(u); checkVertex(v);
        Edge edge = new Edge(u, v, weight);
        edges.add(edge);
        adjacency.get(u).add(edge);
        if (!directed) adjacency.get(v).add(new Edge(v, u, weight));
    }

    public int vertexCount() { return vertexCount; }
    public int edgeCount() { return edges.size(); }
    public boolean isDirected() { return directed; }
    public List<Edge> edges() { return Collections.unmodifiableList(edges); }
    public List<Edge> outgoing(int vertex) { checkVertex(vertex); return Collections.unmodifiableList(adjacency.get(vertex)); }

    public Graph reversed() {
        Graph reverse = new Graph(vertexCount, directed);
        for (Edge edge : edges) reverse.addEdge(edge.to(), edge.from(), edge.weight());
        return reverse;
    }

    private void checkVertex(int v) {
        if (v < 0 || v >= vertexCount) throw new IndexOutOfBoundsException("vertex " + v + " outside [0," + vertexCount + ")");
    }

    public static Graph read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<String> tokens = new ArrayList<>();
        for (String line : lines) {
            String useful = line.replaceFirst("#.*$", "").trim();
            if (!useful.isEmpty()) tokens.addAll(List.of(useful.split("\\s+")));
        }
        if (tokens.size() < 3) throw new IOException("graph header requires n m directed|undirected");
        try {
            int n = Integer.parseInt(tokens.get(0)), m = Integer.parseInt(tokens.get(1));
            String kind = tokens.get(2);
            if (m < 0) throw new IOException("edge count must be non-negative");
            boolean directed;
            if (kind.equalsIgnoreCase("directed")) directed = true;
            else if (kind.equalsIgnoreCase("undirected")) directed = false;
            else throw new IOException("graph type must be directed or undirected");
            if (tokens.size() != 3 + 3 * m) throw new IOException("expected " + m + " edge triples, found " + Math.max(0, tokens.size() - 3) / 3);
            Graph graph = new Graph(n, directed);
            for (int i = 0; i < m; i++) graph.addEdge(Integer.parseInt(tokens.get(3 + 3*i)), Integer.parseInt(tokens.get(4 + 3*i)), Integer.parseInt(tokens.get(5 + 3*i)));
            return graph;
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new IOException("invalid graph file: " + e.getMessage(), e);
        }
    }
}
