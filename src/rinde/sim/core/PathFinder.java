package rinde.sim.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import rinde.sim.core.graph.Graph;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;

/**
 * http://en.wikipedia.org/wiki/A*_search_algorithm
 * 
 * @author Rutger Claes
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public class PathFinder {

	public static List<Point> shortestDistance(Graph graph, final Point from, final Point to) {
		return shortest(graph, from, to, new Distance());
	}

	public static List<Point> shortest(Graph graph, final Point from, final Point to, Heuristic h) {
		if (from == null || !graph.containsNode(from)) {
			throw new IllegalArgumentException("from should be valid vertex. " + from);
		}
		//		if (to == null || !graph.containsKey(to)) {
		//			throw new IllegalArgumentException("to should be valid vertex");
		//		}

		final Set<Point> closedSet = new HashSet<Point>(); // The set of nodes already evaluated.

		final Map<Point, Double> g_score = new HashMap<Point, Double>();// Distance from start along optimal path.
		g_score.put(from, 0d);

		final Map<Point, Double> h_score = new HashMap<Point, Double>();// heuristic estimates 
		h_score.put(from, h.calculateHeuristic(Point.distance(from, to)));

		final SortedMap<Double, Point> f_score = new TreeMap<Double, Point>(); // Estimated total distance from start to goal through y.
		f_score.put(h.calculateHeuristic(Point.distance(from, to)), from);

		final HashMap<Point, Point> came_from = new HashMap<Point, Point>();// The map of navigated nodes.

		while (!f_score.isEmpty()) {
			final Point current = f_score.remove(f_score.firstKey());

			if (current.equals(to)) {
				List<Point> result = new ArrayList<Point>();
				result.add(from);
				result.addAll(reconstructPath(came_from, to));
				return result;
			}

			closedSet.add(current);

			for (final Point outgoingPoint : graph.getConnectedNodes(current)) {
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

	public static List<Point> shortestDistance(Multimap<Point, Point> graph, final Point from, final Point to) {
		return shortest(graph, from, to, new Distance());
	}

	public static List<Point> shortest(Multimap<Point, Point> graph, final Point from, final Point to, Heuristic h) {
		if (from == null || !graph.containsKey(from)) {
			throw new IllegalArgumentException("from should be valid vertex. " + from);
		}
		//		if (to == null || !graph.containsKey(to)) {
		//			throw new IllegalArgumentException("to should be valid vertex");
		//		}

		final Set<Point> closedSet = new HashSet<Point>(); // The set of nodes already evaluated.

		final Map<Point, Double> g_score = new HashMap<Point, Double>();// Distance from start along optimal path.
		g_score.put(from, 0d);

		final Map<Point, Double> h_score = new HashMap<Point, Double>();// heuristic estimates 
		h_score.put(from, h.calculateHeuristic(Point.distance(from, to)));

		final SortedMap<Double, Point> f_score = new TreeMap<Double, Point>(); // Estimated total distance from start to goal through y.
		f_score.put(h.calculateHeuristic(Point.distance(from, to)), from);

		final HashMap<Point, Point> came_from = new HashMap<Point, Point>();// The map of navigated nodes.

		while (!f_score.isEmpty()) {
			final Point current = f_score.remove(f_score.firstKey());

			if (current.equals(to)) {
				List<Point> result = new ArrayList<Point>();
				result.add(from);
				result.addAll(reconstructPath(came_from, to));
				return result;
			}

			closedSet.add(current);

			for (final Point outgoingPoint : graph.get(current)) {
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

	private static List<Point> reconstructPath(final HashMap<Point, Point> cameFrom, final Point end) {
		if (cameFrom.containsKey(end)) {
			final List<Point> path = reconstructPath(cameFrom, cameFrom.get(end));
			path.add(end);
			return path;
		}

		return new LinkedList<Point>();
	}

	@SuppressWarnings("unchecked")
	public static <T> T findClosestObject(Point pos, RoadStructure rs, final Class<T> type) {
		return (T) findClosestObject(pos, rs, new Predicate<Object>() {
			@Override
			public boolean apply(Object input) {
				return type.isInstance(input);
			}
		});
	}

	public static Object findClosestObject(Point pos, RoadStructure rs, Predicate<Object> predicate) {
		Collection<Object> filtered = Collections2.filter(rs.getObjects(), predicate);

		double dist = Double.MAX_VALUE;
		Object closest = null;
		for (Object obj : filtered) {
			Point objPos = rs.getPosition(obj);
			double currentDist = Point.distance(pos, objPos);
			if (currentDist < dist) {
				dist = currentDist;
				closest = obj;
			}
		}

		return closest;
	}

	public static Collection<Object> findObjectsWithinRadius(final Point position, final RoadStructure model, final double radius) {
		return findObjectsWithinRadius(position, model, radius, Object.class);
	}

	@SuppressWarnings("unchecked")
	public static <T> Collection<T> findObjectsWithinRadius(final Point position, final RoadStructure model, final double radius, final Class<T> type) {
		return (Collection<T>) findObjectsWithinRadius(position, model, radius, new Predicate<Object>() {
			@Override
			public boolean apply(Object input) {
				return type.isInstance(input);
			}
		});
	}

	public static Collection<Object> findObjectsWithinRadius(final Point position, final RoadStructure model, final double radius, Predicate<Object> predicate) {
		Collection<Object> filtered = Collections2.filter(model.getObjects(), predicate);
		return Collections2.filter(filtered, new Predicate<Object>() {
			@Override
			public boolean apply(Object input) {
				return Point.distance(model.getPosition(input), position) < radius;
			}
		});
	}

	public static double length(List<Point> path) {
		double dist = 0;
		for (int i = 1; i < path.size(); i++) {
			dist += Point.distance(path.get(i - 1), path.get(i));
		}
		return dist;
	}

	interface Heuristic {
		public abstract double calculateHeuristic(double distance);

		public abstract double calculateCostOff(Point from, Point to);
	}

	static class Distance implements Heuristic {

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