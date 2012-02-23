/**
 * 
 */
package rinde.sim.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import rinde.sim.core.graph.EdgeData;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.MultiAttributeEdgeData;
import rinde.sim.core.graph.Point;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be> changes wrt. models infrastructure
 * 
 */
public class RoadModel implements Model<RoadUser> {
	// TODO remove the Graph related functions, and give a reference to an unmodifiable Graph instance instead

	protected volatile Map<RoadUser, Location> objLocs;
	final Graph<? extends EdgeData> graph;

	public RoadModel(Graph<? extends EdgeData> graph) {
		if (graph == null) {
			throw new IllegalArgumentException("Graph cannot be null");
		}
		this.graph = graph;
		objLocs = Collections.synchronizedMap(new LinkedHashMap<RoadUser, Location>());
	}

	
	//TODO [bm] remove
	public void addConnection(Point from, Point to) {
		if (graph.hasConnection(from, to)) {
			throw new IllegalArgumentException("Connection already exists.");
		}
		graph.addConnection(from, to);
		assert graph.containsNode(from);
	}

	public void addObjectAt(RoadUser newObj, Point pos) {
		if (!graph.containsNode(pos)) {
			throw new IllegalArgumentException("Object must be initiated on a crossroad.");
		} else if (objLocs.containsKey(newObj)) {
			throw new IllegalArgumentException("Object is already added.");
		}
		objLocs.put(newObj, new Location(pos, null, 0));
	}

	public void addObjectAtSamePosition(RoadUser newObj, RoadUser existingObj) {
		if (objLocs.containsKey(newObj)) {
			throw new IllegalArgumentException("Object " + newObj + " is already added.");
		} else if (!objLocs.containsKey(existingObj)) {
			throw new IllegalArgumentException("Object " + existingObj + " does not exist.");
		}
		objLocs.put(newObj, objLocs.get(existingObj));
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
	 * This method moves the specified {@link RoadUser} using the specified path
	 * and the specified distance.
	 * <p>
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
			if (dist > 0 && graph.containsNode(tempPos) && !graph.hasConnection(tempPos, path.peek()) && !(path.peek() instanceof MidPoint) && !(tempPos instanceof MidPoint)) {
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
	 * This method moves the specified {@link RoadUser} using the specified path
	 * and with the specified time. The road model is using the information 
	 * about speed of the {@link RoadUser} and constrains on the graph to reposition the object.
	 * <p>
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
	public PathProgress followPath(MovingRoadUser object, Queue<Point> path, long time) {
		assert path != null : "path cannot be null";
		assert path.peek() != null : "path cannot be empty";
		assert time > 0 : "distance must be greater than 0";
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
		
//		if (objLoc.isEdgePoint() && !path.peek().equals(objLoc.to) && // next point is not the end of edge
//			path.peek() instanceof MidPoint ? !((MidPoint) path.peek()).loc.to.equals(objLoc.to) : true) { //or is incorrect mid point
//				throw new IllegalArgumentException("Illegal path for this object, first point should be in current direction. " + path);
//		} else if (!objLoc.isEdgePoint() && !path.peek().equals(objLoc.from) && !hasConnection(objLoc.from, path.peek()) && //there is no connection to next point
//			(path.peek() instanceof MidPoint ? !hasConnection(objLoc.from, ((MidPoint) path.peek()).loc.to) : true)) { //or is incorrect mid point		
//			throw new IllegalArgumentException("Illegal path for this object, first point should be current point.");
//		}
		
		long timeLeft = time;
		double traveled = 0;
		
		Point tempPos = objLoc.getPosition();
		
		double newDis = Double.NaN;
		boolean nextVertex = false;
		while (timeLeft > 0 && path.size() > 0) {
			
			
			double speed = getMaxSpeed(object, tempPos, path.peek());
			double travelDistance = speed * timeLeft; 

			double dist = getDistance(tempPos, path.peek());
			
			
			if (travelDistance >= dist) {
				tempPos = path.remove();
				long timeSpent = Math.round(dist / speed);
				timeLeft -= timeSpent;
				nextVertex = true;
				
				traveled += dist;
			} else { // distanceLeft < dist
				newDis = travelDistance;				
				timeLeft = 0;
				long timeSpent = Math.round(travelDistance / speed);
				timeLeft -= timeSpent;
				traveled += travelDistance;
			}
		}

		if (Double.isNaN(newDis)) {
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
		return new PathProgress(traveled, time - (timeLeft > 0 ? timeLeft : 0));
	}
	
	
	/**
	 * Compute distance between two points. If points are equal the distance is 0.
	 * If both points are graph's nodes the method checks if there is a length of edge defined.
	 * If the from/to is on edge or the length of edge is not defined {@link Point#distance(Point, Point)} is used.
	 * @return the distance between two points
	 * @throws IllegalArgumentException when two points are part of the graph but are not equal or there is no connection between them
	 */
	private double getDistance(Point from, Point to) {
		assert from != null && to != null : "parameters are not null";
		if(from.equals(to)) return 0;
		if(from instanceof MidPoint || to instanceof MidPoint)
			return Point.distance(from, to);
		
		if(! graph.hasConnection(from, to)) {
			throw new IllegalArgumentException("followPath() attempts to use non-existing connection: " + from + " >> " + to + ".");
		}
		
		EdgeData data = graph.connectionData(from, to);
		assert data == null || !Double.isNaN(data.getLength()) : "edge length cannot be NaN";
		return data == null ? Point.distance(from, to) : data.getLength();
	}

	/**
	 * Compute speed of the object taking into account the speed limits of the object
	 * @param object traveling object
	 * @param from the point on the graph object is located
	 * @param to the next point on the path it want to reach
	 */
	protected double getMaxSpeed(MovingRoadUser object, Point from, Point to) {
		assert object != null && from != null && to != null;
		if(from.equals(to)) return object.getSpeed();
		
		Point start = from instanceof MidPoint ? ((MidPoint) from).loc.from : from;
		Point stop = to instanceof MidPoint ? ((MidPoint) to).loc.to: to;
		if(!hasConnection(start, stop)) 
			throw new IllegalArgumentException("points not connected " + from  + " >> " + to);
		EdgeData data = graph.connectionData(start, stop);
		if(data instanceof MultiAttributeEdgeData) {
			MultiAttributeEdgeData maed = (MultiAttributeEdgeData) data;
			double speed = maed.getMaxSpeed();
			return Double.isNaN(speed) ? object.getSpeed() : Math.min(speed, object.getSpeed());
		}
		return object.getSpeed();
	}

	/**
	 * @return An unmodifiable view on the graph.
	 */
	public Graph<? extends EdgeData> getGraph() {
		return Graphs.unmodifiableGraph(graph);
	}


	/**
	 * This method returns a collection of {@link Point} objects which are the
	 * positions of the objects that exist in this model. The returned
	 * collection is not a live view on the set, but a new created copy.
	 * @return The collection of {@link Point} objects.
	 */
	public Collection<Point> getObjectPositions() {
		return getObjectsAndPositions().values();
	}

	/**
	 * This method returns the set of {@link RoadUser} objects which exist in
	 * this model. The returned set is not a live view on the set, but a new
	 * created copy.
	 * @return The set of {@link RoadUser} objects.
	 */
	public Set<RoadUser> getObjects() {
		synchronized (objLocs) {
			Set<RoadUser> copy = new LinkedHashSet<RoadUser>();
			copy.addAll(objLocs.keySet());
			return copy;
		}
	}

	/**
	 * This method returns a set of {@link RoadUser} objects which exist in this
	 * model and satisfy the given {@link Predicate}. The returned set is not a
	 * live view on this model, but a new created copy.
	 * @param predicate The predicate that decides which objects to return.
	 * @return A set of {@link RoadUser} objects.
	 */
	public Set<RoadUser> getObjects(Predicate<RoadUser> predicate) {
		return Sets.filter(getObjects(), predicate);
	}

	/**
	 * This method returns a mapping of {@link RoadUser} to {@link Point}
	 * objects which exist in this model. The returned map is not a live view on
	 * this model, but a new created copy.
	 * @return A map of {@link RoadUser} to {@link Point} objects.
	 */
	public Map<RoadUser, Point> getObjectsAndPositions() {
		Map<RoadUser, Location> copiedMap;
		synchronized (objLocs) {
			copiedMap = new LinkedHashMap<RoadUser, Location>();
			copiedMap.putAll(objLocs);
		}// it is save to release the lock now

		Map<RoadUser, Point> theMap = new LinkedHashMap<RoadUser, Point>();
		for (java.util.Map.Entry<RoadUser, Location> entry : copiedMap.entrySet()) {
			theMap.put(entry.getKey(), entry.getValue().getPosition());
		}
		return theMap;
	}

	/**
	 * This method returns a set of {@link RoadUser} objects which exist in this
	 * model and are instances of the specified {@link Class}. The returned set
	 * is not a live view on the set, but a new created copy.
	 * @param type The type of returned objects.
	 * @return A set of {@link RoadUser} objects.
	 */
	@SuppressWarnings("unchecked")
	public <Y extends RoadUser> Set<Y> getObjectsOfType(final Class<Y> type) {
		return (Set<Y>) getObjects(new Predicate<RoadUser>() {
			@Override
			public boolean apply(RoadUser input) {
				return type.isInstance(input);
			}
		});
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
	 * Computes the shortest path between the two specified points. This method
	 * can be overridden by subclasses to provide specific features such as
	 * caching.
	 * @param from The path origin
	 * @param to The path destination
	 * @return The shortest path from 'from' to 'to'.
	 * @see Graphs#shortestPathDistance(Graph, Point, Point)
	 */
	//	public List<Point> getShortestPathTo(Point from, Point to) {
	//		return Graphs.shortestPathDistance(graph, from, to);
	//	}

	/**
	 * Convenience method for {@link #getShortestPathTo(Point, Point)}
	 * @param fromObj The object which is used as the path origin
	 * @param to The path destination
	 * @return The shortest path from 'fromObj' to 'to'
	 */
	public List<Point> getShortestPathTo(RoadUser fromObj, Point to) {
		assert objLocs.containsKey(fromObj) : " from object should be in RoadModel. " + fromObj;
		Point from = getNode(fromObj);
		return getShortestPathTo(from, to);
	}

	/**
	 * Convenience method for {@link #getShortestPathTo(Point, Point)}
	 * @param fromObj The object which is used as the path origin
	 * @param toObj The object which is used as the path destination
	 * @return The shortest path from 'fromObj' to 'toObj'.
	 */
	public List<Point> getShortestPathTo(RoadUser fromObj, RoadUser toObj) {
		assert objLocs.containsKey(toObj) : " to object should be in RoadModel. " + toObj;
		//		Location l = objLocs.get(toObj);
		List<Point> path = getShortestPathTo(fromObj, getPosition(toObj));
		//		if (l.isEdgePoint()) {
		//			path.add(l.getPosition());
		//		}
		return path;
	}

	public List<Point> getShortestPathTo(Point from, Point to) {
		List<Point> path = new ArrayList<Point>();
		Point f = from;
		if (from instanceof MidPoint) {
			f = ((MidPoint) from).loc.to;
			path.add(from);
		}

		Point t = to;
		if (to instanceof MidPoint) {
			t = ((MidPoint) to).loc.from;
		}
		path.addAll(doGetShortestPathTo(f, t));
		if (to instanceof MidPoint) {
			path.add(to);
		}
		return path;
	}

	protected List<Point> doGetShortestPathTo(Point from, Point to) {
		return Graphs.shortestPathEuclidianDistance(graph, from, to);
	}

	public boolean hasConnection(Point from, Point to) {
		return graph.hasConnection(from, to);
	}

	public void removeObject(RoadUser o) {
		assert objLocs.containsKey(o);
		objLocs.remove(o);
	}

	protected Location checkLocation(Location l) {
		if (!l.isEdgePoint() && !graph.containsNode(l.from)) {
			throw new IllegalStateException("Location points to non-existing vertex: " + l.from + ".");
		} else if (l.isEdgePoint() && !graph.hasConnection(l.from, l.to)) {
			throw new IllegalStateException("Location points to non-existing connection: " + l.from + " >> " + l.to + ".");
		}
		return l;
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

	@Override
	public boolean register(RoadUser element) {
		element.initRoadUser(this);
		return true;
	}

	@Override
	public Class<RoadUser> getSupportedType() {
		return RoadUser.class;
	}
	
	
	/**
	 * Represents the distance traveled and time spend in {@link RoadModel#followPath(RoadUser, Queue, long)}
	 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
	 * @since 2.0
	 */
	public static final class PathProgress {
		/**
		 * distance traveled in the {@link RoadModel#followPath(RoadUser, Queue, int)}
		 */
		public final double distance;
		/**
		 * time spend on traveling the distance
		 */
		public final long time;
		
		public PathProgress(double distance, long time) {
			assert distance >= 0;
			assert time >= 0;
			this.distance = distance;
			this.time = time;
		}
	}
	
	
	//internal usage only
	/**
	 * Object that is on the graph node has 
	 * to parameter  == <code>null</code>.
	 *
	 */
	class Location {
		private static final double DELTA = 0.000001;
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
			if (isEdgePoint()) {
				this.relativePos = relativePos;
				EdgeData data = graph.connectionData(from, to);
				roadLength = data == null || Double.isNaN(data.getLength()) ?  
						Point.distance(from, to) : data.getLength();
			} else {
				roadLength = -1;
				this.relativePos = -1;
			}
		}

		public boolean isEdgePoint() {
			return to != null;
		}

		@Override
		public String toString() {
			return "from:" + from + ", to:" + to + ", relativepos:" + relativePos;
		}

		Point getPosition() {
			if (!isEdgePoint()) {
				return from;
			}
			Point diff = Point.diff(to, from);
			double perc = relativePos / roadLength;
			if(perc + DELTA >= 1) return to;
			return new MidPoint(from.x + perc * diff.x, from.y + perc * diff.y, this);
		}
	}


	final class MidPoint extends Point {
		private static final long serialVersionUID = -8442184033570204979L;
		protected final Location loc;

		public MidPoint(double x, double y, Location l) {
			super(x, y);
			loc = l;
		}

		@Override
		public String toString() {
			return super.toString() + "{" + loc + "}";
		}
	}
}


