package edu.daa.graphs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public final class Benchmark {
    private static final int[] SIZES={1_000,3_162,10_000,31_623,100_000};
    private static final int[] MULTIPLIERS={4,20};
    private static final long[][] MST={{147970,30363},{468911,97589},{1486139,309550},{4715515,968517},{15012094,3056681}};
    private static final long[][] SCC={{27,1},{137,1},{373,1},{1188,1},{3923,1}};
    private static final long[][] DIST={{396274,252411},{1271911,723157},{3941828,2381342},{12551891,7376612},{40096539,23512087}};
    private Benchmark() { }

    public static void main(String[] args) throws Exception {
        Path csv=args.length==0?Path.of("results/results.csv"):Path.of(args[0]);
        Files.createDirectories(csv.toAbsolutePath().getParent());
        Path tarjanCsv=csv.toAbsolutePath().resolveSibling("tarjan-scan-comparison.csv");
        try(BufferedWriter out=Files.newBufferedWriter(csv); BufferedWriter comparison=Files.newBufferedWriter(tarjanCsv)) {
            out.write("algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result\n");
            comparison.write("n,m,kosaraju_edge_scans,tarjan_edge_scans,scan_reduction_fraction\n");
            for(int si=0;si<SIZES.length;si++) for(int di=0;di<MULTIPLIERS.length;di++) {
                int n=SIZES[si],m=Math.multiplyExact(n,MULTIPLIERS[di]);
                Graph undirected=generate(n,m,Kind.UNDIRECTED);
                GraphAlgorithms.MstResult kruskal=GraphAlgorithms.kruskal(undirected);
                GraphAlgorithms.MstResult prim=GraphAlgorithms.prim(undirected);
                require(kruskal.weight()==prim.weight(),"MST algorithms disagree for n="+n+", m="+m);
                require(kruskal.weight()==MST[si][di],"Kruskal reference mismatch n="+n+", m="+m+": "+kruskal.weight());
                require(prim.weight()==MST[si][di],"Prim reference mismatch n="+n+", m="+m+": "+prim.weight());
                write(out,"Kruskal","undirected",n,m,measure(()->GraphAlgorithms.kruskal(undirected)),r->{var x=(GraphAlgorithms.MstResult)r.value;return row(x.dsuOps(),x.edgeScans(),x.comparisons(),0,x.weight());});
                write(out,"Prim","undirected",n,m,measure(()->GraphAlgorithms.prim(undirected)),r->{var x=(GraphAlgorithms.MstResult)r.value;return row(x.dsuOps(),x.edgeScans(),x.comparisons(),0,x.weight());});

                Graph directed=generate(n,m,Kind.DIRECTED);
                GraphAlgorithms.SccResult scc=GraphAlgorithms.kosaraju(directed);
                require(scc.components().size()==SCC[si][di],"SCC reference mismatch n="+n+", m="+m+": "+scc.components().size());
                GraphAlgorithms.TarjanResult tarjan=GraphAlgorithms.tarjan(directed);
                require(sameComponents(scc.components(),tarjan.components()),"Tarjan/Kosaraju disagreement n="+n+", m="+m);
                require(scc.edgeScans()==2L*m&&tarjan.edgeScans()==m,"unexpected SCC edge scans n="+n+", m="+m);
                comparison.write(n+","+m+","+scc.edgeScans()+","+tarjan.edgeScans()+",0.500\n");
                write(out,"Kosaraju","directed",n,m,measure(()->GraphAlgorithms.kosaraju(directed)),r->{var x=(GraphAlgorithms.SccResult)r.value;return row(0,x.edgeScans(),0,x.maxDepth(),x.components().size());});

                Graph dag=generate(n,m,Kind.DAG);
                GraphAlgorithms.TopologicalResult topo=GraphAlgorithms.kahn(dag);
                require(topo.acyclic()&&validOrder(dag,topo.order()),"invalid topological order n="+n+", m="+m);
                write(out,"Kahn","dag",n,m,measure(()->GraphAlgorithms.kahn(dag)),r->{var x=(GraphAlgorithms.TopologicalResult)r.value;return row(0,x.edgeScans(),0,0,x.order().size());});
                GraphAlgorithms.DagPathsResult paths=GraphAlgorithms.dagShortestPaths(dag);
                long sum=Arrays.stream(paths.distances()).sum();
                require(sum==DIST[si][di],"DAG distance reference mismatch n="+n+", m="+m+": "+sum);
                write(out,"DagShortestPath","dag",n,m,measure(()->GraphAlgorithms.dagShortestPaths(dag)),r->{var x=(GraphAlgorithms.DagPathsResult)r.value;return row(0,x.edgeScans(),x.comparisons(),0,Arrays.stream(x.distances()).sum());});
                out.flush();
                System.out.printf("n=%d m=%d: MST=%d, SCC=%d, DAG-distance-sum=%d%n",n,m,kruskal.weight(),scc.components().size(),sum);
            }
        }
        System.out.println("CSV written to "+csv.toAbsolutePath());
        System.out.println("Tarjan scan comparison written to "+tarjanCsv);
    }

    private static boolean sameComponents(List<List<Integer>> left,List<List<Integer>> right) {
        if(left.size()!=right.size())return false;
        List<String> a=left.stream().map(c->c.stream().sorted().map(String::valueOf).collect(Collectors.joining(","))).sorted().toList();
        List<String> b=right.stream().map(c->c.stream().sorted().map(String::valueOf).collect(Collectors.joining(","))).sorted().toList();
        return a.equals(b);
    }

    private enum Kind { UNDIRECTED,DIRECTED,DAG }
    private static Graph generate(int n,int m,Kind kind) {
        if(m<n-1 && kind==Kind.UNDIRECTED) throw new IllegalArgumentException("MST generator needs m >= n-1");
        Random rnd=new Random(42); Graph graph=new Graph(n,kind!=Kind.UNDIRECTED);
        int[] rank=null;
        if(kind==Kind.UNDIRECTED||kind==Kind.DAG) {
            rank=new int[n];for(int i=0;i<n;i++)rank[i]=i;
            for(int i=n-1;i>0;i--){int j=rnd.nextInt(i+1);int tmp=rank[i];rank[i]=rank[j];rank[j]=tmp;}
        }
        if(kind==Kind.UNDIRECTED) {
            int[] perm=rank;
            for(int i=1;i<n;i++)graph.addEdge(perm[rnd.nextInt(i)],perm[i],1+rnd.nextInt(1000));
        }
        int remaining=m-graph.edgeCount();
        for(int i=0;i<remaining;i++) {
            int a=rnd.nextInt(n),b=rnd.nextInt(n);while(b==a)b=rnd.nextInt(n);
            if(kind==Kind.DAG&&rank[a]>rank[b]){int t=a;a=b;b=t;}
            graph.addEdge(a,b,1+rnd.nextInt(1000));
        }
        return graph;
    }
    private static boolean validOrder(Graph g,List<Integer> order){int[] pos=new int[g.vertexCount()];for(int i=0;i<order.size();i++)pos[order.get(i)]=i;for(Edge e:g.edges())if(pos[e.from()]>=pos[e.to()])return false;return true;}
    private interface Supplier { Object get(); }
    private interface RowMaker { String make(Timed r); }
    private static final class Timed { final Object value;final double time;Timed(Object v,double t){value=v;time=t;} }
    private static Timed measure(Supplier task){Object last=null;double[] ms=new double[5];for(int i=0;i<5;i++){long start=System.nanoTime();last=task.get();ms[i]=(System.nanoTime()-start)/1_000_000.0;}Arrays.sort(ms);return new Timed(last,ms[2]);}
    private static String row(long dsu,long scans,long comparisons,int depth,long result){return dsu+","+scans+","+comparisons+","+depth+","+result;}
    private static void write(BufferedWriter out,String algorithm,String graph,int n,int m,Timed result,RowMaker maker)throws IOException{out.write(algorithm+","+graph+","+n+","+m+","+String.format(java.util.Locale.ROOT,"%.3f",result.time)+","+maker.make(result)+"\n");}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
}
