package Graph;

import Graph.data.DatasetGenerator;
import Graph.model.*;
import Graph.scc.TarjanSCC;
import Graph.topo.KahnTopologicalSort;
import Graph.dagsp.DagShortestPaths;
import Graph.util.Metrics;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class AppMain {

    public static Graph loadJson(File f) throws Exception {
        String s = Files.readString(f.toPath());
        JSONObject o = new JSONObject(s);
        Graph g = new Graph(o.getInt("n"));
        JSONArray a = o.getJSONArray("edges");
        for (int i = 0; i < a.length(); i++) {
            JSONObject e = a.getJSONObject(i);
            int u = e.getInt("u");
            int v = e.getInt("v");
            double w = e.getDouble("w");
            g.addEdge(u, v, w);
        }
        return g;
    }

    public static void main(String[] args) throws Exception {
        new DatasetGenerator().generateAll("data");

        Graph g = loadJson(new File("data/small_1.json"));
        Metrics m1 = new Metrics();
        m1.startTimer();
        TarjanSCC tar = new TarjanSCC(g, m1);
        var comps = tar.run();
        m1.stopTimer();
        System.out.println("SCC: " + comps + " time=" + m1.elapsedNanos());

        Graph dag = buildCondensation(g, comps);
        Metrics mt = new Metrics();
        var topo = new KahnTopologicalSort(dag, mt).topoSort().orElse(List.of());
        System.out.println("Topo: " + topo);

        DagShortestPaths dsp = new DagShortestPaths(dag, new Metrics());
        var shortest = dsp.shortestFrom(0, topo);
        var longest = dsp.longestFrom(0, topo);
        System.out.println("Shortest: " + Arrays.toString(shortest.dist));
        System.out.println("Longest: " + Arrays.toString(longest.dist));
    }

    private static Graph buildCondensation(Graph g, List<List<Integer>> comps) {
        int m = comps.size();
        Graph c = new Graph(m);
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < m; i++)
            for (int v : comps.get(i))
                map.put(v, i);

        for (int u = 0; u < g.size(); u++)
            for (Edge e : g.neighbors(u)) {
                int cu = map.get(u), cv = map.get(e.to);
                if (cu != cv)
                    c.addEdge(cu, cv, e.weight);
            }
        return c;
    }
}
