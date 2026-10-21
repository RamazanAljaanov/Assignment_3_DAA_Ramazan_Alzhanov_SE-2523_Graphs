# DAA Assignment 3 - Graphs

Java 17 implementation for the graph assignment. The project uses one weighted adjacency-list `Graph` for directed and undirected inputs. The algorithms are implemented in `GraphAlgorithms`; Java collections are used for storage and priority queues, not for graph algorithms.

## Build and run

Install JDK 17 or newer and Maven 3.9 or newer, then run from this directory:

```text
mvn test
mvn package
java -jar target/assignment3-graphs-1.0.jar
```

The benchmark writes `results/results.csv`, runs five timed repetitions per algorithm and graph, and checks every reference result before finishing. It also compares Tarjan and Kosaraju on all ten generated directed graphs and writes their edge scans to `results/tarjan-scan-comparison.csv`. The required 100 000 vertex / 2 000 000 edge case takes longer than the small cases. To generate the three PNG charts after the CSV exists:

```text
java -cp target/classes edu.daa.graphs.Plots
```

To write the benchmark CSV to another path, pass it as the first argument:

```text
java -cp target/classes edu.daa.graphs.Benchmark path/to/results.csv
```

## Project map

- `src/main/java/edu/daa/graphs/Graph.java` - adjacency lists, edge list, and text input parser.
- `DisjointSetUnion.java` - union by rank and path compression, with operation counters.
- `GraphAlgorithms.java` - Kruskal, Prim, iterative Kosaraju and Tarjan, Kahn, DAG shortest and longest paths, SCC condensation, and path reconstruction.
- `Benchmark.java` - deterministic graph generation, cross-checks, reference values, timing, and CSV output.
- `Plots.java` - PNG plots generated with the JDK only.
- `src/test/java/edu/daa/graphs/GraphAlgorithmsTest.java` - JUnit 5 cross-checks, randomized tests, and the 100 000 vertex stack-safety test.
- `data/` - three hand-written graph examples.
- `results/` - benchmark output and plots.

## Graph input format

The first line contains `n m directed` or `n m undirected`. The next `m` lines each contain `u v weight`. Vertices use zero-based numbering. Blank lines and text after `#` are ignored. Parallel edges, self-loops, isolated vertices, and disconnected graphs are supported.

## Git and submission

The assignment asks for `main`, feature branches `feature/dsu`, `feature/mst`, `feature/scc`, `feature/paths`, and `feature/metrics`, plus the release tag `v1.0`. The required archive name is `DAA_Assignment3_name_surname_group.zip`; replace the placeholders with the student's name and group before uploading to Moodle. Include a link to the GitHub repository on `main` tagged `v1.0`.

**AI-use acknowledgement:** AI assistance was used to draft and review parts of the implementation, tests, benchmark, plots, and report. The student should verify the work, understand every algorithm, and disclose this assistance in accordance with the course rules.
