/**
 * 
 */
package rinde.sim.core.graph;

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

import org.apache.commons.math3.random.RandomGenerator;

import com.google.common.base.Function;

/**
 * Utility class containing many methods for working with graphs.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be> - change in the
 *         graphs model
 */
public final class Graphs {

    private Graphs() {}

    /**
     * Create a path of connections on the specified {@link Graph} using the
     * specified {@link Point}s. If the points <code>A, B, C</code> are
     * specified, the two connections: <code>A -> B</code> and
     * <code>B -> C</code> will be added to the graph.
     * @param graph The graph to which the connections will be added.
     * @param path Points that will be treated as a path.
     */
    public static <E extends ConnectionData> void addPath(Graph<E> graph,
            Point... path) {
        for (int i = 1; i < path.length; i++) {
            graph.addConnection(path[i - 1], path[i]);
        }
    }

    /**
     * Create a path of bi-directional connections on the specified
     * {@link Graph} using the specified {@link Point}s. If the points
     * <code>A, B, C</code> are specified, the four connections:
     * <code>A -> B</code>, <code>B -> A</code>, <code>B -> C</code> and
     * <code>C -> B</code> will be added to the graph.
     * @param graph The graph to which the connections will be added.
     * @param path Points that will be treated as a path.
     */
    public static <E extends ConnectionData> void addBiPath(Graph<E> graph,
            Point... path) {
        addPath(graph, path);
        final List<Point> list = Arrays.asList(path);
        Collections.reverse(list);
        addPath(graph, list.toArray(new Point[path.length]));
    }

    /**
     * Returns an unmodifiable view on the specified {@link Graph}.
     * @param graph A graph.
     * @return An unmodifiable view on the graph.
     */
    public static <E extends ConnectionData> Graph<E> unmodifiableGraph(
            Graph<E> graph) {
        return new UnmodifiableGraph<E>(graph);
    }

    /**
     * Returns an unmodifiable view on the specified {@link Connection}.
     * @param conn A connection.
     * @return An unmodifiable view on the connection.
     */
    public static <E extends ConnectionData> Connection<E> unmodifiableConnection(
            Connection<E> conn) {
        return new UnmodifiableConnection<E>(conn);
    }

    /**
     * Returns an unmodifiable view on the specified {@link ConnectionData}.
     * @param connData Connection data.
     * @return An unmodifiable view on the connection data.
     */
    @SuppressWarnings("unchecked")
    public static <E extends ConnectionData> E unmodifiableConnectionData(
            E connData) {
        if (connData instanceof MultiAttributeData) {
            return (E) new UnmodifiableMultiAttributeEdgeData(
                    (MultiAttributeData) connData);
        }
        return connData;
    }

    /**
     * Basic equals method.
     * @param g1 A graph.
     * @param g2 Another graph.
     * @return <code>true</code> if the provided graphs are equal,
     *         <code>false</code> otherwise.
     */
    public static <E extends ConnectionData> boolean equals(
            Graph<? extends E> g1, Graph<? extends E> g2) {
        if (g1.getNumberOfNodes() != g2.getNumberOfNodes()) {
            return false;
        }
        if (g1.getNumberOfConnections() != g2.getNumberOfConnections()) {
            return false;
        }
        for (final Connection<? extends E> g1conn : g1.getConnections()) {
            if (!g2.hasConnection(g1conn.from, g1conn.to)) {
                return false;
            }
            final E g2connEdgeData = g2.connectionData(g1conn.from, g1conn.to);

            final boolean null1 = g1conn.getData() == null;
            final boolean null2 = g2connEdgeData == null;
            final int nullCount = (null1 ? 1 : 0) + (null2 ? 1 : 0);
            if ((nullCount == 0 && !g1conn.getData().equals(g2connEdgeData))
                    || nullCount == 1) {
                return false;
            }
        }
        return true;

    }

    /**
     * Computes the shortest path based on the euclidean distance.
     * @param graph The {@link Graph} on which the shortest path is searched.
     * @param from The start point of the path.
     * @param to The destination of the path.
     * @return The shortest path that exists between <code>from</code> and
     *         <code>to</code>.
     */
    public static <E extends ConnectionData> List<Point> shortestPathEuclideanDistance(
            Graph<E> graph, final Point from, final Point to) {
        return Graphs
                .shortestPath(graph, from, to, new Graphs.EuclidianDistance());
    }

    /**
     * A standard implementation of the <a href="http
     * ://en.wikipedia.org/wiki/A*_search_algorithm">A* algorithm</a>.
     * 
     * @param graph The {@link Graph} which contains <code>from</code> and
     *            <code>to</code>.
     * @param from The start position
     * @param to The end position
     * @param h The {@link Heuristic} used in the A* implementation.
     * @return The shortest path from <code>from</code> to <code>to</code> if it
     *         exists, otherwise a {@link PathNotFoundException} is thrown.
     * @throws PathNotFoundException if a path does not exist between
     *             <code>from</code> and <code>to</code>.
     * 
     * @author Rutger Claes
     * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
     */
    public static <E extends ConnectionData> List<Point> shortestPath(
            Graph<E> graph, final Point from, final Point to, Graphs.Heuristic h) {
        if (from == null || !graph.containsNode(from)) {
            throw new IllegalArgumentException("from should be valid vertex. "
                    + from);
        }
        // if (to == null || !graph.containsKey(to)) {
        // throw new IllegalArgumentException("to should be valid vertex");
        // }

        // The set of nodes already evaluated.
        final Set<Point> closedSet = new LinkedHashSet<Point>();

        // Distance from start along optimal path.
        final Map<Point, Double> gScore = new LinkedHashMap<Point, Double>();
        gScore.put(from, 0d);

        // heuristic estimates
        final Map<Point, Double> hScore = new LinkedHashMap<Point, Double>();
        hScore.put(from, h.estimateCost(Point.distance(from, to)));

        // Estimated total distance from start to goal through y
        final SortedMap<Double, Point> fScore = new TreeMap<Double, Point>();
        fScore.put(h.estimateCost(Point.distance(from, to)), from);

        // The map of navigated nodes.
        final Map<Point, Point> cameFrom = new LinkedHashMap<Point, Point>();

        while (!fScore.isEmpty()) {
            final Point current = fScore.remove(fScore.firstKey());
            if (current.equals(to)) {
                final List<Point> result = new ArrayList<Point>();
                result.add(from);
                result.addAll(Graphs.reconstructPath(cameFrom, to));
                return result;
            }
            closedSet.add(current);
            for (final Point outgoingPoint : graph
                    .getOutgoingConnections(current)) {
                if (closedSet.contains(outgoingPoint)) {
                    continue;
                }

                // tentative_g_score := g_score[x] + dist_between(x,y)
                final double tgScore = gScore.get(current)
                        + h.calculateCost(current, outgoingPoint);
                boolean tIsBetter = false;

                if (!fScore.values().contains(outgoingPoint)) {
                    hScore.put(outgoingPoint, h.estimateCost(Point
                            .distance(outgoingPoint, to)));
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
     *            <code>objects</code> into a {@link Point}, normally this means
     *            that the position of the object is retrieved.
     * @return The closest object in <code>objects</code> to <code>pos</code> or
     *         <code>null</code> if no object exists.
     */
    @Nullable
    public static <T> T findClosestObject(Point pos, Collection<T> objects,
            Function<T, Point> transformation) {
        double dist = Double.MAX_VALUE;
        T closest = null;
        for (final T obj : objects) {
            final Point objPos = transformation.apply(obj);
            final double currentDist = Point.distance(pos, objPos);
            if (currentDist < dist) {
                dist = currentDist;
                closest = obj;
            }
        }
        return closest;
    }

    /**
     * Searches the closest <code>n</code> objects to position <code>pos</code>
     * in collection <code>objects</code> using <code>transformation</code>.
     * @param pos The {@link Point} which is used as a reference point.
     * @param objects The list of objects which is searched.
     * @param transformation A function that transforms objects from
     *            <code>objects</code> to a point.
     * @param n The maximum number of objects to return where n must be >= 0.
     * @return A list of objects that are closest to <code>pos</code>. The list
     *         is ordered such that the closest object appears first. An empty
     *         list is returned when <code>objects</code> is empty.
     */
    public static <T> List<T> findClosestObjects(Point pos,
            Collection<T> objects, Function<T, Point> transformation, int n) {
        if (pos == null) {
            throw new IllegalArgumentException("pos can not be null");
        }
        if (objects == null) {
            throw new IllegalArgumentException("objects can not be null");
        }
        if (transformation == null) {
            throw new IllegalArgumentException("transformation can not be null");
        }
        if (n <= 0) {
            throw new IllegalArgumentException("n must be a positive integer");
        }
        final List<ObjectWithDistance<T>> objs = new ArrayList<ObjectWithDistance<T>>();
        for (final T obj : objects) {
            final Point objPos = transformation.apply(obj);
            objs.add(new ObjectWithDistance<T>(obj, Point.distance(pos, objPos)));
        }
        Collections.sort(objs);
        final List<T> results = new ArrayList<T>();
        for (final ObjectWithDistance<T> o : objs.subList(0, Math.min(n, objs
                .size()))) {
            results.add(o.obj);
        }
        return results;
    }

    /**
     * Calculates the length of a path. The length is calculated by simply
     * summing the distances of every two neighboring positions.
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

    static List<Point> reconstructPath(final Map<Point, Point> cameFrom,
            final Point end) {
        if (cameFrom.containsKey(end)) {
            final List<Point> path = reconstructPath(cameFrom, cameFrom.get(end));
            path.add(end);
            return path;
        }

        return new LinkedList<Point>();
    }

    /**
     * A heuristic can be used to direct the {@link #shortestPath} algorithm, it
     * determines the cost of traveling which should be minimized.
     * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
     */
    public interface Heuristic {
        /**
         * Can be used to estimate the cost of traveling a distance.
         * @param distance A distance.
         * @return The estimate of the cost.
         */
        double estimateCost(double distance);

        /**
         * Computes the cost of traveling over the connection as specified by
         * the provided points.
         * @param from Start point of a connection.
         * @param to End point of a connection.
         * @return The cost of traveling.
         */
        double calculateCost(Point from, Point to);
    }

    static class ObjectWithDistance<T> implements
            Comparable<ObjectWithDistance<T>> {
        public final double dist;
        public final T obj;

        public ObjectWithDistance(T pObj, double pDist) {
            obj = pObj;
            dist = pDist;
        }

        @Override
        public int compareTo(ObjectWithDistance<T> o) {
            return Double.compare(dist, o.dist);
        }
    }

    private static class UnmodifiableMultiAttributeEdgeData extends
            MultiAttributeData {

        private final MultiAttributeData original;

        public UnmodifiableMultiAttributeEdgeData(MultiAttributeData pOriginal) {
            super(-1);
            original = pOriginal;
        }

        @Override
        public double getLength() {
            return original.getLength();
        }

        @Override
        public double getMaxSpeed() {
            return original.getMaxSpeed();
        }

        @Override
        public Map<String, Object> getAttributes() {
            return original.getAttributes();
        }

        @Override
        public <E> E get(String key, Class<E> type) {
            return original.get(key, type);
        }

        @Override
        public double setMaxSpeed(double maxSpeed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E> void put(String key, E value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object obj) {
            return original.equals(obj);
        }

        @Override
        public int hashCode() {
            return original.hashCode();
        }

    }

    private static final class UnmodifiableConnection<E extends ConnectionData>
            extends Connection<E> {
        private final Connection<E> original;

        public UnmodifiableConnection(Connection<E> c) {
            super(c.from, c.to, null);
            original = c;
        }

        @Override
        public void setData(E data) {
            throw new UnsupportedOperationException();
        }

        @Override
        public E getData() {
            if (original.getData() == null) {
                return null;
            }
            return Graphs.unmodifiableConnectionData(original.getData());
        }

        @Override
        public boolean equals(Object obj) {
            return original.equals(obj);
        }

        @Override
        public int hashCode() {
            return original.hashCode();
        }

        @Override
        public String toString() {
            return original.toString();
        }
    }

    private static class UnmodifiableGraph<E extends ConnectionData> implements
            Graph<E> {
        final Graph<E> delegate;

        public UnmodifiableGraph(Graph<E> pDelegate) {
            delegate = pDelegate;
        }

        @Override
        public boolean containsNode(Point node) {
            return delegate.containsNode(node);
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
        public boolean hasConnection(Point from, Point to) {
            return delegate.hasConnection(from, to);
        }

        @Override
        public int getNumberOfConnections() {
            return delegate.getNumberOfConnections();
        }

        @Override
        public List<Connection<E>> getConnections() {
            final List<Connection<E>> conn = delegate.getConnections();
            final List<Connection<E>> unmodConn = new ArrayList<Connection<E>>();
            for (final Connection<E> c : conn) {
                unmodConn.add(unmodifiableConnection(c));
            }
            return Collections.unmodifiableList(unmodConn);
        }

        @Override
        public int getNumberOfNodes() {
            return delegate.getNumberOfNodes();
        }

        @Override
        public Set<Point> getNodes() {
            return Collections.unmodifiableSet(delegate.getNodes());
        }

        @Override
        public double connectionLength(Point from, Point to) {
            return delegate.connectionLength(from, to);
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public void addConnection(Point from, Point to) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void merge(Graph<E> other) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addConnections(Collection<Connection<E>> connections) {
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

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public boolean equals(Object other) {
            return other instanceof Graph ? equals((Graph) other) : false;
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public boolean equals(Graph<? extends E> other) {
            return Graphs.equals(this, other);
        }

        @Override
        public E connectionData(Point from, Point to) {
            return unmodifiableConnectionData(delegate.connectionData(from, to));
        }

        @Override
        public void addConnection(Point from, Point to, E edgeData) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addConnection(Connection<E> connection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public E setEdgeData(Point from, Point to, E edgeData) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Point getRandomNode(RandomGenerator generator) {
            return delegate.getRandomNode(generator);
        }

        @Override
        public Connection<E> getConnection(Point from, Point to) {
            return unmodifiableConnection(delegate.getConnection(from, to));
        }

    }

    static class EuclidianDistance implements Graphs.Heuristic {

        @Override
        public double calculateCost(final Point from, Point to) {
            return Point.distance(from, to);
        }

        @Override
        public double estimateCost(final double distance) {
            return distance;
        }
    }
}
