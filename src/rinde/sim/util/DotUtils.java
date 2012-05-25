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
import java.util.Map.Entry;

import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultimapGraph;
import rinde.sim.core.graph.Point;
import rinde.sim.core.graph.TableGraph;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class DotUtils {

	public static void saveToDot(Graph mp, String fileName, boolean pdf) {

		try {
			FileWriter fileWriter = new FileWriter(fileName + ".dot");

			final BufferedWriter out = new BufferedWriter(fileWriter);

			final StringBuilder string = new StringBuilder();
			string.append("digraph genegraph {\n");

			int nodeId = 0;
			HashMap<Point, Integer> idMap = new HashMap<Point, Integer>();
			for (Point p : mp.getNodes()) {
				string.append("node" + nodeId + "[pos=\"" + p.x / 3 + "," + p.y / 3 + "\", label=\"" + p
						+ "\", pin=true]\n");
				idMap.put(p, nodeId);
				nodeId++;
			}

			for (Entry<Point, Point> entry : mp.getConnections()) {

				String label = "" + Math.round(mp.connectionLength(entry.getKey(), entry.getValue()) * 10d) / 10d;
				if (!idMap.containsKey(entry.getValue())) {
					Point p = entry.getValue();
					string.append("node" + nodeId + "[pos=\"" + p.x / 3 + "," + p.y / 3 + "\", label=\"" + p
							+ "\", pin=true]\n");
					idMap.put(p, nodeId);
					nodeId++;
				}
				string.append("node" + idMap.get(entry.getKey()) + " -> node" + idMap.get(entry.getValue())
						+ "[label=\"" + label + "\"]\n");

			}

			string.append("}");
			out.append(string);
			out.close();

			if (pdf) {
				dotToPDF(fileName + ".dot", fileName + ".pdf");
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// copied from rinde graduation
	protected static final boolean isDotAvailable = checkCommandAvailability("/usr/local/bin/dot");

	public static boolean dotToPDF(final String dotFile, final String pdfFile) {
		if (isDotAvailable) {
			try {
				// Execute a command with an argument that contains a space
				// "-Kneato", // -Kfdp
				final String[] commands = new String[] { "/usr/local/bin/dot", "-o", pdfFile, "-Tpdf", dotFile };
				// System.out.println(Arrays.toString(commands).replace(",",
				// ""));

				final Process p = Runtime.getRuntime().exec(commands);

				// final BufferedReader stdInput = new BufferedReader(new
				// InputStreamReader(p.getInputStream()));

				final BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

				String s;
				boolean flag = false;
				// read any errors from the attempted command
				while ((s = stdError.readLine()) != null) {
					System.err.println(s);
					flag = true;
				}
				if (flag) {
					throw new RuntimeException("Error while converting \"" + dotFile + "\" to \"" + pdfFile + "\"");
				}
				return true;
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public static Graph parseDot(String file) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			TableGraph graph = new TableGraph();
			boolean containsDistances = false;

			HashMap<String, Point> nodeMapping = new HashMap<String, Point>();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains("pos=")) {
					String nodeName = line.substring(0, line.indexOf("[")).trim();
					String[] position = line.split("\"")[1].split(",");
					Point p = new Point(Double.parseDouble(position[0]), Double.parseDouble(position[1]));
					nodeMapping.put(nodeName, p);
				} else if (line.contains("->")) {
					// example:

					// node1004 -> node820[label="163.3"]
					String[] names = line.split("->");
					String fromStr = names[0].trim();
					String toStr = names[1].substring(0, names[1].indexOf("[")).trim();
					double distance = Double.parseDouble(line.split("\"")[1]);
					Point from = nodeMapping.get(fromStr);
					Point to = nodeMapping.get(toStr);
					// if (!from.equals(to)) {
					if (Point.distance(from, to) == distance) {
						graph.addConnection(from, to);
					} else {
						graph.addConnection(from, to, distance);
						containsDistances = true;
					}
					// }
				}
			}
			// if (containsDistances) {
			// return graph;
			// } else {
			Graph g = new MultimapGraph();
			g.merge(graph);
			return g;
			// }
		} catch (Exception e) {
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
