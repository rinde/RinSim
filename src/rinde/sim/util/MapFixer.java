/**
 * 
 */
package rinde.sim.util;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rinde.sim.core.PathFinder;
import rinde.sim.core.PathNotFoundException;
import rinde.sim.core.Point;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class MapFixer {

	public static Multimap<Point, Point> hack(Multimap<Point, Point> graph) {
		Multimap<Point, Point> newGraph = HashMultimap.create();
		newGraph.putAll(graph);

		HashSet<Point> connected = new HashSet<Point>();
		HashSet<Point> neighbors = new HashSet<Point>();

		Point root = graph.keySet().iterator().next();
		connected.add(root);
		neighbors.addAll(graph.get(root));

		fixCluster(newGraph, connected, neighbors, new HashSet<Point>());
		return newGraph;
	}

	public static Multimap<Point, Point> connect2(Multimap<Point, Point> graph) {
		Multimap<Point, Point> newGraph = HashMultimap.create();
		newGraph.putAll(graph);

		HashSet<Point> connected = new HashSet<Point>();
		HashSet<Point> neighbors = new HashSet<Point>();

		Point root = graph.keySet().iterator().next();
		connected.add(root);
		neighbors.addAll(graph.get(root));

		fixCluster(newGraph, connected, neighbors, new HashSet<Point>());

		Set<Point> unconnected;
		while (!(unconnected = Sets.difference(newGraph.keySet(), connected)).isEmpty()) {
			Point p = unconnected.iterator().next();
			System.out.println("unconnected: " + unconnected.size());
			HashSet<Point> cluster = new HashSet<Point>(asList(p));
			fixCluster(newGraph, cluster, new HashSet<Point>(newGraph.get(p)), connected);
			//			System.out.println("cluster: " + cluster);
			Tuple<Point, Point> pair = findClosestPair(cluster, connected);

			if (!isConnected(newGraph, cluster, connected)) {
				//				System.out.println("not connection from cluster -> main");
				newGraph.put(pair.getKey(), pair.getValue());
				newGraph.put(pair.getValue(), pair.getKey());
			}

			connected.addAll(cluster);
		}

		//			newGraph.put(findClosest(p, connected), p);
		//			connected.add(p);
		//		}

		//
		// 		if( neighbour n isConnectedWith(n, connectedSet) )
		// 			add to connected set
		// 		else
		// 			create connection from n to one of the connectedSet (maybe make one of them bidirectional)
		// endwhile

		// traverse all unvisited nodes, create connection between each unvisited node and one of the connected nodes.

		return newGraph;
	}

	private static void fixCluster(Multimap<Point, Point> newGraph, HashSet<Point> connected, HashSet<Point> neighbors, HashSet<Point> otherClusters) {
		//		System.out.println(">> fixCluster");
		while (!neighbors.isEmpty()) {
			Point n = neighbors.iterator().next();
			//			System.out.println(n);
			neighbors.remove(n);
			// if this point is also in a other cluster, we don't have to check its neighbors
			if (!otherClusters.contains(n)) {
				for (Point b : newGraph.get(n)) {
					if (!connected.contains(b) && !neighbors.contains(b)) {
						neighbors.add(b);
					}
				}
			}
			if (!isConnectedWith(newGraph, n, connected)) {
				newGraph.put(n, findClosest(n, connected));// connect it
			}
			connected.add(n);
		}
	}

	/**
	 * @param n
	 * @param set
	 */
	private static Point findClosest(Point n, Set<Point> set) {
		double minDist = Double.POSITIVE_INFINITY;
		Point closest = null;
		for (Point p : set) {
			double dist = Point.distance(p, n);
			if (dist < minDist) {
				minDist = dist;
				closest = p;
			}
		}
		return closest;
	}

	private static Tuple<Point, Point> findClosestPair(Set<Point> set1, Set<Point> set2) {
		double minDist = Double.POSITIVE_INFINITY;
		Tuple<Point, Point> closestPair = null;
		for (Point p : set1) {
			Point c = findClosest(p, set2);
			double dist = Point.distance(p, c);
			if (dist < minDist) {
				minDist = dist;
				closestPair = new Tuple<Point, Point>(p, c);
			}
		}
		return closestPair;
	}

	private static boolean isConnected(Multimap<Point, Point> graph, Set<Point> set1, Set<Point> set2) {
		HashSet<Point> visited = new HashSet<Point>();
		HashSet<Point> queue = new HashSet<Point>();
		queue.addAll(set1);
		while (!queue.isEmpty()) {
			Point b = queue.iterator().next();
			queue.remove(b);
			Collection<Point> neighbours = graph.get(b);
			for (Point n : neighbours) {
				if (set2.contains(n)) {
					return true;
				}
				if (!visited.contains(n)) {
					queue.add(n);
				}
			}
			visited.add(b);
		}
		return false;
	}

	/**
	 * checks if it is possible to reach a point in <code>set</code> by
	 * following the outgoing arcs in point <code>p</code>.
	 * @param graph
	 * @param p
	 * @param set
	 * @return
	 */
	private static boolean isConnectedWith(Multimap<Point, Point> graph, Point p, Set<Point> set) {
		return isConnected(graph, new HashSet<Point>(asList(p)), set);
	}

	public static Multimap<Point, Point> connect(Multimap<Point, Point> graph) {
		Multimap<Point, Point> currentGraph = HashMultimap.create(graph);

		List<Set<Point>> result = findNotFullyConnectedNodes(currentGraph);

		boolean isFullyConnected = false;
		while (!isFullyConnected) {

			Set<Point> unconnected = result.get(0);
			Set<Point> connected = result.get(1);

			double minDist = Double.POSITIVE_INFINITY;
			Tuple<Point, Point> connection = null;
			for (Point u : unconnected) {
				for (Point c : connected) {
					double dist = Point.distance(u, c);
					if (dist < minDist) {
						minDist = dist;
						connection = new Tuple<Point, Point>(u, c);
					}
				}
			}

			currentGraph.put(connection.getKey(), connection.getValue());
			currentGraph.put(connection.getValue(), connection.getKey());
			result = findNotFullyConnectedNodes(currentGraph);
			if (result.get(0).isEmpty()) {
				isFullyConnected = true;
			}
		}

		return currentGraph;
	}

	public static List<Set<Point>> findNotFullyConnectedNodes(Multimap<Point, Point> graph) {
		// just get a 'random' starting point
		return findNotFullyConnectedNodes(graph, new ArrayList<Point>(graph.keySet()).get(0));
	}

	public static List<Set<Point>> findNotFullyConnectedNodes(Multimap<Point, Point> graph, Point root) {

		HashSet<Point> fullyConnectedSet = new HashSet<Point>();
		HashSet<Point> neighbours = new HashSet<Point>();
		HashSet<Point> notConnectedSet = new HashSet<Point>();

		fullyConnectedSet.add(root);
		neighbours.addAll(graph.get(root));

		while (!neighbours.isEmpty()) {
			List<Point> path = null;
			Point current = neighbours.iterator().next();
			neighbours.remove(current);
			if (graph.containsKey(current)) {
				try {
					path = PathFinder.shortestDistance(graph, current, root);
				} catch (PathNotFoundException e) {
				}
			}

			if (path == null) {
				notConnectedSet.add(current);
			} else {
				for (Point p : path) {
					fullyConnectedSet.add(p);
					if (neighbours.contains(p)) {
						neighbours.remove(p);
					}
					for (Point q : graph.get(p)) {
						if (!fullyConnectedSet.contains(q)) {
							neighbours.add(q);
						}
					}
				}
			}
		}
		for (Point p : graph.keySet()) {
			if (!fullyConnectedSet.contains(p)) {
				notConnectedSet.add(p);
			}
		}

		return new ArrayList<Set<Point>>(asList(notConnectedSet, fullyConnectedSet));
	}

	public static void main(String[] args) {
		Multimap<Point, Point> graph;

		String name = "leuven";

		//graph = OSM.parse("/Users/rindevanlon/Downloads/belgium.osm");
		//graph = OSM.parse("/Users/rindevanlon/Downloads/corse.osm");
		//graph = OSM.parse("/Users/rindevanlon/Downloads/enfield.osm");
		//graph = OSM.parse("/Users/rindevanlon/Downloads/luxembourg.osm");
		//graph = OSM.parse("/Users/rindevanlon/Downloads/andorra.osm");
		//graph = OSM.parse("/Users/rindevanlon/Downloads/liechtenstein.osm");
		//graph = OSM.parse("/Users/rindevanlon/Downloads/berlin.osm");
		// graph = OSM.parse("/Users/rindevanlon/Downloads/netherlands.osm.highway");

		//graph = OSM.parse("/Users/rindevanlon/Downloads/" + name + ".osm");
		graph = OSM.parse("../RinSim/files/maps/leuven.osm.xml");
		System.out.println("loaded map of " + name);
		graph = MapFixer.connect2(graph);
		//graph = MapFixer.hack(graph);
		System.out.println("fixed map of " + name);
		DotUtils.saveToDot(graph, "files/maps/dot/" + name);
		System.out.println("converted map of " + name + " to .dot");

		//		(1098696.6105863547,1.334706587029543E7) from (1099936.0,1.3346904333333334E7)

		Point p1 = new Point(1098696.6105863547, 1.334706587029543E7);
		Point p2 = new Point(1099936.0, 1.3346904333333334E7);
		PathFinder.shortestDistance(graph, p2, p1);

	}
}
