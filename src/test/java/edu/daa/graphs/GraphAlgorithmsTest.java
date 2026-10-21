package edu.daa.graphs;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class GraphAlgorithmsTest {
    @Test void graphReaderLoadsDirectedAndUndirectedInput() throws Exception {
        Path file=Files.createTempFile("graph-input", ".txt");
        try {
            Files.writeString(file,"# sample\n4 3 undirected\n0 1 8\n0 1 2 # parallel edge\n2 2 -1\n");
            Graph g=Graph.read(file);assertFalse(g.isDirected());assertEquals(3,g.edgeCount());assertEquals(2,g.outgoing(0).size());assertEquals(2,g.outgoing(1).size());assertEquals(4,g.vertexCount());
        } finally { Files.deleteIfExists(file); }
    }

    @Test void assignmentDataGraphsAreUsedAndDirectedSccAlgorithmsAgree() throws Exception {
        Graph disconnected = Graph.read(Path.of("data/disconnected.txt"));
        assertEquals(3, GraphAlgorithms.kruskal(disconnected).components());

        Graph cyclic = Graph.read(Path.of("data/cyclic-directed.txt"));
        assertFalse(GraphAlgorithms.kahn(cyclic).acyclic());
        assertEquivalentPartitions(GraphAlgorithms.kosaraju(cyclic).components(), GraphAlgorithms.tarjan(cyclic).components());

        Graph negativeDag = Graph.read(Path.of("data/dag-negative.txt"));
        assertTrue(GraphAlgorithms.kahn(negativeDag).acyclic());
        assertSccAlgorithmsAgree(negativeDag);
        assertTrue(Arrays.stream(GraphAlgorithms.dagShortestPaths(negativeDag).distances()).anyMatch(distance -> distance < 0));
    }

    @Test void dsuMatchesReferencePartitionAfterRandomUnions() {
        int n=120;DisjointSetUnion dsu=new DisjointSetUnion(n);int[] label=new int[n];for(int i=0;i<n;i++)label[i]=i;
        Random random=new Random(7);
        for(int i=0;i<700;i++){
            int a=random.nextInt(n),b=random.nextInt(n);boolean same=label[a]==label[b];
            assertEquals(!same,dsu.union(a,b));
            if(!same){int old=label[b],replacement=label[a];for(int j=0;j<n;j++)if(label[j]==old)label[j]=replacement;}
            for(int j=0;j<n;j++)for(int k=0;k<n;k++)assertEquals(label[j]==label[k],dsu.find(j)==dsu.find(k));
            assertEquals(Arrays.stream(label).distinct().count(),dsu.components());
        }
        assertTrue(dsu.findCalls()>0);assertEquals(n-dsu.components(),dsu.successfulUnionCalls());
    }

    @Test void kruskalAndPrimAgreeAcrossRandomConnectedGraphs() {
        Random random=new Random(19);
        for(int trial=0;trial<100;trial++){
            int n=2+random.nextInt(25);Graph g=new Graph(n,false);
            for(int v=1;v<n;v++)g.addEdge(v,random.nextInt(v),random.nextInt(101)-20);
            int extra=random.nextInt(n*n/2);
            for(int i=0;i<extra;i++){int a=random.nextInt(n),b=random.nextInt(n);if(a!=b)g.addEdge(a,b,random.nextInt(101)-20);}
            var k=GraphAlgorithms.kruskal(g);var p=GraphAlgorithms.prim(g);
            assertEquals(k.weight(),p.weight());assertEquals(n-1,k.edges().size());assertEquals(n-1,p.edges().size());
        }
    }

    @Test void kruskalReturnsForestForDisconnectedGraphAndIgnoresLoops() {
        Graph g=new Graph(5,false);g.addEdge(0,0,-100);g.addEdge(0,1,4);g.addEdge(0,1,2);g.addEdge(3,4,7);
        var k=GraphAlgorithms.kruskal(g);var p=GraphAlgorithms.prim(g);
        assertEquals(3,k.components());assertEquals(3,p.components());assertEquals(9,k.weight());assertEquals(k.weight(),p.weight());
    }

    @Test void kosarajuMatchesMutualReachabilityBruteForce() {
        Random random=new Random(23);
        for(int trial=0;trial<30;trial++){
            int n=1+random.nextInt(18);Graph g=new Graph(n,true);
            for(int i=0;i<n*n/2;i++)g.addEdge(random.nextInt(n),random.nextInt(n),1);
            boolean[][] reach=new boolean[n][n];
            for(int s=0;s<n;s++){ArrayDeque<Integer> q=new ArrayDeque<>();q.add(s);reach[s][s]=true;while(!q.isEmpty()){int u=q.remove();for(Edge e:g.outgoing(u))if(!reach[s][e.to()]){reach[s][e.to()]=true;q.add(e.to());}}}
            int[] component=new int[n];Arrays.fill(component,-1);int id=0;
            for(int u=0;u<n;u++)if(component[u]<0){for(int v=0;v<n;v++)if(component[v]<0&&reach[u][v]&&reach[v][u])component[v]=id;id++;}
            var actual=GraphAlgorithms.kosaraju(g).components();int[] actualId=new int[n];for(int c=0;c<actual.size();c++)for(int v:actual.get(c))actualId[v]=c;
            for(int u=0;u<n;u++)for(int v=0;v<n;v++)assertEquals(component[u]==component[v],actualId[u]==actualId[v]);
            var tarjan = GraphAlgorithms.tarjan(g);
            assertEquivalentPartitions(actual, tarjan.components());
            assertEquals(g.edgeCount(), tarjan.edgeScans());
        }
    }

    @Test void condensationIsAcyclicAndLongestPathUsesNegatedEdgeWeights() {
        Graph g = new Graph(6, true);
        g.addEdge(0, 1, 1); g.addEdge(1, 0, 1);
        g.addEdge(2, 3, 1); g.addEdge(3, 2, 1);
        g.addEdge(0, 2, 4); g.addEdge(1, 4, 2);
        g.addEdge(3, 5, 7); g.addEdge(4, 5, 10);

        var scc = GraphAlgorithms.kosaraju(g);
        var condensed = GraphAlgorithms.condensation(g, scc.components());
        assertEquals(4, condensed.graph().vertexCount());
        assertTrue(GraphAlgorithms.kahn(condensed.graph()).acyclic());

        int source = condensed.componentOfVertex()[0];
        int target = condensed.componentOfVertex()[5];
        var longest = GraphAlgorithms.dagLongestPaths(condensed.graph());
        assertEquals(12, longest.distances()[target]);
        List<Integer> path = GraphAlgorithms.reconstructPath(target, longest.parent());
        assertEquals(source, path.get(0));
        long weight = 0;
        for (int i = 1; i < path.size(); i++) {
            int from = path.get(i - 1), to = path.get(i);
            weight += condensed.graph().outgoing(from).stream().filter(edge -> edge.to() == to)
                    .mapToInt(Edge::weight).max().orElseThrow();
        }
        assertEquals(longest.distances()[target], weight);
    }

    @Test void kahnValidatesOrderAndDetectsCycle() {
        Graph dag=new Graph(5,true);dag.addEdge(0,2,1);dag.addEdge(1,2,1);dag.addEdge(2,3,1);dag.addEdge(1,4,1);
        assertSccAlgorithmsAgree(dag);
        var result=GraphAlgorithms.kahn(dag);assertTrue(result.acyclic());assertEquals(5,result.order().size());
        int[] p=new int[5];for(int i=0;i<5;i++)p[result.order().get(i)]=i;for(Edge e:dag.edges())assertTrue(p[e.from()]<p[e.to()]);
        dag.addEdge(3,1,1);assertFalse(GraphAlgorithms.kahn(dag).acyclic());assertSccAlgorithmsAgree(dag);
    }

    @Test void dagPathsAgreeWithBellmanFordAndSupportNegativeWeightsAndParents() {
        Random random=new Random(31);
        for(int trial=0;trial<50;trial++){
            int n=2+random.nextInt(18);Graph g=new Graph(n,true);int[] rank=new int[n];for(int i=0;i<n;i++)rank[i]=i;
            for(int i=n-1;i>0;i--){int j=random.nextInt(i+1),t=rank[i];rank[i]=rank[j];rank[j]=t;}
            for(int i=0;i<n*n/3;i++){int a=random.nextInt(n),b=random.nextInt(n);if(a!=b){if(rank[a]>rank[b]){int t=a;a=b;b=t;}g.addEdge(a,b,random.nextInt(31)-15);}}
            assertSccAlgorithmsAgree(g);
            long[] expected=new long[n];Arrays.fill(expected,Long.MAX_VALUE);int[] indegree=new int[n];for(Edge e:g.edges())indegree[e.to()]++;for(int v=0;v<n;v++)if(indegree[v]==0)expected[v]=0;
            for(int i=0;i<n;i++)for(Edge e:g.edges())if(expected[e.from()]!=Long.MAX_VALUE)expected[e.to()]=Math.min(expected[e.to()],expected[e.from()]+e.weight());
            var actual=GraphAlgorithms.dagShortestPaths(g);assertArrayEquals(expected,actual.distances());
            for(int v=0;v<n;v++)if(actual.parent()[v]>=0){int target=v,u=actual.parent()[v];assertTrue(g.outgoing(u).stream().anyMatch(e->e.to()==target&&actual.distances()[u]+e.weight()==actual.distances()[target]));}
        }
    }

    @Test void iterativeSccHandlesOneHundredThousandVertexPath() {
        int n=100_000;Graph g=new Graph(n,true);for(int i=0;i<n-1;i++)g.addEdge(i,i+1,1);
        var result=assertDoesNotThrow(()->GraphAlgorithms.kosaraju(g));assertEquals(n,result.components().size());assertEquals(2L*(n-1),result.edgeScans());assertTrue(result.maxDepth()<=n);
        var tarjan=assertDoesNotThrow(()->GraphAlgorithms.tarjan(g));assertEquals(n,tarjan.components().size());assertEquals(n-1,tarjan.edgeScans());assertTrue(tarjan.maxDepth()<=n);
    }

    private static void assertEquivalentPartitions(List<List<Integer>> left, List<List<Integer>> right) {
        assertEquals(left.size(), right.size());
        Set<Set<Integer>> leftSets = new HashSet<>(), rightSets = new HashSet<>();
        for (List<Integer> component : left) leftSets.add(new HashSet<>(component));
        for (List<Integer> component : right) rightSets.add(new HashSet<>(component));
        assertEquals(leftSets, rightSets);
    }

    private static void assertSccAlgorithmsAgree(Graph graph) {
        assertEquivalentPartitions(GraphAlgorithms.kosaraju(graph).components(), GraphAlgorithms.tarjan(graph).components());
    }
}
