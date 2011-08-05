/**
 * 
 */
package rinde.sim.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.Point;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class RoadModel {

	protected volatile Map<RoadUser, Location> objLocs;
	final Graph graph;

	public RoadModel(Graph graph) {
		if (graph == null) {
			throw new IllegalArgumentException("Graph cannot be null");
		}
		this.graph = graph;
		objLocs = Collections.synchronizedMap(new LinkedHashMap<RoadUser, Location>());
	}

	public void addConnection(Point from, Point to) {
		if (graph.hasConnection(from, to)) {
			throw new IllegalArgumentException("Connection already exists.");
		}
		graph.addConnection(from, to);
		assert graph.containsNode(from);
	}

	/**
	 * Merges the specified graph into this graph.
	 * @param other The specified graph.
	 */
	public void addGraph(Graph other) {
		graph.merge(other);
	}

	public void addObjectAt(RoadUser newObj, Point pos) {
		if (!graph.containsNode(pos)) {
			throw new IllegalArgumentException("Object must be initiated on a crossroad.");
		} else if (objLocs.containsKey(newObj)) {
			throw new IllegalArgumentException("Object is already added.");
		}
		objLocs.put(newObj, new Location(pos, null, 0));
	}

	//	public abstract double followPath(Object object, Queue<Point> path, double distance);

	public void addObjectAtSamePosition(RoadUser newObj, RoadUser existingObj) {
		if (objLocs.containsKey(newObj)) {
			throw new IllegalArgumentException("Object " + newObj + " is already added.");
		} else if (!objLocs.containsKey(existingObj)) {
			throw new IllegalArgumentException("Object " + existingObj + " does not exist.");
		}
		objLocs.put(newObj, objLocs.get(existingObj));
	}

	protected Location checkLocation(Location l) {
		if (l.to == null && !graph.containsNode(l.from)) {
			throw new IllegalStateException("Location points to non-existing vertex: " + l.from + ".");
		} else if (l.to != null && !graph.hasConnection(l.from, l.to)) {
			throw new IllegalStateException("Location points to non-existing connection: " + l.from + " >> " + l.to + ".");
		}
		return l;
	}

	/**
	 * Removes all objects on this RoadStructure instance.
	 */
	public void clear() {
		objLocs.clear();
	}

	public boolean containsObject(RoadUser obj) {
		return objLocs.containsKey(obj);
	}

	public boolean containsObjectAt(RoadUser obj, Point p) {
		if (containsObject(obj)) {
			return objLocs.get(obj).getPosition().equals(p);
		}
		return false;
	}

	public boolean equalPosition(RoadUser obj1, RoadUser obj2) {
		return containsObject(obj1) && containsObject(obj2) && getPosition(obj1).equals(getPosition(obj2));
	}

	/**
	 * This method can be called repeatedly to follow a path. Each time this
	 * method is invoked the <code>path</code> {@link Queue} can be modified.
	 * When a vertex in <code>path</code> has been visited, it is removed from
	 * the {@link Queue}.
	 * @param object The object in the physical world that is to be moved.
	 * @param path The path that is followed, it is modified by this method.
	 * @param distance The distance that is attempted to be traveled over the
	 *            <code>path</code>.
	 * @return The actual distance that <code>object</code> has traveled after
	 *         the execution of this method has finished.
	 */
	public double followPath(RoadUser object, Queue<Point> path, double distance) {
		assert path != null : "path cannot be null";
		assert path.peek() != null : "path cannot be empty";
		assert distance > 0 : "distance must be greater than 0";
		assert object != null : "object cannot be null";
		assert objLocs.containsKey(object) : "object must have a location";

		Location objLoc = objLocs.get(object);
		checkLocation(objLoc);

		if (objLoc.to != null && !path.peek().equals(objLoc.to) && (path.peek() instanceof MidPoint ? !((MidPoint) path.peek()).loc.to.equals(objLoc.to) : true)) {
			throw new IllegalArgumentException("Illegal path for this object, first point should be in current direction. " + path);
		} else if (objLoc.to == null && !path.peek().equals(objLoc.from) && !hasConnection(objLoc.from, path.peek())
				&& (path.peek() instanceof MidPoint ? !hasConnection(objLoc.from, ((MidPoint) path.peek()).loc.to) : true)) {
			throw new IllegalArgumentException("Illegal path for this object, first point should be current point.");
		}
		double travelDistance = distance;

		Point tempPos = objLoc.getPosition();
		double newDis = -1;
		boolean nextVertex = false;
		while (travelDistance > 0 && path.size() >= 1) {
			double dist = Point.distance(tempPos, path.peek());

			if (dist > 0 && graph.containsNode(tempPos) && !graph.hasConnection(tempPos, path.peek()) && !(path.peek() instanceof MidPoint)) {
				throw new IllegalStateException("followPath() attempts to use non-existing connection: " + tempPos + " >> " + path.peek() + ".");
			}

			if (travelDistance >= dist) {
				tempPos = path.remove();
				travelDistance -= dist;
				nextVertex = true;
			} else { // distanceLeft < dist
				newDis = travelDistance;
				travelDistance = 0;
			}
		}

		if (newDis == -1) {
			if (tempPos instanceof MidPoint) {
				objLocs.put(object, checkLocation(((MidPoint) tempPos).loc));
			} else {
				objLocs.put(object, checkLocation(new Location(tempPos)));
			}
		} else if (nextVertex) {
			if (path.peek() instanceof MidPoint) {
				objLocs.put(object, checkLocation(new Location(tempPos, ((MidPoint) path.peek()).loc.to, newDis)));
			} else {
				objLocs.put(object, checkLocation(new Location(tempPos, path.peek(), newDis)));
			}
		} else {
			Point t = objLoc.to;
			double relpos = objLoc.relativePos + newDis;
			if (t == null) {
				t = path.peek();
				relpos = newDis;
			}
			objLocs.put(object, checkLocation(new Location(objLoc.from, t, relpos)));
		}
		return distance - travelDistance;
	}

	/**
	 * @return An unmodifiable view on the graph.
	 */
	public Graph getGraph() {
		return Graphs.unmodifiableGraph(graph);
	}

	protected Point getNode(RoadUser obj) {
		assert obj != null;
		assert objLocs.containsKey(obj);

		if (objLocs.get(obj).to != null) {
			return objLocs.get(obj).to;
		} else {
			return objLocs.get(obj).from;
		}
	}

	public Set<Point> getNodes() {
		return graph.getNodes();
	}

	public int getNumberOfConnections() {
		return graph.getNumberOfConnections();
	}

	public int getNumberOfNodes() {
		return graph.getNumberOfNodes();
	}

	public Collection<Point> getObjectPositions() {
		return getObjectsAndPositions().values();
	}

	/**
	 * @return A synchronized and unmodifiable set of the objects in the road
	 *         structure.
	 */
	public Set<RoadUser> getObjects() {
		synchronized (objLocs) {
			Set<RoadUser> copy = new LinkedHashSet<RoadUser>();
			copy.addAll(objLocs.keySet());
			return Collections.unmodifiableSet(copy);
		}
	}

	public Set<RoadUser> getObjects(Predicate<RoadUser> p) {
		return Collections.unmodifiableSet(Sets.filter(getObjects(), p));
	}

	@SuppressWarnings("unchecked")
	public <Y extends RoadUser> Set<Y> getObjectsOfType(final Class<Y> type) {
		return (Set<Y>) getObjects(new Predicate<RoadUser>() {
			@Override
			public boolean apply(RoadUser input) {
				return type.isInstance(input);
			}
		});
	}

	public Map<RoadUser, Point> getObjectsAndPositions() {
		Map<RoadUser, Location> copiedMap;
		synchronized (objLocs) {
			copiedMap = new LinkedHashMap<RoadUser, Location>();
			copiedMap.putAll(objLocs);
		}// its save to release the lock now

		Map<RoadUser, Point> theMap = new LinkedHashMap<RoadUser, Point>();
		for (java.util.Map.Entry<RoadUser, Location> entry : copiedMap.entrySet()) {
			theMap.put(entry.getKey(), entry.getValue().getPosition());
		}
		return Collections.unmodifiableMap(theMap);
	}

	/**
	 * Method to retrieve the location of an object.
	 * @param obj The object for which the position is examined.
	 * @return The position (as a {@link Point} object) for the specified
	 *         <code>obj</code> object.
	 */
	public Point getPosition(RoadUser obj) {
		assert obj != null : "object cannot be null";
		assert objLocs.containsKey(obj) : "object must have a location in RoadStructure " + obj;
		return objLocs.get(obj).getPosition();
	}

	/**
	 * Convenience method for @link {@link #getShortestPathTo(Point, Point)}
	 * @param fromObj The object which is used as the path origin
	 * @param toObj The object which is used as the path destination
	 * @return The shortest path from 'fromObj' to 'toObj'.
	 */
	public List<Point> getShortestPathTo(RoadUser fromObj, RoadUser toObj) {
		assert objLocs.containsKey(toObj) : " to object should be in RoadStructure. " + toObj;
		Location l = objLocs.get(toObj);
		List<Point> path = getShortestPathTo(fromObj, l.from);
		if (l.to != null) {
			path.add(new MidPoint(l));
		}
		return path;
	}

	/**
	 * Convenience method for @link {@link #getShortestPathTo(Point, Point)}
	 * @param fromObj The object which is used as the path origin
	 * @param to The path destination
	 * @return The shortest path from 'fromObj' to 'to'
	 */
	public List<Point> getShortestPathTo(RoadUser fromObj, Point to) {
		assert objLocs.containsKey(fromObj) : " from object should be in RoadStructure. " + fromObj;
		Point from = getNode(fromObj);
		return getShortestPathTo(from, to);
	}

	/**
	 * Computes the shortest path between the two specified points. This method
	 * can be overridden by subclasses to provide specific features such as
	 * caching.
	 * @param from The path origin
	 * @param to The path destination
	 * @return The shortest path from 'from' to 'to'.
	 * @see Graphs#shortestPathDistance(Graph, Point, Point)
	 */
	public List<Point> getShortestPathTo(Point from, Point to) {
		return Graphs.shortestPathDistance(graph, from, to);
	}

	public boolean hasConnection(Point from, Point to) {
		return graph.hasConnection(from, to);
	}

	public void removeObject(RoadUser o) {
		assert objLocs.containsKey(o);
		objLocs.remove(o);
	}
}

//internal usage only
class Location {
	final Point from;
	final Point to;
	final double relativePos;
	final double roadLength;

	public Location(Point from) {
		this(from, null, -1);
	}

	public Location(Point from, Point to, double relativePos) {
		this.from = from;
		this.to = to;
		if (to != null) {
			this.relativePos = relativePos;
			roadLength = Point.distance(from, to);
		} else {
			roadLength = -1;
			this.relativePos = -1;
		}
	}

	Point getPosition() {
		if (to == null) {
			return from;
		}
		Point diff = Point.diff(to, from);
		double perc = relativePos / roadLength;
		return new Point(from.x + perc * diff.x, from.y + perc * diff.y);
	}

	@Override
	public String toString() {
		return "from:" + from + ", to:" + to + ", relativepos:" + relativePos;
	}
}

class MidPoint extends Point {
	final Location loc;

	public MidPoint(Location l) {
		super(l.getPosition().x, l.getPosition().y);
		loc = l;
	}
}
