/**
 * 
 */
package rinde.sim.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import rinde.sim.core.graph.Connection;
import rinde.sim.core.graph.ConnectionData;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.LengthData;
import rinde.sim.core.graph.MultimapGraph;
import rinde.sim.core.graph.Point;
import rinde.sim.core.graph.TableGraph;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class DotUtils {

  public static <E extends ConnectionData> void saveToDot(Graph<E> mp,
      String fileName, boolean pdf) {

    try {
      final FileWriter fileWriter = new FileWriter(fileName + ".dot");

      final BufferedWriter out = new BufferedWriter(fileWriter);

      final StringBuilder string = new StringBuilder();
      string.append("digraph genegraph {\n");

      int nodeId = 0;
      final HashMap<Point, Integer> idMap = new HashMap<Point, Integer>();
      for (final Point p : mp.getNodes()) {
        string.append("node" + nodeId + "[pos=\"" + p.x / 3 + "," + p.y / 3
            + "\", label=\"" + p + "\", pin=true]\n");
        idMap.put(p, nodeId);
        nodeId++;
      }

      for (final Connection<E> entry : mp.getConnections()) {

        final String label = ""
            + Math.round(mp.connectionLength(entry.from, entry.to) * 10d) / 10d;
        if (!idMap.containsKey(entry.to)) {
          final Point p = entry.to;
          string.append("node" + nodeId + "[pos=\"" + p.x / 3 + "," + p.y / 3
              + "\", label=\"" + p + "\", pin=true]\n");
          idMap.put(p, nodeId);
          nodeId++;
        }
        string.append("node" + idMap.get(entry.from) + " -> node"
            + idMap.get(entry.to) + "[label=\"" + label + "\"]\n");
      }

      string.append('}');
      out.append(string);
      out.close();

      if (pdf) {
        dotToPDF(fileName + ".dot", fileName + ".pdf");
      }

    } catch (final IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  // copied from rinde graduation
  protected static final boolean IS_DOT_AVAILABLE = checkCommandAvailability("/usr/local/bin/dot");

  private static boolean dotToPDF(final String dotFile, final String pdfFile) {
    if (IS_DOT_AVAILABLE) {
      try {
        // Execute a command with an argument that contains a space
        // "-Kneato", // -Kfdp
        final String[] commands = new String[] { "/usr/local/bin/dot", "-o",
            pdfFile, "-Tpdf", dotFile };
        // System.out.println(Arrays.toString(commands).replace(",",
        // ""));
        final Process p = Runtime.getRuntime().exec(commands);

        // final BufferedReader stdInput = new BufferedReader(new
        // InputStreamReader(p.getInputStream()));

        final BufferedReader stdError = new BufferedReader(
            new InputStreamReader(p.getErrorStream()));

        String s;
        boolean flag = false;
        // read any errors from the attempted command
        while ((s = stdError.readLine()) != null) {
          System.err.println(s);
          flag = true;
        }
        if (flag) {
          throw new RuntimeException("Error while converting \"" + dotFile
              + "\" to \"" + pdfFile + "\"");
        }
        return true;
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  public static Graph<LengthData> parseDot(String file) {
    try {
      final BufferedReader reader = new BufferedReader(new FileReader(file));

      final TableGraph<LengthData> graph = new TableGraph<LengthData>(
          LengthData.EMPTY);
      boolean containsDistances = false;

      final HashMap<String, Point> nodeMapping = new HashMap<String, Point>();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains("pos=")) {
          final String nodeName = line.substring(0, line.indexOf("[")).trim();
          final String[] position = line.split("\"")[1].split(",");
          final Point p = new Point(Double.parseDouble(position[0]),
              Double.parseDouble(position[1]));
          nodeMapping.put(nodeName, p);
        } else if (line.contains("->")) {
          // example:
          // node1004 -> node820[label="163.3"]
          final String[] names = line.split("->");
          final String fromStr = names[0].trim();
          final String toStr = names[1].substring(0, names[1].indexOf("["))
              .trim();
          final double distance = Double.parseDouble(line.split("\"")[1]);
          final Point from = nodeMapping.get(fromStr);
          final Point to = nodeMapping.get(toStr);
          // if (!from.equals(to)) {
          if (Point.distance(from, to) == distance) {
            graph.addConnection(from, to);
          } else {
            graph.addConnection(from, to, new LengthData(distance));
            containsDistances = true;
          }
          // }
        }
      }
      // if (containsDistances) {
      // return graph;
      // } else {
      final Graph<LengthData> g = new MultimapGraph<LengthData>();
      g.merge(graph);
      return g;
      // }
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean checkCommandAvailability(final String... command) {
    try {
      Runtime.getRuntime().exec(command).destroy();
      return true;
    } catch (final IOException ioe) {
      return false;
    }
  }
}
