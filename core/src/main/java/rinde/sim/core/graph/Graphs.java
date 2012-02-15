/**
 * 
 */
package rinde.sim.core.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.RoadUser;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class Graphs {

	public static void addPath(Graph g, Point... path) {
		for (int i = 1; i < path.length; i++) {
			g.addConnection(path[i - 1], path[i]);
		}
	}

	// bidirectional
	public static void addBiPath(Graph g, Point... path) {
		addPath(g, path);

		List<Point> list = Arrays.asList(path);
		Collections.reverse(list);
		addPath(g, list.toArray(new Point[path.length]));
	}

	public static Graph unmodifiableGraph(Graph delegate) {
		return new UnmodifiableGraph(delegate);
	}

	public static boolean equals(Graph g1, Graph g2) {
		if (g1.getNumberOfNodes() != g2.getNumberOfNodes()) {
			return false;
		}
		if (g1.getNumberOfConnections() != g2.getNumberOfConnections()) {
			return false;
		}
		for (Entry<Point, Point> connection : g1.getConnections()) {
			if (!g2.hasConnection(connection.getKey(), connection.getValue())) {
				return false;
			}
			if (g1.connectionLength(connection.getKey(), connection.getValue()) != g2.connectionLength(connection.getKey(), connection.getValue())) {
				return false;
			}
		}
		return true;

	}

	private static class UnmodifiableGraph implements Graph {
		final Graph delegate;

		public UnmodifiableGraph(Graph delegate) {
			this.delegate = delegate;
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
		public Collection<Entry<Point, Point>> getConnections() {
			return Collections.unmodifiableCollection(delegate.getConnections());
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
		public void merge(Graph other) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addConnections(Collection<Entry<Point, Point>> connections) {
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
		public boolean equals(Object other) {
			return other instanceof Graph ? equals((Graph) other) : false;
		}

		@Override
		public boolean equals(Graph other) {
			return Graphs.equals(this, other);
		}

	}

	public static List<Point> shortestPathEuclidianDistance(Graph graph, final Point from, final Point to) {
		return Graphs.shortestPath(graph, from, to, new Graphs.EuclidianDistance());
	}

	/**
	 * http://en.wikipedia.org/wiki/A*_search_algorithm
	 * 
	 * @author Rutger Claes
	 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
	 */
	public static List<Point> shortestPath(Graph graph, final Point from, final Point to, Graphs.Heuristic h) {
		if (from == null || !graph.containsNode(from)) {
			throw new IllegalArgumentException("from should be valid vertex. " + from);
		}
		//		if (to == null || !graph.containsKey(to)) {
		//			throw new IllegalArgumentException("to should be valid vertex");
		//		}

		final Set<Point> closedSet = new LinkedHashSet<Point>(); // The set of nodes already evaluated.

		final Map<Point, Double> g_score = new LinkedHashMap<Point, Double>();// Distance from start along optimal path.
		g_score.put(from, 0d);

		final Map<Point, Double> h_score = new LinkedHashMap<Point, Double>();// heuristic estimates 
		h_score.put(from, h.calculateHeuristic(Point.distance(from, to)));

		final SortedMap<Double, Point> f_score = new TreeMap<Double, Point>(); // Estimated total distance from start to goal through y.
		f_score.put(h.calculateHeuristic(Point.distance(from, to)), from);

		final HashMap<Point, Point> came_from = new LinkedHashMap<Point, Point>();// The map of navigated nodes.

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
	 * Convenience method
	 * @param pos
	 * @param rs
	 * @param type
	 * @return
	 * @see #findClosestObject(Point, RoadModel, Collection)
	 */
	public static <T extends RoadUser> T findClosestObject(Point pos, RoadModel rs, final Class<T> type) {
		return findClosestObject(pos, rs, rs.getObjectsOfType(type));
	}

	/**
	 * Convenience method
	 * @param pos
	 * @param rs
	 * @param predicate
	 * @return
	 * @see #findClosestObject(Point, RoadModel, Collection)
	 */
	public static RoadUser findClosestObject(Point pos, RoadModel rs, Predicate<RoadUser> predicate) {
		Collection<RoadUser> filtered = Collections2.filter(rs.getObjects(), predicate);
		return findClosestObject(pos, rs, filtered);
	}

	public static <T extends RoadUser> T findClosestObject(Point pos, RoadModel rm, Collection<T> objects) {
		return findClosestObject(pos, objects, new RoadUserToPositionFunction<T>(rm));
	}

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

		public RoadUserWithDistance(T obj, double dist) {
			this.obj = obj;
			this.dist = dist;
		}

		@Override
		public int compareTo(RoadUserWithDistance<T> o) {
			return Double.compare(dist, o.dist);
		}
	}

	public static List<RoadUser> findClosestObjects(Point pos, RoadModel rm, Predicate<RoadUser> predicate, int n) {
		Collection<RoadUser> filtered = Collections2.filter(rm.getObjects(), predicate);
		return findClosestObjects(pos, rm, filtered, n);
	}

	public static <T extends RoadUser> List<T> findClosestObjects(Point pos, RoadModel rm, Class<T> type, int n) {
		return findClosestObjects(pos, rm, rm.getObjectsOfType(type), n);
	}

	public static <T extends RoadUser> List<T> findClosestObjects(Point pos, RoadModel rm, Collection<T> objects, int n) {
		return findClosestObjects(pos, objects, new RoadUserToPositionFunction<T>(rm), n);
	}

	public static <T> List<T> findClosestObjects(Point pos, Collection<T> objects, Function<T, Point> transformation, int n) {
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

		public RoadUserToPositionFunction(RoadModel rm) {
			this.rm = rm;
		}

		@Override
		public Point apply(T input) {
			return rm.getPosition(input);
		}
	}

	public static Collection<RoadUser> findObjectsWithinRadius(final Point position, final RoadModel model, final double radius) {
		return Graphs.findObjectsWithinRadius(position, model, radius, model.getObjects());
	}

	public static <T extends RoadUser> Collection<T> findObjectsWithinRadius(final Point position, final RoadModel model, final double radius, final Class<T> type) {
		return findObjectsWithinRadius(position, model, radius, model.getObjectsOfType(type));
	}

	protected static <T extends RoadUser> Collection<T> findObjectsWithinRadius(final Point position, final RoadModel model, final double radius, Collection<T> objects) {
		return Collections2.filter(objects, new DistancePredicate(position, model, radius));
	}

	private static class DistancePredicate implements Predicate<RoadUser> {
		private final Point position;
		private final RoadModel model;
		private final double radius;

		public DistancePredicate(final Point position, final RoadModel model, final double radius) {
			this.position = position;
			this.model = model;
			this.radius = radius;
		}

		@Override
		public boolean apply(RoadUser input) {
			return Point.distance(model.getPosition(input), position) < radius;
		}
	}

	public static double pathLength(List<Point> path) {
		double dist = 0;
		for (int i = 1; i < path.size(); i++) {
			dist += Point.distance(path.get(i - 1), path.get(i));
		}
		return dist;
	}

	static List<Point> reconstructPath(final HashMap<Point, Point> cameFrom, final Point end) {
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
