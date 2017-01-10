/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.geom;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.reverse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

/**
 * Utility class containing many methods for working with graphs.
 * @author Rinde van Lon
 * @author Bartosz Michalik - change in the graphs model
 */
public final class Graphs {

  private Graphs() {}

  /**
   * Create a path of connections on the specified {@link Graph} using the
   * specified {@link Point}s. If the points <code>A, B, C</code> are specified,
   * the two connections: <code>A -&gt; B</code> and <code>B -&gt; C</code> will
   * be added to the graph.
   * @param graph The graph to which the connections will be added.
   * @param path Points that will be treated as a path.
   * @param <E> The type of connection data.
   */
  public static <E extends ConnectionData> void addPath(Graph<E> graph,
      Point... path) {
    for (int i = 1; i < path.length; i++) {
      graph.addConnection(path[i - 1], path[i]);
    }
  }

  /**
   * Create a path of connections on the specified {@link Graph} using the
   * specified {@link Point}s. If the points <code>A, B, C</code> are specified,
   * the two connections: <code>A -&gt; B</code> and <code>B -&gt; C</code> will
   * be added to the graph.
   * @param graph The graph to which the connections will be added.
   * @param path Points that will be treated as a path.
   * @param <E> The type of connection data.
   */
  public static <E extends ConnectionData> void addPath(Graph<E> graph,
      Iterable<Point> path) {
    final PeekingIterator<Point> it =
      Iterators.peekingIterator(path.iterator());
    while (it.hasNext()) {
      final Point n = it.next();
      if (it.hasNext()) {
        graph.addConnection(n, it.peek());
      }
    }
  }

  /**
   * Create a path of bi-directional connections on the specified {@link Graph}
   * using the specified {@link Point}s. If the points <code>A, B, C</code> are
   * specified, the four connections: <code>A -&gt; B</code>,
   * <code>B -&gt; A</code>, <code>B -&gt; C</code> and <code>C -&gt; B</code>
   * will be added to the graph.
   * @param graph The graph to which the connections will be added.
   * @param path Points that will be treated as a path.
   * @param <E> The type of connection data.
   */
  public static <E extends ConnectionData> void addBiPath(Graph<E> graph,
      Point... path) {
    addPath(graph, path);
    final List<Point> list = Arrays.asList(path);
    Collections.reverse(list);
    addPath(graph, list.toArray(new Point[path.length]));
  }

  /**
   * Create a path of bi-directional connections on the specified {@link Graph}
   * using the specified {@link Point}s. If the points <code>A, B, C</code> are
   * specified, the four connections: <code>A -&gt; B</code>,
   * <code>B -&gt; A</code>, <code>B -&gt; C</code> and <code>C -&gt; B</code>
   * will be added to the graph.
   * @param graph The graph to which the connections will be added.
   * @param path Points that will be treated as a path.
   * @param <E> The type of connection data.
   */
  public static <E extends ConnectionData> void addBiPath(Graph<E> graph,
      Iterable<Point> path) {
    addPath(graph, path);
    addPath(graph, reverse(newArrayList(path)));
  }

  /**
   * Returns an unmodifiable view on the specified {@link Graph}.
   * @param graph A graph.
   * @param <E> The type of connection data.
   * @return An unmodifiable view on the graph.
   */
  public static <E extends ConnectionData> Graph<E> unmodifiableGraph(
      Graph<E> graph) {
    return new UnmodifiableGraph<>(graph);
  }

  /**
   * Basic equals method.
   * @param g1 A graph.
   * @param other The object to compare for equality with g1.
   * @return <code>true</code> if the provided graphs are equal,
   *         <code>false</code> otherwise.
   */
  public static boolean equal(Graph<?> g1, @Nullable Object other) {
    if (!(other instanceof Graph<?>)) {
      return false;
    }
    final Graph<?> g2 = (Graph<?>) other;
    return Objects.equal(g1.getConnections(), g2.getConnections());
  }

  /**
   * Computes the shortest path based on the Euclidean distance.
   * @param graph The {@link Graph} on which the shortest path is searched.
   * @param from The start point of the path.
   * @param to The destination of the path.
   * @param <E> The type of connection data.
   * @return The shortest path that exists between <code>from</code> and
   *         <code>to</code>.
   */
  public static <E extends ConnectionData> List<Point> shortestPathEuclideanDistance(
      Graph<E> graph, final Point from, final Point to) {
    return Graphs.shortestPath(graph, from, to, GraphHeuristics.euclidean());
  }

  /**
   * A standard implementation of the
   * <a href="http://en.wikipedia.org/wiki/A*_search_algorithm">A* algorithm</a>
   * .
   * @author Rutger Claes
   * @author Rinde van Lon
   * @param graph The {@link Graph} which contains <code>from</code> and
   *          <code>to</code>.
   * @param from The start position
   * @param to The end position
   * @param h The {@link Heuristic} used in the A* implementation.
   * @param <E> The type of connection data.
   * @return The shortest path from <code>from</code> to <code>to</code> if it
   *         exists, otherwise a {@link PathNotFoundException} is thrown.
   * @throws PathNotFoundException if a path does not exist between
   *           <code>from</code> and <code>to</code>.
   */
  public static <E extends ConnectionData> List<Point> shortestPath(
      Graph<E> graph, final Point from, final Point to, Graphs.Heuristic h) {
    if (!graph.containsNode(from)) {
      throw new IllegalArgumentException("from should be valid node. " + from);
    }

    // The set of nodes already evaluated.
    final Set<Point> closedSet = new LinkedHashSet<>();

    // Distance from start along optimal path.
    final Map<Point, Double> gScore = new LinkedHashMap<>();
    gScore.put(from, 0d);

    // heuristic estimates
    final Map<Point, Double> hScore = new LinkedHashMap<>();
    hScore.put(from, h.estimateCost(graph, from, to));

    // Estimated total distance from start to goal through y
    final SortedMap<Double, Point> fScore = new TreeMap<>();
    fScore.put(h.estimateCost(graph, from, to), from);

    // The map of navigated nodes.
    final Map<Point, Point> cameFrom = new LinkedHashMap<>();

    while (!fScore.isEmpty()) {
      final Point current = fScore.remove(fScore.firstKey());
      if (current.equals(to)) {
        final List<Point> result = new ArrayList<>();
        result.add(from);
        result.addAll(Graphs.reconstructPath(cameFrom, to));
        return result;
      }
      closedSet.add(current);
      for (final Point outgoingPoint : graph.getOutgoingConnections(current)) {
        if (closedSet.contains(outgoingPoint)) {
          continue;
        }

        // tentative_g_score := g_score[x] + dist_between(x,y)
        final double tgScore = gScore.get(current)
          + h.calculateCost(graph, current, outgoingPoint);
        boolean tIsBetter = false;

        if (!fScore.values().contains(outgoingPoint)) {
          hScore.put(outgoingPoint,
            h.estimateCost(graph, outgoingPoint, to));
          tIsBetter = true;
        } else if (tgScore < gScore.get(outgoingPoint)) {
          tIsBetter = true;
        }

        if (tIsBetter) {
          cameFrom.put(outgoingPoint, current);
          gScore.put(outgoingPoint, tgScore);

          double fScoreValue = gScore.get(outgoingPoint)
            + hScore.get(outgoingPoint);
          while (fScore.containsKey(fScoreValue)) {
            fScoreValue = Double.longBitsToDouble(Double
              .doubleToLongBits(fScoreValue) + 1);
          }
          fScore.put(fScoreValue, outgoingPoint);
        }
      }
    }

    throw new PathNotFoundException("Cannot reach " + to + " from " + from);
  }

  /**
   * A method for finding the closest object to a point. If there is no object
   * <code>null</code> is returned instead.
   * @param pos The {@link Point} which is used as reference.
   * @param objects The {@link Collection} which is searched.
   * @param transformation A {@link Function} that transforms an object from
   *          <code>objects</code> into a {@link Point}, normally this means
   *          that the position of the object is retrieved.
   * @param <T> the type of object.
   * @return The closest object in <code>objects</code> to <code>pos</code> or
   *         <code>null</code> if no object exists.
   */
  @Nullable
  public static <T> T findClosestObject(Point pos, Collection<T> objects,
      Function<T, Point> transformation) {
    double dist = Double.MAX_VALUE;
    T closest = null;
    for (final T obj : objects) {
      @Nullable
      final Point objPos = transformation.apply(obj);
      assert objPos != null;
      final double currentDist = Point.distance(pos, objPos);
      if (currentDist < dist) {
        dist = currentDist;
        closest = obj;
      }
    }
    return closest;
  }

  /**
   * Searches the closest <code>n</code> objects to position <code>pos</code> in
   * collection <code>objects</code> using <code>transformation</code>.
   * @param pos The {@link Point} which is used as a reference point.
   * @param objects The list of objects which is searched.
   * @param transformation A function that transforms objects from
   *          <code>objects</code> to a point.
   * @param n The maximum number of objects to return where n must be &gt;= 0.
   * @param <T> The type of object.
   * @return A list of objects that are closest to <code>pos</code>. The list is
   *         ordered such that the closest object appears first. An empty list
   *         is returned when <code>objects</code> is empty.
   */
  public static <T> List<T> findClosestObjects(Point pos,
      Collection<T> objects, Function<T, Point> transformation, int n) {
    checkArgument(n > 0, "n must be positive.");
    final List<ObjectWithDistance<T>> objs = new ArrayList<>();
    for (final T obj : objects) {
      @Nullable
      final Point objPos = transformation.apply(obj);
      checkNotNull(objPos);
      objs.add(new ObjectWithDistance<>(obj, Point.distance(pos, objPos)));
    }
    Collections.sort(objs);
    final List<T> results = new ArrayList<>();
    for (final ObjectWithDistance<T> o : objs.subList(0,
      Math.min(n, objs.size()))) {
      results.add(o.obj);
    }
    return results;
  }

  /**
   * Calculates the length of a path. The length is calculated by simply summing
   * the distances of every two neighboring positions.
   * @param path A list of {@link Point}s forming a path.
   * @return The total length of the path.
   */
  public static double pathLength(List<Point> path) {
    double dist = 0;
    for (int i = 1; i < path.size(); i++) {
      dist += Point.distance(path.get(i - 1), path.get(i));
    }
    return dist;
  }

  /**
   * Calculates the extremes of the graph.
   * @param graph The graph.
   * @return A list containing two points, the first point contains the min x
   *         and min y, the second point contains the max x and max y.
   */
  public static ImmutableList<Point> getExtremes(Graph<?> graph) {
    final Collection<Point> nodes = graph.getNodes();
    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    for (final Point p : nodes) {
      minX = Math.min(minX, p.x);
      maxX = Math.max(maxX, p.x);
      minY = Math.min(minY, p.y);
      maxY = Math.max(maxY, p.y);
    }
    return ImmutableList.of(new Point(minX, minY), new Point(maxX, maxY));
  }

  static List<Point> reconstructPath(final Map<Point, Point> cameFrom,
      final Point end) {
    if (cameFrom.containsKey(end)) {
      final List<Point> path = reconstructPath(cameFrom, cameFrom.get(end));
      path.add(end);
      return path;
    }

    return new LinkedList<>();
  }

  /**
   * A heuristic can be used to direct the {@link #shortestPath} algorithm, it
   * determines the cost of traveling which should be minimized.
   * @author Rinde van Lon
   * @see GraphHeuristics
   */
  public interface Heuristic {
    /**
     * Can be used to estimate the cost of traveling a distance.
     * @param graph The graph.
     * @param from Start point of a connection.
     * @param to End point of a connection.
     * @return The estimate of the cost.
     */
    double estimateCost(Graph<?> graph, Point from, Point to);

    /**
     * Computes the cost of traveling over the connection as specified by the
     * provided points.
     * @param graph The graph.
     * @param from Start point of a connection.
     * @param to End point of a connection.
     * @return The cost of traveling.
     */
    double calculateCost(Graph<?> graph, Point from, Point to);
  }

  // Equals is not consistent with compareTo!
  private static final class ObjectWithDistance<T> implements
      Comparable<ObjectWithDistance<T>> {
    final double dist;
    final T obj;

    ObjectWithDistance(T pObj, double pDist) {
      obj = pObj;
      dist = pDist;
    }

    @Override
    public int compareTo(@Nullable ObjectWithDistance<T> o) {
      return Double.compare(dist, verifyNotNull(o).dist);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (other == null) {
        return false;
      }
      if (other == this) {
        return true;
      }
      if (getClass() != other.getClass()) {
        return false;
      }
      @SuppressWarnings("unchecked")
      final ObjectWithDistance<T> o = (ObjectWithDistance<T>) other;
      return Objects.equal(dist, o.dist) && Objects.equal(obj, o.obj);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(dist, obj);
    }
  }

  private static class UnmodifiableGraph<E extends ConnectionData> extends
      ForwardingGraph<E> {

    UnmodifiableGraph(Graph<E> delegateGraph) {
      super(delegateGraph);
    }

    @Override
    public Collection<Point> getOutgoingConnections(Point node) {
      return Collections.unmodifiableCollection(delegate
        .getOutgoingConnections(node));
    }

    @Override
    public Collection<Point> getIncomingConnections(Point node) {
      return Collections.unmodifiableCollection(delegate
        .getIncomingConnections(node));
    }

    @Override
    public Set<Connection<E>> getConnections() {
      return Collections.unmodifiableSet(delegate.getConnections());
    }

    @Override
    public Set<Point> getNodes() {
      return Collections.unmodifiableSet(delegate.getNodes());
    }

    @Override
    public void addConnection(Point from, Point to) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addConnection(Point from, Point to, @Nullable E connData) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addConnection(Connection<E> connection) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addConnections(Iterable<? extends Connection<E>> connections) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void merge(Graph<E> other) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void removeNode(Point node) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void removeConnection(Point from, Point to) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<E> setConnectionData(Point from, Point to, E connData) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<E> removeConnectionData(Point from, Point to) {
      throw new UnsupportedOperationException();
    }
  }

}
