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

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * Utility class containing many methods for working with graphs.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be> - change in the
 *         graphs model
 */
public final class Graphs {

	// TODO move all RoadModel related methods to new RoadModels class

	private Graphs() {}

	public static <E extends ConnectionData> void addPath(Graph<E> g, Point... path) {
		for (int i = 1; i < path.length; i++) {
			g.addConnection(path[i - 1], path[i]);
		}
	}

	// bidirectional
	public static <E extends ConnectionData> void addBiPath(Graph<E> g, Point... path) {
		addPath(g, path);
		List<Point> list = Arrays.asList(path);
		Collections.reverse(list);
		addPath(g, list.toArray(new Point[path.length]));
	}

	public static <E extends ConnectionData> Graph<E> unmodifiableGraph(Graph<E> delegate) {
		return new UnmodifiableGraph<E>(delegate);
	}

	public static <E extends ConnectionData> Connection<E> unmodifiableConnection(Connection<E> conn) {
		return new UnmodifiableConnection<E>(conn);
	}

	@SuppressWarnings("unchecked")
	public static <E extends ConnectionData> E unmodifiableEdgeData(E edgeData) {
		if (edgeData instanceof MultiAttributeData) {
			return (E) new UnmodifiableMultiAttributeEdgeData((MultiAttributeData) edgeData);
		}
		return edgeData;
	}

	/**
	 * Basic equals method.
	 * @param g1 A graph.
	 * @param g2 Another graph.
	 * @return <code>true</code> if the provided graphs are equal,
	 *         <code>false</code> otherwise.
	 */
	public static <E extends ConnectionData> boolean equals(Graph<? extends E> g1, Graph<? extends E> g2) {
		if (g1.getNumberOfNodes() != g2.getNumberOfNodes()) {
			return false;
		}
		if (g1.getNumberOfConnections() != g2.getNumberOfConnections()) {
			return false;
		}
		for (Connection<? extends E> g1conn : g1.getConnections()) {
			if (!g2.hasConnection(g1conn.from, g1conn.to)) {
				return false;
			}
			E g2connEdgeData = g2.connectionData(g1conn.from, g1conn.to);

			boolean null1 = g1conn.getData() == null;
			boolean null2 = g2connEdgeData == null;
			int nullCount = (null1 ? 1 : 0) + (null2 ? 1 : 0);
			if ((nullCount == 0 && !g1conn.getData().equals(g2connEdgeData)) || nullCount == 1) {
				return false;
			}
		}
		return true;

	}

	private static class UnmodifiableMultiAttributeEdgeData extends MultiAttributeData {

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

	private static final class UnmodifiableConnection<E extends ConnectionData> extends Connection<E> {
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
			return Graphs.unmodifiableEdgeData(original.getData());
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

	private static class UnmodifiableGraph<E extends ConnectionData> implements Graph<E> {
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
			return Collections.unmodifiableCollection(delegate.getOutgoingConnections(node));
		}

		@Override
		public Collection<Point> getIncomingConnections(Point node) {
			return Collections.unmodifiableCollection(delegate.getIncomingConnections(node));
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
			List<Connection<E>> conn = delegate.getConnections();
			List<Connection<E>> unmodConn = new ArrayList<Connection<E>>();
			for (Connection<E> c : conn) {
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
			return unmodifiableEdgeData(delegate.connectionData(from, to));
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

	public static <E extends ConnectionData> List<Point> shortestPathEuclidianDistance(Graph<E> graph,
			final Point from, final Point to) {
		return Graphs.shortestPath(graph, from, to, new Graphs.EuclidianDistance());
	}

	/**
	 * A standard implementation of the A* algorithm.
	 * http://en.wikipedia.org/wiki/A*_search_algorithm
	 * 
	 * @author Rutger Claes
	 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
	 * 
	 * @param graph The {@link Graph} which contains <code>from</code> and
	 *            <code>to</code>.
	 * @param from The start position
	 * @param to The end position
	 * @param h The {@link Heuristic} used in the A* implementation.
	 * @return The shortest path from <code>from</code> to <code>to</code> if it
	 *         exists, otherwise a {@link PathNotFoundException} is thrown.
	 */
	public static <E extends ConnectionData> List<Point> shortestPath(Graph<E> graph, final Point from, final Point to,
			Graphs.Heuristic h) {
		if (from == null || !graph.containsNode(from)) {
			throw new IllegalArgumentException("from should be valid vertex. " + from);
		}
		// if (to == null || !graph.containsKey(to)) {
		// throw new IllegalArgumentException("to should be valid vertex");
		// }

		// The set of nodes already evaluated.
		final Set<Point> closedSet = new LinkedHashSet<Point>();

		// Distance from start along optimal path.
		final Map<Point, Double> g_score = new LinkedHashMap<Point, Double>();
		g_score.put(from, 0d);

		// heuristic estimates
		final Map<Point, Double> h_score = new LinkedHashMap<Point, Double>();
		h_score.put(from, h.calculateHeuristic(Point.distance(from, to)));

		// Estimated total distance from start to goal through y
		final SortedMap<Double, Point> f_score = new TreeMap<Double, Point>();
		f_score.put(h.calculateHeuristic(Point.distance(from, to)), from);

		// The map of navigated nodes.
		final Map<Point, Point> came_from = new LinkedHashMap<Point, Point>();

		while (!f_score.isEmpty()) {
			final Point current = f_score.remove(f_score.firstKey());
			if (current.equals(to)) {
				List<Point> result = new ArrayList<Point>();
				result.add(from);
				result.addAll(Graphs.reconstructPath(came_from, to));
				return result;
			}
			closedSet.add(current);
			for (final Point outgoingPoint : graph.getOutgoingConnections(current)) {
				if (closedSet.contains(outgoingPoint)) {
					continue;
				}

				// tentative_g_score := g_score[x] + dist_between(x,y)
				final double t_g_score = g_score.get(current) + h.calculateCostOff(current, outgoingPoint);
				boolean t_is_better = false;

				if (!f_score.values().contains(outgoingPoint)) {
					h_score.put(outgoingPoint, h.calculateHeuristic(Point.distance(outgoingPoint, to)));
					t_is_better = true;
				} else if (t_g_score < g_score.get(outgoingPoint)) {
					t_is_better = true;
				}

				if (t_is_better) {
					came_from.put(outgoingPoint, current);
					g_score.put(outgoingPoint, t_g_score);

					double f_score_value = g_score.get(outgoingPoint) + h_score.get(outgoingPoint);
					while (f_score.containsKey(f_score_value)) {
						f_score_value = Double.longBitsToDouble(Double.doubleToLongBits(f_score_value) + 1);
					}
					f_score.put(f_score_value, outgoingPoint);
				}
			}
		}

		throw new PathNotFoundException("Cannot reach " + to + " from " + from);
	}

	/**
	 * Convenience method for
	 * {@link #findClosestObject(Point, RoadModel, Class)}.
	 * @param pos The {@link Point} which is used as reference.
	 * @param rm The {@link RoadModel} which is searched.
	 * @return The closest object in <code>rm</code> to <code>pos</code>.
	 * @see #findClosestObject(Point, RoadModel, Collection)
	 */
	@Deprecated
	public static RoadUser findClosestObject(Point pos, RoadModel rm) {
		return findClosestObject(pos, rm, RoadUser.class);
	}

	/**
	 * Convenience method for
	 * {@link #findClosestObject(Point, RoadModel, Collection)}.
	 * @param pos The {@link Point} which is used as reference.
	 * @param rm The {@link RoadModel} which is searched.
	 * @param type The type of object that is searched.
	 * @return The closest object in <code>rm</code> to <code>pos</code> of type
	 *         <code>type</code>.
	 * @see #findClosestObject(Point, RoadModel, Collection)
	 */
	@Deprecated
	public static <T extends RoadUser> T findClosestObject(Point pos, RoadModel rm, final Class<T> type) {
		return findClosestObject(pos, rm, rm.getObjectsOfType(type));
	}

	/**
	 * Convenience method for
	 * {@link #findClosestObject(Point, RoadModel, Collection)}.
	 * @param pos The {@link Point} which is used as reference.
	 * @param rm The {@link RoadModel} which is searched.
	 * @param predicate A {@link Predicate} indicating which objects are
	 *            included in the search.
	 * @return The closest object in <code>rm</code> to <code>pos</code> which
	 *         satisfies the <code>predicate</code>.
	 * @see #findClosestObject(Point, RoadModel, Collection)
	 */
	@Deprecated
	public static RoadUser findClosestObject(Point pos, RoadModel rm, Predicate<RoadUser> predicate) {
		Collection<RoadUser> filtered = Collections2.filter(rm.getObjects(), predicate);
		return findClosestObject(pos, rm, filtered);
	}

	/**
	 * Convenience method for
	 * {@link #findClosestObject(Point, Collection, Function)}.
	 * @param pos The {@link Point} which is used as reference.
	 * @param rm The {@link RoadModel} which is searched.
	 * @param objects The {@link Collection} which is searched, each object must
	 *            exist in <code>rm</code>.
	 * @return The closest object in <code>rm</code> to <code>pos</code> which
	 *         satisfies the <code>predicate</code>.
	 * @see #findClosestObject(Point, Collection, Function)
	 */
	@Deprecated
	public static <T extends RoadUser> T findClosestObject(Point pos, RoadModel rm, Collection<T> objects) {
		return findClosestObject(pos, objects, new RoadUserToPositionFunction<T>(rm));
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
	public static <T> T findClosestObject(Point pos, Collection<T> objects, Function<T, Point> transformation) {
		double dist = Double.MAX_VALUE;
		T closest = null;
		for (T obj : objects) {
			Point objPos = transformation.apply(obj);
			double currentDist = Point.distance(pos, objPos);
			if (currentDist < dist) {
				dist = currentDist;
				closest = obj;
			}
		}
		return closest;
	}

	static class RoadUserWithDistance<T> implements Comparable<RoadUserWithDistance<T>> {
		public final double dist;
		public final T obj;

		public RoadUserWithDistance(T pObj, double pDist) {
			obj = pObj;
			dist = pDist;
		}

		@Override
		public int compareTo(RoadUserWithDistance<T> o) {
			return Double.compare(dist, o.dist);
		}
	}

	/**
	 * Returns a list of objects from {@link RoadModel} <code>rm</code> ordered
	 * by its distance to position <code>pos</code>.
	 * @param pos The {@link Point} which is used as a reference point.
	 * @param rm The {@link RoadModel} instance in which the closest objects are
	 *            searched.
	 * @return A list of objects that are closest to <code>pos</code>. The list
	 *         is ordered such that the closest object appears first. An empty
	 *         list is returned when <code>objects</code> is empty.
	 */
	@Deprecated
	public static List<RoadUser> findClosestObjects(Point pos, RoadModel rm) {
		return findClosestObjects(pos, rm, RoadUser.class, Integer.MAX_VALUE);
	}

	/**
	 * Searches the closest <code>n</code> objects to position <code>pos</code>
	 * in {@link RoadModel} <code>rm</code>.
	 * @param pos The {@link Point} which is used as a reference point.
	 * @param rm The {@link RoadModel} instance in which the closest objects are
	 *            searched.
	 * @param n The maximum number of objects to return where n must be >= 0.
	 * @return A list of objects that are closest to <code>pos</code>. The list
	 *         is ordered such that the closest object appears first. An empty
	 *         list is returned when <code>objects</code> is empty.
	 */
	@Deprecated
	public static List<RoadUser> findClosestObjects(Point pos, RoadModel rm, int n) {
		return findClosestObjects(pos, rm, RoadUser.class, n);
	}

	/**
	 * Searches the closest <code>n</code> objects to position <code>pos</code>
	 * in {@link RoadModel} <code>rm</code>. Only the objects that satisfy
	 * <code>predicate</code> are included in the search.
	 * @param pos The {@link Point} which is used as a reference point.
	 * @param rm The {@link RoadModel} instance in which the closest objects are
	 *            searched.
	 * @param predicate Only objects that satisfy this predicate will be
	 *            returned.
	 * @param n The maximum number of objects to return where n must be >= 0.
	 * @return A list of objects that are closest to <code>pos</code>. The list
	 *         is ordered such that the closest object appears first. An empty
	 *         list is returned when <code>objects</code> is empty.
	 */
	@Deprecated
	public static List<RoadUser> findClosestObjects(Point pos, RoadModel rm, Predicate<RoadUser> predicate, int n) {
		if (rm == null) {
			throw new IllegalArgumentException("rm can not be null");
		}
		if (predicate == null) {
			throw new IllegalArgumentException("predicate can not be null");
		}
		Collection<RoadUser> filtered = Collections2.filter(rm.getObjects(), predicate);
		return findClosestObjects(pos, rm, filtered, n);
	}

	/**
	 * Searches the closest <code>n</code> objects to position <code>pos</code>
	 * in {@link RoadModel} <code>rm</code>.
	 * @param pos The {@link Point} which is used as a reference point.
	 * @param rm The {@link RoadModel} instance in which the closest objects are
	 *            searched.
	 * @param type The type of objects which are included in the search.
	 * @param n The maximum number of objects to return where n must be >= 0.
	 * @return A list of objects that are closest to <code>pos</code>. The list
	 *         is ordered such that the closest object appears first. An empty
	 *         list is returned when <code>objects</code> is empty.
	 */
	@Deprecated
	public static <T extends RoadUser> List<T> findClosestObjects(Point pos, RoadModel rm, Class<T> type, int n) {
		if (rm == null) {
			throw new IllegalArgumentException("rm can not be null");
		}
		return findClosestObjects(pos, rm, rm.getObjectsOfType(type), n);
	}

	/**
	 * Searches the closest <code>n</code> objects to position <code>pos</code>
	 * in collection <code>objects</code>.
	 * @param pos The {@link Point} which is used as a reference point.
	 * @param rm The {@link RoadModel} instance which is used to lookup the
	 *            positions of the objects in <code>objects</code>.
	 * @param objects The list of objects which is searched.
	 * @param n The maximum number of objects to return where n must be >= 0.
	 * @return A list of objects that are closest to <code>pos</code>. The list
	 *         is ordered such that the closest object appears first. An empty
	 *         list is returned when <code>objects</code> is empty.
	 */
	@Deprecated
	public static <T extends RoadUser> List<T> findClosestObjects(Point pos, RoadModel rm, Collection<T> objects, int n) {
		if (rm == null) {
			throw new IllegalArgumentException("rm can not be null");
		}
		return findClosestObjects(pos, objects, new RoadUserToPositionFunction<T>(rm), n);
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
	public static <T> List<T> findClosestObjects(Point pos, Collection<T> objects, Function<T, Point> transformation,
			int n) {
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
		List<RoadUserWithDistance<T>> objs = new ArrayList<RoadUserWithDistance<T>>();
		for (T obj : objects) {
			Point objPos = transformation.apply(obj);
			objs.add(new RoadUserWithDistance<T>(obj, Point.distance(pos, objPos)));
		}
		Collections.sort(objs);
		List<T> results = new ArrayList<T>();
		for (RoadUserWithDistance<T> o : objs.subList(0, Math.min(n, objs.size()))) {
			results.add(o.obj);
		}
		return results;
	}

	static class RoadUserToPositionFunction<T extends RoadUser> implements Function<T, Point> {
		private final RoadModel rm;

		public RoadUserToPositionFunction(RoadModel roadModel) {
			rm = roadModel;
		}

		@Override
		public Point apply(T input) {
			return rm.getPosition(input);
		}
	}

	/**
	 * Returns all {@link RoadUser}s in <code>model</code> that are
	 * <strong>within</strong> a bird-flight distance of <code>radius</code> to
	 * <code>position</code>.
	 * @param position The position which is used to measure distance.
	 * @param model The {@link RoadModel} which contains the objects.
	 * @param radius Objects with a distance smaller than <code>radius</code> to
	 *            <code>position</code> are included.
	 * @return A collection of {@link RoadUser}s.
	 */
	@Deprecated
	public static Collection<RoadUser> findObjectsWithinRadius(final Point position, final RoadModel model,
			final double radius) {
		return Graphs.findObjectsWithinRadius(position, model, radius, model.getObjects());
	}

	/**
	 * Returns all {@link RoadUser}s of type <code> type</code> in
	 * <code>model</code> that are <strong>within</strong> a bird-flight
	 * distance of <code>radius</code> to <code>position</code>.
	 * @param position The position which is used to measure distance.
	 * @param model The {@link RoadModel} which contains the objects.
	 * @param radius Objects with a distance smaller than <code>radius</code> to
	 *            <code>position</code> are included.
	 * @param type The {@link Class} of the required type.
	 * @return A collection of type <code>type</code>.
	 */
	@Deprecated
	public static <T extends RoadUser> Collection<T> findObjectsWithinRadius(final Point position,
			final RoadModel model, final double radius, final Class<T> type) {
		return findObjectsWithinRadius(position, model, radius, model.getObjectsOfType(type));
	}

	@Deprecated
	protected static <T extends RoadUser> Collection<T> findObjectsWithinRadius(final Point position,
			final RoadModel model, final double radius, Collection<T> objects) {
		return Collections2.filter(objects, new DistancePredicate(position, model, radius));
	}

	private static class DistancePredicate implements Predicate<RoadUser> {
		private final Point position;
		private final RoadModel model;
		private final double radius;

		public DistancePredicate(final Point p, final RoadModel m, final double r) {
			position = p;
			model = m;
			radius = r;
		}

		@Override
		public boolean apply(RoadUser input) {
			return Point.distance(model.getPosition(input), position) < radius;
		}
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

	static List<Point> reconstructPath(final Map<Point, Point> cameFrom, final Point end) {
		if (cameFrom.containsKey(end)) {
			final List<Point> path = reconstructPath(cameFrom, cameFrom.get(end));
			path.add(end);
			return path;
		}

		return new LinkedList<Point>();
	}

	interface Heuristic {
		public abstract double calculateHeuristic(double distance);

		public abstract double calculateCostOff(Point from, Point to);
	}

	static class EuclidianDistance implements Graphs.Heuristic {

		@Override
		public double calculateCostOff(final Point from, Point to) {
			return Point.distance(from, to);
		}

		@Override
		public double calculateHeuristic(final double distance) {
			return distance;
		}
	}
}
