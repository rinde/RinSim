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

import rinde.sim.core.graph.Connection;
import rinde.sim.core.graph.EdgeData;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.LengthEdgeData;
import rinde.sim.core.graph.MultiAttributeEdgeData;
import rinde.sim.core.graph.MultimapGraph;
import rinde.sim.core.graph.PathNotFoundException;
import rinde.sim.core.graph.Point;
import rinde.sim.core.graph.TableGraph;

import com.google.common.collect.Sets;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class MapPreprocessor {

	private static <E extends EdgeData> Graph<E> hack(Graph<E> graph) {
		Graph<E> newGraph = new MultimapGraph<E>();
		newGraph.merge(graph);

		HashSet<Point> connected = new HashSet<Point>();
		HashSet<Point> neighbors = new HashSet<Point>();

		Point root = graph.getNodes().iterator().next();
		connected.add(root);
		neighbors.addAll(graph.getOutgoingConnections(root));

		fixCluster(newGraph, connected, neighbors, new HashSet<Point>());
		return newGraph;
	}

	public static <E extends EdgeData> Graph<E> connect2(Graph<E> graph) {
		Graph<E> newGraph = new MultimapGraph<E>();
		newGraph.merge(graph);

		HashSet<Point> connected = new HashSet<Point>();
		HashSet<Point> neighbors = new HashSet<Point>();

		Point root = graph.getNodes().iterator().next();
		connected.add(root);
		neighbors.addAll(graph.getOutgoingConnections(root));
		fixCluster(newGraph, connected, neighbors, new HashSet<Point>());

		Set<Point> unconnected;
		while (!(unconnected = Sets.difference(newGraph.getNodes(), connected)).isEmpty()) {
			Point p = unconnected.iterator().next();
			System.out.println("unconnected: " + unconnected.size());
			HashSet<Point> cluster = new HashSet<Point>(asList(p));
			fixCluster(newGraph, cluster, new HashSet<Point>(newGraph.getOutgoingConnections(p)), connected);
			//			System.out.println("cluster: " + cluster);
			Tuple<Point, Point> pair = findClosestPair(cluster, connected);

			if (!isConnected(newGraph, cluster, connected)) {
				//				System.out.println("not connection from cluster -> main");
				newGraph.addConnection(pair.getKey(), pair.getValue());
				newGraph.addConnection(pair.getValue(), pair.getKey());
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

	private static <E extends EdgeData> void fixCluster(Graph<E> newGraph, HashSet<Point> connected, HashSet<Point> neighbors, HashSet<Point> otherClusters) {
		//		System.out.println(">> fixCluster");
		while (!neighbors.isEmpty()) {
			Point n = neighbors.iterator().next();
			assert n != null;
			//			System.out.println(n);
			neighbors.remove(n);
			// if this point is also in a other cluster, we don't have to check its neighbors
			if (!otherClusters.contains(n)) {
				for (Point b : newGraph.getOutgoingConnections(n)) {
					if (b != null && !connected.contains(b) && !neighbors.contains(b)) {

						neighbors.add(b);
					}
				}
			}
			if (!isConnectedWith(newGraph, n, connected)) {
				assert n != null;
				assert !connected.isEmpty();
				assert !newGraph.isEmpty();
				newGraph.addConnection(n, findClosest(n, connected));// connect it
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
			assert p != null;
			double dist = Point.distance(p, n);
			if (dist < minDist) {
				minDist = dist;
				closest = p;
			}
		}
		assert closest != null;
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

	private static <E extends EdgeData> boolean isConnected(Graph<E> graph, Set<Point> set1, Set<Point> set2) {
		HashSet<Point> visited = new HashSet<Point>();
		HashSet<Point> queue = new HashSet<Point>();
		queue.addAll(set1);
		while (!queue.isEmpty()) {
			Point b = queue.iterator().next();
			queue.remove(b);
			Collection<Point> neighbours = graph.getOutgoingConnections(b);
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
	private static boolean isConnectedWith(Graph graph, Point p, Set<Point> set) {
		return isConnected(graph, new HashSet<Point>(asList(p)), set);
	}

	public static <E extends EdgeData> Graph<E> removeUnconnectedSubGraphs(Graph<E> graph, E empty) {
		Graph<E> currentGraph = new TableGraph<E>(empty);
		currentGraph.merge(graph);

		List<Set<Point>> result = findNotFullyConnectedNodes(currentGraph);
		int totalSize = 0;
		int biggestIndex = -1;
		int biggestSize = -1;
		for (int i = 0; i < result.size(); i++) {
			totalSize += result.get(i).size();
			if (result.get(i).size() > biggestSize) {
				biggestSize = result.get(i).size();
				biggestIndex = i;
			}
		}

		for (int i = 0; i < result.size(); i++) {
			if (i != biggestIndex) {
				System.out.println("removing: " + i + " " + result.size());
				for (Point p : result.get(i)) {
					currentGraph.removeNode(p);
				}
			}
		}

		System.out.println("Removed " + (result.size() - 1) + " subgraphs, with total size " + (totalSize - currentGraph.getNumberOfNodes()) + " nodes, resulting graph has: "
				+ currentGraph.getNumberOfNodes() + " nodes.");
		System.out.println(totalSize);
		System.out.println(currentGraph.getNumberOfNodes());

		return currentGraph;
	}

	public static <E extends EdgeData> Graph<E> connect(Graph<E> graph) {
		Graph<E> currentGraph = new MultimapGraph<E>();
		currentGraph.merge(graph);

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

			currentGraph.addConnection(connection.getKey(), connection.getValue());
			currentGraph.addConnection(connection.getValue(), connection.getKey());
			result = findNotFullyConnectedNodes(currentGraph);
			if (result.get(0).isEmpty()) {
				isFullyConnected = true;
			}
		}

		return currentGraph;
	}

	private static <E extends EdgeData> Set<Point> unseenNeighbours(Graph<E> g, Set<Point> seen, Set<Point> current) {
		HashSet<Point> set = new HashSet<Point>();
		for (Point p : current) {
			set.addAll(g.getIncomingConnections(p));
			set.addAll(g.getOutgoingConnections(p));
		}
		set.removeAll(seen);
		return set;
	}

	private static <E extends EdgeData> boolean isConnected(Graph<E> g, Point from, Point to, Set<Point> subgraph) {

		Point cur = from;
		Set<Point> seen = new HashSet<Point>();

		while (true) {
			seen.add(cur);
			Set<Point> ns = new HashSet<Point>(g.getOutgoingConnections(cur));

			ns.retainAll(subgraph);
			ns.removeAll(seen);

			if (ns.contains(to)) {
				return true;
			} else if (ns.isEmpty()) {
				return false;
			} else if (ns.size() == 1) {
				cur = ns.iterator().next();
			} else {
				throw new RuntimeException("not expected..");
			}

		}

	}

	public static <E extends EdgeData> double getLength(Graph<E> g, Point from, Point to) {
		E connectionData = g.connectionData(from, to);
		if (connectionData == null || Double.isNaN(connectionData.getLength())) {
			return Point.distance(from, to);
		} else {
			return connectionData.getLength();
		}
	}

	public static <E extends EdgeData> Graph<E> simplify(Graph<E> g, E empty) {
		TableGraph<E> newGraph = new TableGraph<E>(empty);
		newGraph.merge(g);
		boolean working = true;

		//int iterations = 0;
		while (working) {
			//System.out.println("starting iteration: " + iterations);
			//iterations++;
			boolean edit = false;
			//			System.out.println(newGraph.getConnections());

			HashSet<Connection<E>> connections = new HashSet<Connection<E>>(newGraph.getConnections());
			HashSet<Connection<E>> removeList = new HashSet<Connection<E>>();
			for (Connection<E> connection : connections) {
				if (removeList.contains(connection)) {
					continue;
				}
				Point left = connection.from;
				Point right = connection.to;

				ContractType type = isContractable(newGraph, left, right);
				//				System.out.println(type + " " + left + " " + right);
				if (type == ContractType.NO) {
					continue;
				} else {
					//					double length = getLength(newGraph, left, right);

					E removeEdgeData = newGraph.connectionData(left, right);
					double removeLength = newGraph.connectionLength(left, right);
					removeList.add(newGraph.getConnection(left, right));
					newGraph.removeConnection(left, right);

					Point removeNode = (type == ContractType.RIGHT) ? right : left;
					Point mergeNode = (type == ContractType.RIGHT) ? left : right;

					//					System.out.println("remove: " + removeNode);
					//					System.out.println("merge into: " + mergeNode);
					for (Point outgoing : newGraph.getOutgoingConnections(removeNode)) {
						if (!outgoing.equals(mergeNode)) {
							E edgeData = newGraph.connectionData(removeNode, outgoing);
							double edgeLength = newGraph.connectionLength(removeNode, outgoing);
							//double newLength = length + getLength(newGraph, removeNode, outgoing);

							if (!newGraph.hasConnection(mergeNode, outgoing)) {
								newGraph.addConnection(mergeNode, outgoing, mergeEdgeData(empty, removeEdgeData, removeLength, edgeData, edgeLength));
							}
							//							if (clazz.equals(LengthEdgeData.class)) {
							//								newGraph.addConnection(mergeNode, outgoing, (E) new LengthEdgeData(newLength));
							//							} else if (clazz.equals(MultiAttributeEdgeData.class)) {
							//								throw new UnsupportedOperationException();
							// TODO Merge the MultiAttributeEdgeData object here!
							//								MultiAttributeEdgeData maed = (MultiAttributeEdgeData) newGraph.connectionData(removeNode, outgoing);
							//
							//								if(!Double.isNaN(maed.getMaxSpeed())){
							//									
							//								}
							//								newGraph.addConnection(mergeNode, outgoing, (E) new MultiAttributeEdgeData(newLength));
							//							} else {
							//								throw new UnsupportedOperationException();
							//							}
						}
					}
					for (Point incoming : newGraph.getIncomingConnections(removeNode)) {
						if (!incoming.equals(mergeNode)) {
							E edgeData = newGraph.connectionData(incoming, removeNode);
							double edgeLength = newGraph.connectionLength(incoming, removeNode);

							//							double newLength = length + getLength(newGraph, incoming, removeNode);
							if (!newGraph.hasConnection(incoming, mergeNode)) {
								newGraph.addConnection(incoming, mergeNode, mergeEdgeData(empty, edgeData, edgeLength, removeEdgeData, removeLength));
							}
							//							if (clazz.equals(LengthEdgeData.class)) {
							//								
							//							} else {
							//								throw new UnsupportedOperationException();
							//							}

							//							newGraph.addConnection(incoming, mergeNode, new LengthEdgeData(newLength));
						}
					}

					Collection<Point> in = newGraph.getIncomingConnections(removeNode);
					for (Point p : in) {
						removeList.add(newGraph.getConnection(p, removeNode));
					}
					Collection<Point> out = newGraph.getOutgoingConnections(removeNode);
					for (Point p : out) {
						removeList.add(newGraph.getConnection(removeNode, p));
					}

					newGraph.removeNode(removeNode);
					edit = true;
					//break;
				}
			}
			if (!edit) {
				working = false;
			}
		}
		return newGraph;
	}

	// TODO also check if input values are valid!!
	// TODO do something with maxSpeed!!
	@SuppressWarnings("unchecked")
	static <E extends EdgeData> E mergeEdgeData(E empty, E e1, double l1, E e2, double l2) {
		if (empty instanceof LengthEdgeData) {
			return (E) new LengthEdgeData(l1 + l2);
		} else if (empty instanceof MultiAttributeEdgeData) {
			return (E) new MultiAttributeEdgeData(l1 + l2);
		}
		throw new IllegalArgumentException("EdgeData objects are of unknown type");
	}

	enum ContractType {
		BOTH, LEFT, RIGHT, NO
	}

	//TODO fix this method to also take the EdgeData into account 
	static ContractType isContractable(Graph<? extends EdgeData> g, Point node1, Point node2) {
		boolean n12 = g.getOutgoingConnections(node1).contains(node2);
		boolean n21 = g.getOutgoingConnections(node2).contains(node1);

		if (!(n12 || n21)) {
			throw new IllegalArgumentException("There is no connection between the nodes.");
		}
		boolean bidi1 = n12 && n21;
		boolean bidi0 = false, bidi2 = false;

		Set<Point> outgoing1 = new HashSet<Point>(g.getOutgoingConnections(node1));
		Set<Point> incoming1 = new HashSet<Point>(g.getIncomingConnections(node1));
		outgoing1.remove(node2);
		incoming1.remove(node2);

		Set<Point> outgoing2 = new HashSet<Point>(g.getOutgoingConnections(node2));
		Set<Point> incoming2 = new HashSet<Point>(g.getIncomingConnections(node2));
		outgoing2.remove(node1);
		incoming2.remove(node1);

		Set<Point> neighbors1 = new HashSet<Point>();
		neighbors1.addAll(outgoing1);
		neighbors1.addAll(incoming1);

		if (neighbors1.size() == 1) {
			Point node0 = neighbors1.iterator().next();
			bidi0 = outgoing1.contains(node0) && incoming1.contains(node0);
		}

		Set<Point> neighbors2 = new HashSet<Point>();
		neighbors2.addAll(outgoing2);
		neighbors2.addAll(incoming2);

		if (neighbors2.size() == 1) {
			Point node3 = neighbors2.iterator().next();
			bidi2 = outgoing2.contains(node3) && incoming2.contains(node3);
		}

		if (neighbors1.size() != 1 && neighbors2.size() != 1) {
			return ContractType.NO;
		} else if (neighbors1.size() == 1 && neighbors2.size() == 1) {
			boolean sameneigh = neighbors1.iterator().next().equals(neighbors2.iterator().next());

			if (sameneigh) {
				if (!bidi0 && !bidi1 && !bidi2) {
					return ContractType.BOTH;
				}
				return ContractType.NO;
			}

			if ((bidi0 == bidi1) && (bidi1 == bidi2)) {
				return ContractType.BOTH;
			} else if ((bidi0 == bidi1) && (bidi1 != bidi2)) {
				return ContractType.LEFT;
			} else if ((bidi0 != bidi1) && (bidi1 == bidi2)) {
				return ContractType.RIGHT;
			} else {
				return ContractType.NO;
			}
		} else if (neighbors1.size() == 1 && neighbors2.size() != 1) {
			return bidi0 == bidi1 ? ContractType.LEFT : ContractType.NO;
		} else if (neighbors1.size() != 1 && neighbors2.size() == 1) {
			return bidi1 == bidi2 ? ContractType.RIGHT : ContractType.NO;
		}

		throw new IllegalStateException("Unexpected node configuration..");
	}

	public static <E extends EdgeData> List<Set<Point>> findNotFullyConnectedNodes(Graph<E> graph) {
		if (graph == null || graph.isEmpty()) {
			throw new IllegalArgumentException("Graph may not be null and must contain at least one node.");
		}
		// just get a 'random' starting point
		return findNotFullyConnectedNodes(graph, new ArrayList<Point>(graph.getNodes()).get(0));
	}

	public static <E extends EdgeData> List<Set<Point>> findNotFullyConnectedNodes(Graph<E> graph, Point root) {

		HashSet<Point> fullyConnectedSet = new HashSet<Point>();
		HashSet<Point> neighbours = new HashSet<Point>();
		HashSet<Point> notConnectedSet = new HashSet<Point>();

		fullyConnectedSet.add(root);
		neighbours.addAll(graph.getOutgoingConnections(root));

		while (!neighbours.isEmpty()) {
			List<Point> path = null;
			Point current = neighbours.iterator().next();
			neighbours.remove(current);
			if (graph.containsNode(current)) {
				try {
					path = Graphs.shortestPathEuclidianDistance(graph, current, root);
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
					for (Point q : graph.getOutgoingConnections(p)) {
						if (!fullyConnectedSet.contains(q)) {
							neighbours.add(q);
						}
					}
				}
			}
		}
		for (Point p : graph.getNodes()) {
			if (!fullyConnectedSet.contains(p)) {
				notConnectedSet.add(p);
			}
		}

		return new ArrayList<Set<Point>>(asList(notConnectedSet, fullyConnectedSet));
	}

	public static void main(String[] args) {
		//		Graph graph = new MultimapGraph();
		//
		//		String name = "brussels";

		//graph = OSM.parse("/Users/rindevanlon/Downloads/belgium.osm");
		//graph = OSM.parse("/Users/rindevanlon/Downloads/corse.osm");
		//graph = OSM.parse("/Users/rindevanlon/Downloads/enfield.osm");
		//graph = OSM.parse("/Users/rindevanlon/Downloads/luxembourg.osm");
		//graph = OSM.parse("/Users/rindevanlon/Downloads/andorra.osm");
		//graph = OSM.parse("/Users/rindevanlon/Downloads/liechtenstein.osm");
		//graph = OSM.parse("/Users/rindevanlon/Downloads/berlin.osm");
		// graph = OSM.parse("/Users/rindevanlon/Downloads/netherlands.osm.highway");

		//graph = OSM.parse("/Users/rindevanlon/Downloads/" + name + ".osm");
		//		graph.addConnections(OSM.parse("../RinSim/files/maps/brussels.osm").entries());
		//		System.out.println("loaded map of " + name);
		//		graph = MapFixer.connect2(graph);
		//		//graph = MapFixer.hack(graph);
		//		System.out.println("fixed map of " + name);
		//		DotUtils.saveToDot(graph, "files/maps/dot/" + name);
		//		System.out.println("converted map of " + name + " to .dot");

		//		(1098696.6105863547,1.334706587029543E7) from (1099936.0,1.3346904333333334E7)

		//		Point p1 = new Point(1098696.6105863547, 1.334706587029543E7);
		//		Point p2 = new Point(1099936.0, 1.3346904333333334E7);
		//		PathFinder.shortestDistance(graph, p2, p1);

		//		Graph graph = new TableGraph();
		//		graph.addConnections(OSM.parse("/Users/rindevanlon/Downloads/leuven-centrum-small.osm").entries());
		//		graph = MapFixer.connect2(graph);
		//
		//		DotUtils.saveToDot(graph, "files/maps/dot/leuven-centrum");

		//Graph g = DotUtils.parseDot("files/maps/dot/leuven.dot");
		//Graph simple = simplify(g);
		//DotUtils.saveToDot(simple, "files/maps/dot/leuven-simple", false);

	}
}
