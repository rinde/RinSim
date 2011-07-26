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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class DotUtils {

	public static void saveToDot(Graph mp, String fileName) {

		try {
			FileWriter fileWriter = new FileWriter(fileName + ".dot");

			final BufferedWriter out = new BufferedWriter(fileWriter);

			final StringBuilder string = new StringBuilder();
			string.append("digraph genegraph {\n");

			int nodeId = 0;
			HashMap<Point, Integer> idMap = new HashMap<Point, Integer>();
			for (Point p : mp.getNodes()) {
				string.append("node" + nodeId + "[pos=\"" + p.x / 3 + "," + p.y / 3 + "\", label=\"" + p + "\", pin=true]\n");
				idMap.put(p, nodeId);
				nodeId++;
			}

			for (Entry<Point, Point> entry : mp.getConnections()) {

				String label = "" + Math.round(Point.distance(entry.getKey(), entry.getValue()) * 10d) / 10d;
				if (!idMap.containsKey(entry.getValue())) {
					Point p = entry.getValue();
					string.append("node" + nodeId + "[pos=\"" + p.x / 3 + "," + p.y / 3 + "\", label=\"" + p + "\", pin=true]\n");
					idMap.put(p, nodeId);
					nodeId++;
				}
				string.append("node" + idMap.get(entry.getKey()) + " -> node" + idMap.get(entry.getValue()) + "[label=\"" + label + "\"]\n");
			}

			string.append("}");
			out.append(string);
			out.close();

			dotToPDF(fileName + ".dot", fileName + ".pdf");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// copied from rinde graduation
	protected static final boolean isDotAvailable = checkCommandAvailability("dot");

	private static boolean dotToPDF(final String dotFile, final String pdfFile) {
		if (isDotAvailable) {
			try {
				// Execute a command with an argument that contains a space
				final String[] commands = new String[] { "dot", "-Kfdp", "-o", pdfFile, "-Tpdf", dotFile };
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

			Multimap<Point, Point> graph = HashMultimap.create();
			HashMap<String, Point> nodeMapping = new HashMap<String, Point>();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains("pos=")) {
					String nodeName = line.substring(0, line.indexOf("[")).trim();
					String[] position = line.split("\"")[1].split(",");
					Point p = new Point(Double.parseDouble(position[0]), Double.parseDouble(position[1]));
					nodeMapping.put(nodeName, p);
				} else if (line.contains("->")) {
					String[] names = line.split("->");
					String from = names[0].trim();
					String to = names[1].substring(0, names[1].indexOf("[")).trim();
					graph.put(nodeMapping.get(from), nodeMapping.get(to));
				}
			}

			return new MultimapGraph(graph);
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
