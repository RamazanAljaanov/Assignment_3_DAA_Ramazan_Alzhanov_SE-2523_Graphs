package edu.daa.graphs;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Plots {
    private static final int W=1400,H=850;
    private static final Color[] COLORS={new Color(37,99,235),new Color(220,38,38),new Color(22,163,74),new Color(147,51,234),new Color(234,88,12)};
    private record Point(double n,double value) { }
    private static final class Series { final String name;final List<Point> points=new ArrayList<>();final Color color;final boolean dense;Series(String n,Color c,boolean d){name=n;color=c;dense=d;} }
    private Plots() { }

    public static void main(String[] args)throws Exception{
        Path csv=args.length>0?Path.of(args[0]):Path.of("results/results.csv");Path output=args.length>1?Path.of(args[1]):Path.of("results/plots");Files.createDirectories(output);
        Map<String,Series> operations=new LinkedHashMap<>(),scans=new LinkedHashMap<>();Map<String,Series> kruskal=new LinkedHashMap<>();
        List<String> algorithms=List.of("Kruskal","Prim","Kosaraju","Kahn","DagShortestPath");
        try(BufferedReader reader=Files.newBufferedReader(csv)){
            reader.readLine();String line;
            while((line=reader.readLine())!=null){String[] c=line.split(",");String alg=c[0];int n=Integer.parseInt(c[2]),m=Integer.parseInt(c[3]);boolean dense=m==20*n;double dsu=Double.parseDouble(c[5]),edge=Double.parseDouble(c[6]),comparisons=Double.parseDouble(c[7]);
                int index=algorithms.indexOf(alg);if(index<0)continue;String density=dense?"dense":"sparse",key=alg+" / "+density;Color color=COLORS[index];
                operations.computeIfAbsent(key,k->new Series(k,color,dense)).points.add(new Point(n,dsu+edge+comparisons));
                scans.computeIfAbsent(key,k->new Series(k,color,dense)).points.add(new Point(n,edge/m));
                if(alg.equals("Kruskal"))kruskal.computeIfAbsent(density,k->new Series(k,dense?new Color(220,38,38):new Color(37,99,235),dense)).points.add(new Point(n,edge/m));
            }
        }
        draw(output.resolve("operations-vs-n.png"),"Measured operations vs number of vertices",operations,true,0,0,"Operations (log scale)");
        draw(output.resolve("scans-per-edge.png"),"Edge scans per input edge",scans,false,0,2.2,"Scans / m");
        draw(output.resolve("kruskal-edge-share.png"),"Share of edges examined by Kruskal",kruskal,false,0,1.05,"Scanned edges / m");
    }

    private static void draw(Path file,String title,Map<String,Series> data,boolean logY,double yMin,double yMax,String yLabel)throws Exception{
        BufferedImage image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(Color.WHITE);g.fillRect(0,0,W,H);
        int left=105,top=90,right=390,bottom=105,pw=W-left-right,ph=H-top-bottom;g.setFont(new Font("SansSerif",Font.PLAIN,15));g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("SansSerif",Font.BOLD,25));g.drawString(title,left,42);g.setFont(new Font("SansSerif",Font.PLAIN,15));
        double xmin=3,xmax=5;
        int xTicks[]={3,4,5};for(int t:xTicks){int x=left+(int)((t-xmin)/(xmax-xmin)*pw);g.setColor(new Color(225,225,225));g.drawLine(x,top,x,top+ph);g.setColor(Color.DARK_GRAY);g.drawString("10^"+t,x-20,top+ph+27);}
        int tickCount=logY?6:6;for(int i=0;i<=tickCount;i++){double v;if(logY){double lo=0,hi=8;v=Math.pow(10,lo+(hi-lo)*i/tickCount);}else v=yMin+(yMax-yMin)*i/tickCount;int y=mapY(v,logY,yMin,yMax,top,ph);g.setColor(new Color(230,230,230));g.drawLine(left,y,left+pw,y);g.setColor(Color.DARK_GRAY);String label=logY?String.format(java.util.Locale.ROOT,"%.0e",v):String.format(java.util.Locale.ROOT,"%.1f",v);g.drawString(label,48,y+5);}
        g.setColor(Color.BLACK);g.setStroke(new BasicStroke(1.5f));g.drawLine(left,top,left,top+ph);g.drawLine(left,top+ph,left+pw,top+ph);
        g.setFont(new Font("SansSerif",Font.PLAIN,17));g.drawString("Vertices n (log scale)",left+pw/2-75,H-32);g.rotate(-Math.PI/2);g.drawString(yLabel,-top-ph/2-70,25);g.rotate(Math.PI/2);
        for(Series s:data.values()){s.points.sort((a,b)->Double.compare(a.n,b.n));g.setColor(s.color);g.setStroke(s.dense?new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,10,new float[]{8,6},0):new BasicStroke(2.5f));Point prev=null;for(Point p:s.points){if(p.value<=0)continue;int x=left+(int)((Math.log10(p.n)-xmin)/(xmax-xmin)*pw);int y=mapY(p.value,logY,yMin,yMax,top,ph);if(prev!=null){int px=left+(int)((Math.log10(prev.n)-xmin)/(xmax-xmin)*pw);int py=mapY(prev.value,logY,yMin,yMax,top,ph);g.drawLine(px,py,x,y);}g.fillOval(x-4,y-4,8,8);prev=p;}}
        int ly=top+10;g.setFont(new Font("SansSerif",Font.PLAIN,14));for(Series s:data.values()){g.setColor(s.color);g.setStroke(s.dense?new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,10,new float[]{8,6},0):new BasicStroke(2.5f));g.drawLine(left+pw+25,ly,left+pw+65,ly);g.setColor(Color.DARK_GRAY);g.drawString(s.name,left+pw+74,ly+5);ly+=27;}
        g.dispose();ImageIO.write(image,"png",file.toFile());
    }
    private static int mapY(double value,boolean log,double min,double max,int top,int height){double ratio=log?(Math.log10(Math.max(1,value))/8.0):(value-min)/(max-min);return top+height-(int)(ratio*height);}
}
