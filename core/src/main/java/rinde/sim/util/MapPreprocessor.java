/**
 * 
 */
package rinde.sim.util;

import static java.util.Arrays.asList;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rinde.sim.core.graph.Connection;
import rinde.sim.core.graph.ConnectionData;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.LengthData;
import rinde.sim.core.graph.MultiAttributeData;
import rinde.sim.core.graph.MultimapGraph;
import rinde.sim.core.graph.PathNotFoundException;
import rinde.sim.core.graph.Point;
import rinde.sim.core.graph.TableGraph;
import rinde.sim.serializers.DotGraphSerializer;

import com.google.common.collect.Sets;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
@Deprecated
public class MapPreprocessor {

  private static <E extends ConnectionData> Graph<E> hack(Graph<E> graph) {
    final Graph<E> newGraph = new MultimapGraph<E>();
    newGraph.merge(graph);

    final HashSet<Point> connected = new HashSet<Point>();
    final HashSet<Point> neighbors = new HashSet<Point>();

    final Point root = graph.getNodes().iterator().next();
    connected.add(root);
    neighbors.addAll(graph.getOutgoingConnections(root));

    fixCluster(newGraph, connected, neighbors, new HashSet<Point>());
    return newGraph;
  }

  public static <E extends ConnectionData> Graph<E> connect2(Graph<E> graph) {
    final Graph<E> newGraph = new MultimapGraph<E>();
    newGraph.merge(graph);

    final HashSet<Point> connected = new HashSet<Point>();
    final HashSet<Point> neighbors = new HashSet<Point>();

    final Point root = graph.getNodes().iterator().next();
    connected.add(root);
    neighbors.addAll(graph.getOutgoingConnections(root));
    fixCluster(newGraph, connected, neighbors, new HashSet<Point>());

    Set<Point> unconnected;
    while (!(unconnected = Sets.difference(newGraph.getNodes(), connected))
        .isEmpty()) {
      final Point p = unconnected.iterator().next();
      System.out.println("unconnected: " + unconnected.size());
      final HashSet<Point> cluster = new HashSet<Point>(asList(p));
      fixCluster(newGraph, cluster,
          new HashSet<Point>(newGraph.getOutgoingConnections(p)), connected);
      // System.out.println("cluster: " + cluster);
      final Tuple<Point, Point> pair = findClosestPair(cluster, connected);

      if (!isConnected(newGraph, cluster, connected)) {
        // System.out.println("not connection from cluster -> main");
        newGraph.addConnection(pair.getKey(), pair.getValue());
        newGraph.addConnection(pair.getValue(), pair.getKey());
      }

      connected.addAll(cluster);
    }

    // newGraph.put(findClosest(p, connected), p);
    // connected.add(p);
    // }

    //
    // if( neighbour n isConnectedWith(n, connectedSet) )
    // add to connected set
    // else
    // create connection from n to one of the connectedSet (maybe make one
    // of them bidirectional)
    // endwhile

    // traverse all unvisited nodes, create connection between each
    // unvisited node and one of the connected nodes.

    return newGraph;
  }

  private static <E extends ConnectionData> void fixCluster(Graph<E> newGraph,
      HashSet<Point> connected, HashSet<Point> neighbors,
      HashSet<Point> otherClusters) {
    // System.out.println(">> fixCluster");
    while (!neighbors.isEmpty()) {
      final Point n = neighbors.iterator().next();
      assert n != null;
      // System.out.println(n);
      neighbors.remove(n);
      // if this point is also in a other cluster, we don't have to check
      // its neighbors
      if (!otherClusters.contains(n)) {
        for (final Point b : newGraph.getOutgoingConnections(n)) {
          if (b != null && !connected.contains(b) && !neighbors.contains(b)) {

            neighbors.add(b);
          }
        }
      }
      if (!isConnectedWith(newGraph, n, connected)) {
        assert n != null;
        assert !connected.isEmpty();
        assert !newGraph.isEmpty();
        newGraph.addConnection(n, findClosest(n, connected));// connect
                                                             // it
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
    for (final Point p : set) {
      assert p != null;
      final double dist = Point.distance(p, n);
      if (dist < minDist) {
        minDist = dist;
        closest = p;
      }
    }
    assert closest != null;
    return closest;
  }

  private static Tuple<Point, Point> findClosestPair(Set<Point> set1,
      Set<Point> set2) {
    double minDist = Double.POSITIVE_INFINITY;
    Tuple<Point, Point> closestPair = null;
    for (final Point p : set1) {
      final Point c = findClosest(p, set2);
      final double dist = Point.distance(p, c);
      if (dist < minDist) {
        minDist = dist;
        closestPair = new Tuple<Point, Point>(p, c);
      }
    }
    return closestPair;
  }

  private static <E extends ConnectionData> boolean isConnected(Graph<E> graph,
      Set<Point> set1, Set<Point> set2) {
    final HashSet<Point> visited = new HashSet<Point>();
    final HashSet<Point> queue = new HashSet<Point>();
    queue.addAll(set1);
    while (!queue.isEmpty()) {
      final Point b = queue.iterator().next();
      queue.remove(b);
      final Collection<Point> neighbours = graph.getOutgoingConnections(b);
      for (final Point n : neighbours) {
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
   * checks if it is possible to reach a point in <code>set</code> by following
   * the outgoing arcs in point <code>p</code>.
   * @param graph
   * @param p
   * @param set
   * @return
   */
  private static boolean isConnectedWith(Graph graph, Point p, Set<Point> set) {
    return isConnected(graph, new HashSet<Point>(asList(p)), set);
  }

  public static <E extends ConnectionData> Graph<E> removeUnconnectedSubGraphs(
      Graph<E> graph, E empty) {
    final Graph<E> currentGraph = new TableGraph<E>(empty);
    currentGraph.merge(graph);

    final List<Set<Point>> result = findNotFullyConnectedNodes(currentGraph);
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
        for (final Point p : result.get(i)) {
          currentGraph.removeNode(p);
        }
      }
    }

    System.out.println("Removed " + (result.size() - 1)
        + " subgraphs, with total size "
        + (totalSize - currentGraph.getNumberOfNodes())
        + " nodes, resulting graph has: " + currentGraph.getNumberOfNodes()
        + " nodes.");
    System.out.println(totalSize);
    System.out.println(currentGraph.getNumberOfNodes());

    return currentGraph;
  }

  public static <E extends ConnectionData> Graph<E> connect(Graph<E> graph) {
    final Graph<E> currentGraph = new MultimapGraph<E>();
    currentGraph.merge(graph);

    List<Set<Point>> result = findNotFullyConnectedNodes(currentGraph);

    boolean isFullyConnected = false;
    while (!isFullyConnected) {

      final Set<Point> unconnected = result.get(0);
      final Set<Point> connected = result.get(1);

      double minDist = Double.POSITIVE_INFINITY;
      Tuple<Point, Point> connection = null;
      for (final Point u : unconnected) {
        for (final Point c : connected) {
          final double dist = Point.distance(u, c);
          if (dist < minDist) {
            minDist = dist;
            connection = new Tuple<Point, Point>(u, c);
          }
        }
      }

      if (!currentGraph.hasConnection(connection.getKey(),
          connection.getValue())) {
        currentGraph.addConnection(connection.getKey(), connection.getValue());
      }
      if (!currentGraph.hasConnection(connection.getValue(),
          connection.getKey())) {
        currentGraph.addConnection(connection.getValue(), connection.getKey());
      }
      result = findNotFullyConnectedNodes(currentGraph);
      if (result.get(0).isEmpty()) {
        isFullyConnected = true;
      }
    }

    return currentGraph;
  }

  private static <E extends ConnectionData> Set<Point> unseenNeighbours(
      Graph<E> g, Set<Point> seen, Set<Point> current) {
    final HashSet<Point> set = new HashSet<Point>();
    for (final Point p : current) {
      set.addAll(g.getIncomingConnections(p));
      set.addAll(g.getOutgoingConnections(p));
    }
    set.removeAll(seen);
    return set;
  }

  private static <E extends ConnectionData> boolean isConnected(Graph<E> g,
      Point from, Point to, Set<Point> subgraph) {

    Point cur = from;
    final Set<Point> seen = new HashSet<Point>();

    while (true) {
      seen.add(cur);
      final Set<Point> ns = new HashSet<Point>(g.getOutgoingConnections(cur));

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

  public static <E extends ConnectionData> double getLength(Graph<E> g,
      Point from, Point to) {
    final E connectionData = g.connectionData(from, to);
    if (connectionData == null || Double.isNaN(connectionData.getLength())) {
      return Point.distance(from, to);
    } else {
      return connectionData.getLength();
    }
  }

  public static <E extends ConnectionData> Graph<E> simplify(Graph<E> g, E empty) {
    final TableGraph<E> newGraph = new TableGraph<E>(empty);
    newGraph.merge(g);
    boolean working = true;

    // int iterations = 0;
    while (working) {
      // System.out.println("starting iteration: " + iterations);
      // iterations++;
      boolean edit = false;
      // System.out.println(newGraph.getConnections());

      final HashSet<Connection<E>> connections = new HashSet<Connection<E>>(
          newGraph.getConnections());
      final HashSet<Connection<E>> removeList = new HashSet<Connection<E>>();
      for (final Connection<E> connection : connections) {
        if (removeList.contains(connection)) {
          continue;
        }
        final Point left = connection.from;
        final Point right = connection.to;

        final ContractType type = isContractable(newGraph, left, right);
        // System.out.println(type + " " + left + " " + right);
        if (type == ContractType.NO) {
          continue;
        } else {
          // double length = getLength(newGraph, left, right);

          final E removeEdgeData = newGraph.connectionData(left, right);
          final double removeLength = newGraph.connectionLength(left, right);
          removeList.add(newGraph.getConnection(left, right));
          newGraph.removeConnection(left, right);

          final Point removeNode = (type == ContractType.RIGHT) ? right : left;
          final Point mergeNode = (type == ContractType.RIGHT) ? left : right;

          // System.out.println("remove: " + removeNode);
          // System.out.println("merge into: " + mergeNode);
          for (final Point outgoing : newGraph
              .getOutgoingConnections(removeNode)) {
            if (!outgoing.equals(mergeNode)) {
              final E edgeData = newGraph.connectionData(removeNode, outgoing);
              final double edgeLength = newGraph.connectionLength(removeNode,
                  outgoing);
              // double newLength = length + getLength(newGraph,
              // removeNode, outgoing);

              if (!newGraph.hasConnection(mergeNode, outgoing)) {
                newGraph.addConnection(
                    mergeNode,
                    outgoing,
                    mergeEdgeData(empty, removeEdgeData, removeLength,
                        edgeData, edgeLength));
              }
              // if (clazz.equals(LengthEdgeData.class)) {
              // newGraph.addConnection(mergeNode, outgoing, (E)
              // new LengthEdgeData(newLength));
              // } else if
              // (clazz.equals(MultiAttributeEdgeData.class)) {
              // throw new UnsupportedOperationException();
              // TODO Merge the MultiAttributeEdgeData object
              // here!
              // MultiAttributeEdgeData maed =
              // (MultiAttributeEdgeData)
              // newGraph.connectionData(removeNode, outgoing);
              //
              // if(!Double.isNaN(maed.getMaxSpeed())){
              //
              // }
              // newGraph.addConnection(mergeNode, outgoing, (E)
              // new MultiAttributeEdgeData(newLength));
              // } else {
              // throw new UnsupportedOperationException();
              // }
            }
          }
          for (final Point incoming : newGraph
              .getIncomingConnections(removeNode)) {
            if (!incoming.equals(mergeNode)) {
              final E edgeData = newGraph.connectionData(incoming, removeNode);
              final double edgeLength = newGraph.connectionLength(incoming,
                  removeNode);

              // double newLength = length + getLength(newGraph,
              // incoming, removeNode);
              if (!newGraph.hasConnection(incoming, mergeNode)) {
                newGraph.addConnection(
                    incoming,
                    mergeNode,
                    mergeEdgeData(empty, edgeData, edgeLength, removeEdgeData,
                        removeLength));
              }
              // if (clazz.equals(LengthEdgeData.class)) {
              //
              // } else {
              // throw new UnsupportedOperationException();
              // }

              // newGraph.addConnection(incoming, mergeNode, new
              // LengthEdgeData(newLength));
            }
          }

          final Collection<Point> in = newGraph
              .getIncomingConnections(removeNode);
          for (final Point p : in) {
            removeList.add(newGraph.getConnection(p, removeNode));
          }
          final Collection<Point> out = newGraph
              .getOutgoingConnections(removeNode);
          for (final Point p : out) {
            removeList.add(newGraph.getConnection(removeNode, p));
          }

          newGraph.removeNode(removeNode);
          edit = true;
          // break;
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
  static <E extends ConnectionData> E mergeEdgeData(E empty, E e1, double l1,
      E e2, double l2) {
    if (empty instanceof LengthData) {
      return (E) new LengthData(l1 + l2);
    } else if (empty instanceof MultiAttributeData) {
      return (E) new MultiAttributeData(l1 + l2);
    }
    throw new IllegalArgumentException("EdgeData objects are of unknown type");
  }

  enum ContractType {
    BOTH, LEFT, RIGHT, NO
  }

  // TODO fix this method to also take the EdgeData into account
  static ContractType isContractable(Graph<? extends ConnectionData> g,
      Point node1, Point node2) {
    final boolean n12 = g.getOutgoingConnections(node1).contains(node2);
    final boolean n21 = g.getOutgoingConnections(node2).contains(node1);

    if (!(n12 || n21)) {
      throw new IllegalArgumentException(
          "There is no connection between the nodes.");
    }
    final boolean bidi1 = n12 && n21;
    boolean bidi0 = false, bidi2 = false;

    final Set<Point> outgoing1 = new HashSet<Point>(
        g.getOutgoingConnections(node1));
    final Set<Point> incoming1 = new HashSet<Point>(
        g.getIncomingConnections(node1));
    outgoing1.remove(node2);
    incoming1.remove(node2);

    final Set<Point> outgoing2 = new HashSet<Point>(
        g.getOutgoingConnections(node2));
    final Set<Point> incoming2 = new HashSet<Point>(
        g.getIncomingConnections(node2));
    outgoing2.remove(node1);
    incoming2.remove(node1);

    final Set<Point> neighbors1 = new HashSet<Point>();
    neighbors1.addAll(outgoing1);
    neighbors1.addAll(incoming1);

    if (neighbors1.size() == 1) {
      final Point node0 = neighbors1.iterator().next();
      bidi0 = outgoing1.contains(node0) && incoming1.contains(node0);
    }

    final Set<Point> neighbors2 = new HashSet<Point>();
    neighbors2.addAll(outgoing2);
    neighbors2.addAll(incoming2);

    if (neighbors2.size() == 1) {
      final Point node3 = neighbors2.iterator().next();
      bidi2 = outgoing2.contains(node3) && incoming2.contains(node3);
    }

    if (neighbors1.size() != 1 && neighbors2.size() != 1) {
      return ContractType.NO;
    } else if (neighbors1.size() == 1 && neighbors2.size() == 1) {
      final boolean sameneigh = neighbors1.iterator().next()
          .equals(neighbors2.iterator().next());

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

  public static <E extends ConnectionData> List<Set<Point>> findNotFullyConnectedNodes(
      Graph<E> graph) {
    if (graph == null || graph.isEmpty()) {
      throw new IllegalArgumentException(
          "Graph may not be null and must contain at least one node.");
    }
    // just get a 'random' starting point
    return findNotFullyConnectedNodes(graph,
        new ArrayList<Point>(graph.getNodes()).get(0));
  }

  public static <E extends ConnectionData> List<Set<Point>> findNotFullyConnectedNodes(
      Graph<E> graph, Point root) {

    final HashSet<Point> fullyConnectedSet = new HashSet<Point>();
    final HashSet<Point> neighbours = new HashSet<Point>();
    final HashSet<Point> notConnectedSet = new HashSet<Point>();

    fullyConnectedSet.add(root);
    neighbours.addAll(graph.getOutgoingConnections(root));

    while (!neighbours.isEmpty()) {
      List<Point> path = null;
      final Point current = neighbours.iterator().next();
      neighbours.remove(current);
      if (graph.containsNode(current)) {
        try {
          path = Graphs.shortestPathEuclideanDistance(graph, current, root);
        } catch (final PathNotFoundException e) {/*
                                                  * this is intentionally empty
                                                  */}
      }

      if (path == null) {
        notConnectedSet.add(current);
      } else {
        for (final Point p : path) {
          fullyConnectedSet.add(p);
          if (neighbours.contains(p)) {
            neighbours.remove(p);
          }
          for (final Point q : graph.getOutgoingConnections(p)) {
            if (!fullyConnectedSet.contains(q)) {
              neighbours.add(q);
            }
          }
        }
      }
    }
    for (final Point p : graph.getNodes()) {
      if (!fullyConnectedSet.contains(p)) {
        notConnectedSet.add(p);
      }
    }

    return new ArrayList<Set<Point>>(asList(notConnectedSet, fullyConnectedSet));
  }

  // when executing there should be two folders at the same level as this
  // class (or jar):
  // 1. osm-files/
  // 2. dot-files/
  // when calling main("brussels") a file named brussels.osm is expected in
  // osm-files. All .dot output is written in dot-files.
  public static void main2(String[] args) throws FileNotFoundException,
      IOException {
    final DotGraphSerializer<MultiAttributeData> serializer = DotGraphSerializer
        .getMultiAttributeGraphSerializer();

    final String name = "leuven";// args[0];// "wroclaw";
    final String file = "/Users/rindevanlon/Downloads/temp.osm";

    System.out.println(name);
    final Graph<MultiAttributeData> g = OSM.parse(file);// "osm-files/" + name +
                                                        // ".osm");
    serializer.write(g, "dot-files/" + name + "-raw.dot");
    System.out.println(g);

    final long startRead = System.currentTimeMillis();
    final Graph<MultiAttributeData> g2 = serializer.read("dot-files/" + name
        + "-raw.dot");
    System.out.println("loading took: "
        + (System.currentTimeMillis() - startRead));
    Graph<MultiAttributeData> graph = new TableGraph<MultiAttributeData>(
        MultiAttributeData.EMPTY);
    graph.merge(g2);
    System.out.println("(V,E) = (" + graph.getNumberOfNodes() + ","
        + graph.getNumberOfConnections() + ")");

    // final long startSimplify = System.currentTimeMillis();
    // graph = MapPreprocessor.simplify(graph, MultiAttributeData.EMPTY);
    // System.out.println("simplifying took: "
    // + (System.currentTimeMillis() - startSimplify));

    final long startFix = System.currentTimeMillis();
    graph = MapPreprocessor.removeUnconnectedSubGraphs(graph,
        MultiAttributeData.EMPTY);
    System.out.println("fixing took: "
        + (System.currentTimeMillis() - startFix));
    serializer.write(graph, "dot-files/" + name + "-simple.dot");
  }
}
