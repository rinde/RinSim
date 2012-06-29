/**
 * 
 */
package rinde.sim.core.model.road;

import static com.google.common.base.Preconditions.checkArgument;
import static rinde.sim.core.graph.Graphs.shortestPathEuclideanDistance;
import static rinde.sim.core.graph.Graphs.unmodifiableGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Connection;
import rinde.sim.core.graph.ConnectionData;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeData;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.GraphRoadModel.Loc;
import rinde.sim.util.SpeedConverter;
import rinde.sim.util.TimeUnit;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 *         TODO add class comment
 */
public class GraphRoadModel extends AbstractRoadModel<Loc> {

	protected final Graph<? extends ConnectionData> graph;

	// TODO add comments to public methods without comment

	/**
	 * @param pGraph The graph which will be used as road strucutre.
	 */
	public GraphRoadModel(Graph<? extends ConnectionData> pGraph) {
		super();
		checkArgument(pGraph != null, "Graph can not be null");
		graph = pGraph;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void addObjectAt(RoadUser newObj, Point pos) {
		checkArgument(graph.containsNode(pos), "Object must be initiated on a crossroad.");
		super.addObjectAt(newObj, newLoc(pos));
	}

	/**
	 * This method moves the specified {@link RoadUser} using the specified path
	 * and with the specified time. The road model is using the information
	 * about speed of the {@link RoadUser} and constrains on the graph to
	 * reposition the object.
	 * <p>
	 * This method can be called repeatedly to follow a path. Each time this
	 * method is invoked the <code>path</code> {@link Queue} can be modified.
	 * When a vertex in <code>path</code> has been visited, it is removed from
	 * the {@link Queue}.
	 * @param object The object in the physical world that is to be moved.
	 * @param path The path that is followed, it is modified by this method.
	 * @param time The time that has elapsed. The actual distance that the
	 *            {@link MovingRoadUser} has traveled is based on its speed an
	 *            the elapsed time.
	 * @return The actual distance that <code>object</code> has traveled after
	 *         the execution of this method has finished.
	 */
	// TODO add unit tests for timelapse inputs
	@Override
	public PathProgress followPath(MovingRoadUser object, Queue<Point> path, TimeLapse time) {
		checkArgument(object != null, "object cannot be null");
		checkArgument(objLocs.containsKey(object), "object must have a location");
		checkArgument(path.peek() != null, "path can not be empty");
		checkArgument(time.hasTimeLeft(), "can not follow path when to time is left");
		// checkArgument(time > 0, "time must be a positive number");

		Loc objLoc = objLocs.get(object);
		checkLocation(objLoc);

		// long timeLeft = time;
		double traveled = 0;

		Loc tempLoc = objLoc;
		Point tempPos = objLoc;

		double newDis = Double.NaN;

		final SpeedConverter sc = new SpeedConverter();

		List<Point> travelledNodes = new ArrayList<Point>();
		while (time.hasTimeLeft() && path.size() > 0) {
			checkIsValidMove(tempLoc, path.peek());

			// speed in graph units per hour -> converting to miliseconds
			double speed = getMaxSpeed(object, tempPos, path.peek());
			speed = sc.from(speed, TimeUnit.H).to(TimeUnit.MS);

			// distance that can be traveled in current edge with timeleft
			double travelDistance = speed * time.getTimeLeft();
			double connLength = computeConnectionLength(tempPos, path.peek());

			if (travelDistance >= connLength) {
				// jump to next vertex
				tempPos = path.remove();
				if (!(tempPos instanceof Loc)) {
					travelledNodes.add(tempPos);
				}
				long timeSpent = Math.round(connLength / speed);
				time.consume(timeSpent);
				traveled += connLength;

				if (tempPos instanceof Loc) {
					tempLoc = checkLocation(((Loc) tempPos));
				} else {
					tempLoc = checkLocation(newLoc(tempPos));
				}

			} else { // distanceLeft < connLength
				newDis = travelDistance;
				time.consumeAll();
				// timeLeft = 0;
				// long timeSpent = Math.round(travelDistance / speed);
				// timeLeft -= timeSpent;
				traveled += travelDistance;

				Point from = isMidPoint(tempLoc) ? tempLoc.conn.from : tempLoc;
				Point peekTo = isMidPoint(path.peek()) ? ((Loc) path.peek()).conn.to : path.peek();
				Connection<?> conn = graph.getConnection(from, peekTo);
				tempLoc = checkLocation(newLoc(conn, tempLoc.relativePos + newDis));
			}
			tempPos = tempLoc;
		}

		objLocs.put(object, tempLoc);
		return new PathProgress(traveled, time.getTimeConsumed(), travelledNodes);
	}

	protected void checkIsValidMove(Loc objLoc, Point nextHop) {
		// in case we start from an edge and our next destination is to go to
		// the end of the current edge then its ok. Otherwise more checks are
		// required..
		if (objLoc.isEdgePoint() && !nextHop.equals(objLoc.conn.to)) {
			// check if next destination is a MidPoint
			checkArgument(nextHop instanceof Loc, "Illegal path for this object, from a position on an edge we can not jump to another edge or go back.");
			Loc dest = (Loc) nextHop;
			// check for same edge
			checkArgument(objLoc.isOnSameConnection(dest), "Illegal path for this object, first point is not on the same edge as the object.");
			// check for relative position
			checkArgument(objLoc.relativePos <= dest.relativePos, "Illegal path for this object, can not move backward over an edge.");
		}
		// in case we start from a node and we are not going to another node
		else if (!objLoc.isEdgePoint() && !nextHop.equals(objLoc) && !graph.hasConnection(objLoc, nextHop)) {
			checkArgument(nextHop instanceof Loc, "Illegal path, first point should be directly connected to object location.");
			Loc dest = (Loc) nextHop;
			checkArgument(graph.hasConnection(objLoc, dest.conn.to), "Illegal path, first point is on an edge not connected to object location. ");
			checkArgument(objLoc.equals(dest.conn.from), "Illegal path, first point is on a different edge.");
		}
	}

	/**
	 * Compute distance between two points. If points are equal the distance is
	 * 0. This method uses length stored in {@link ConnectionData} objects when
	 * available.
	 * @return the distance between two points
	 * @throws IllegalArgumentException when two points are part of the graph
	 *             but are not equal or there is no connection between them
	 */
	protected double computeConnectionLength(Point from, Point to) {
		if (from == null) {
			throw new IllegalArgumentException("from can not be null");
		}
		if (to == null) {
			throw new IllegalArgumentException("to can not be null");
		}
		if (from.equals(to)) {
			return 0;
		}
		if (isMidPoint(from) && isMidPoint(to)) {
			Loc start = (Loc) from;
			Loc end = (Loc) to;
			checkArgument(start.isOnSameConnection(end), "the points are not on the same connection");
			return Math.abs(start.relativePos - end.relativePos);
		} else if (isMidPoint(from)) {
			Loc start = (Loc) from;
			checkArgument(start.conn.to.equals(to), "from is not on a connection leading to 'to'");
			return start.roadLength - start.relativePos;
		} else if (isMidPoint(to)) {
			Loc end = (Loc) to;
			checkArgument(end.conn.from.equals(from), "to is not connected to from");
			return end.relativePos;
		} else {
			checkArgument(graph.hasConnection(from, to), "connection does not exist");
			return getConnectionLength(graph.getConnection(from, to));
		}
	}

	protected static double getConnectionLength(Connection<?> conn) {
		return conn.getData() == null || Double.isNaN(conn.getData().getLength()) ? Point
				.distance(conn.from, conn.to) : conn.getData().getLength();
	}

	protected static boolean isMidPoint(Point p) {
		return p instanceof Loc && ((Loc) p).isEdgePoint();
	}

	protected Loc checkLocation(Loc l) {
		checkArgument(l.isEdgePoint() || graph.containsNode(l), "Location points to non-existing vertex: " + l + ".");
		checkArgument(!l.isEdgePoint() || graph.hasConnection(l.conn.from, l.conn.to), "Location points to non-existing connection: "
				+ l.conn + ".");
		return l;
	}

	/**
	 * Compute speed of the object taking into account the speed limits of the
	 * object
	 * @param object traveling object
	 * @param from the point on the graph object is located
	 * @param to the next point on the path it want to reach
	 */
	protected double getMaxSpeed(MovingRoadUser object, Point from, Point to) {
		if (object == null) {
			throw new IllegalArgumentException("object can not be null");
		}
		if (from == null) {
			throw new IllegalArgumentException("from can not be null");
		}
		checkArgument(to != null, "to can not be null");
		if (from.equals(to)) {
			return object.getSpeed();
		}

		Connection<?> conn = getConnection(from, to);

		// Point start = from instanceof Loc && ((Loc) from).isEdgePoint() ?
		// ((Loc) from).conn.from : from;
		// Point stop = to instanceof Loc && ((Loc) to).isEdgePoint() ? ((Loc)
		// to).conn.to : to;
		// checkArgument(graph.hasConnection(start, stop),
		// "points not connected " + from + " >> " + to);
		// EdgeData data = graph.connectionData(from, to);

		if (conn.getData() instanceof MultiAttributeData) {
			MultiAttributeData maed = (MultiAttributeData) conn.getData();
			double speed = maed.getMaxSpeed();
			return Double.isNaN(speed) ? object.getSpeed() : Math.min(speed, object.getSpeed());
		}
		return object.getSpeed();
	}

	// TODO error messages
	protected Connection<?> getConnection(Point from, Point to) {
		boolean fromIsMid = isMidPoint(from);
		boolean toIsMid = isMidPoint(to);
		if (fromIsMid && toIsMid) {
			Loc start = (Loc) from;
			Loc end = (Loc) to;
			checkArgument(start.isOnSameConnection(end), "should be on same connection");
			return start.conn;
		} else if (fromIsMid) {
			Loc start = (Loc) from;
			checkArgument(start.conn.to.equals(to));
			return start.conn;
		} else if (toIsMid) {
			Loc end = (Loc) to;
			checkArgument(end.conn.from.equals(from));
			return end.conn;
		} else {
			checkArgument(graph.hasConnection(from, to));
			return graph.getConnection(from, to);
		}
	}

	@Override
	public List<Point> getShortestPathTo(Point from, Point to) {
		if (from == null) {
			throw new IllegalArgumentException("from can not be null");
		}
		if (to == null) {
			throw new IllegalArgumentException("to can not be null");
		}
		List<Point> path = new ArrayList<Point>();
		Point start = from;
		if (isMidPoint(from)) {
			start = ((Loc) from).conn.to;
			path.add(from);
		}

		Point end = to;
		if (isMidPoint(to)) {
			end = ((Loc) to).conn.from;
		}
		path.addAll(doGetShortestPathTo(start, end));
		if (isMidPoint(to)) {
			path.add(to);
		}
		return path;
	}

	protected List<Point> doGetShortestPathTo(Point from, Point to) {
		return shortestPathEuclideanDistance(graph, from, to);
	}

	/**
	 * @return An unmodifiable view on the graph.
	 */
	public Graph<? extends ConnectionData> getGraph() {
		return unmodifiableGraph(graph);
	}

	/**
	 * Retrieves the connection which the specified {@link RoadUser} is at. If
	 * the road user is at a vertex <code>null</code> is returned instead.
	 * @param obj The object which position is checked.
	 * @return A {@link Connection} if <code>obj</code> is on one,
	 *         <code>null</code> otherwise.
	 */
	public Connection<? extends ConnectionData> getConnection(RoadUser obj) {
		Loc point = objLocs.get(obj);
		if (isMidPoint(point)) {
			return graph.getConnection(point.conn.from, point.conn.to);
		}
		return null;
	}

	// internal usage only
	/**
	 * Indicates a location somewhere on the graph. This can be either on a
	 * vertex or an edge.
	 */
	// final protected class Location extends Point {
	// private static final double DELTA = 0.000001;
	// final Point from;
	// final Point to;
	// final double relativePos;
	// final double roadLength;
	//
	// public Location(Point pFrom) {
	// this(pFrom, null, 0);
	// }
	//
	// public Location(Point pFrom, Point pTo, double pRelativePos) {
	// checkArgument(pFrom != null, "from can not be null");
	// from = pFrom;
	//
	// if (pTo instanceof MidPoint) {
	// to = ((MidPoint) pTo).loc.to;
	// } else {
	// to = pTo;
	// }
	// if (isEdgePoint()) {
	// relativePos = pRelativePos;
	// EdgeData data = graph.connectionData(pFrom, to);
	// roadLength = data == null || Double.isNaN(data.getLength()) ?
	// Point.distance(pFrom, to) : data
	// .getLength();
	// } else {
	// roadLength = -1;
	// relativePos = 0;
	// }
	// }
	//
	// public boolean isOnSameConnection(Location l) {
	// if (!isEdgePoint() || !l.isEdgePoint()) {
	// return false;
	// }
	// return from.equals(l.from) && to.equals(l.to);
	// }
	//
	// public boolean isEdgePoint() {
	// return to != null;
	// }
	//
	// @Override
	// public String toString() {
	// return "from:" + from + ", to:" + to + ", relativepos:" + relativePos;
	// }
	//
	// Point getPosition() {
	// if (!isEdgePoint()) {
	// return from;
	// }
	// Point diff = Point.diff(to, from);
	// double perc = relativePos / roadLength;
	// if (perc + DELTA >= 1) {
	// return to;
	// }
	// return new MidPoint(from.x + perc * diff.x, from.y + perc * diff.y,
	// this);
	// }
	// }

	@SuppressWarnings("synthetic-access")
	protected static Loc newLoc(Point p) {
		return new Loc(p.x, p.y, null, -1, 0);
	}

	protected static final double DELTA = 0.000001;

	@SuppressWarnings("synthetic-access")
	protected static Loc newLoc(Connection<? extends ConnectionData> conn, double relativePos) {
		if (conn == null) {
			throw new IllegalArgumentException("conn can not be null");
		}
		Point diff = Point.diff(conn.to, conn.from);
		double roadLength = getConnectionLength(conn);

		double perc = relativePos / roadLength;
		if (perc + DELTA >= 1) {
			return new Loc(conn.to.x, conn.to.y, null, -1, 0);
		}
		return new Loc(conn.from.x + perc * diff.x, conn.from.y + perc * diff.y, conn, roadLength, relativePos);
	}

	final protected static class Loc extends Point {
		/**
		 * 
		 */
		private static final long serialVersionUID = 7070585967590832300L;
		public final double roadLength;
		public final double relativePos;
		public final Connection<? extends ConnectionData> conn;

		private Loc(double pX, double pY, Connection<? extends ConnectionData> pConn, double pRoadLength, double pRelativePos) {
			super(pX, pY);
			roadLength = pRoadLength;
			relativePos = pRelativePos;
			conn = pConn;
		}

		public boolean isEdgePoint() {
			return conn != null;
		}

		public boolean isOnSameConnection(Loc l) {
			if (!isEdgePoint() || !l.isEdgePoint()) {
				return false;
			}
			return conn.equals(l.conn);
		}

		@Override
		public String toString() {
			return super.toString() + "{" + conn + "}";
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.AbstractRoadModel#locObj2point(java.lang.Object)
	 */
	@Override
	protected Point locObj2point(Loc locObj) {
		return locObj;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.AbstractRoadModel#point2LocObj(rinde.sim.core.graph
	 * .Point)
	 */
	@Override
	protected Loc point2LocObj(Point point) {
		return newLoc(point);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.road.RoadModel#getRandomPosition(org.apache.commons
	 * .math.random.RandomGenerator)
	 */
	@Override
	public Point getRandomPosition(RandomGenerator rnd) {
		return graph.getRandomNode(rnd);
	}

	// final protected class MidPoint extends Point {
	// private static final long serialVersionUID = -8442184033570204979L;
	// protected final Location loc;
	//
	// public MidPoint(double pX, double pY, Location l) {
	// super(pX, pY);
	// loc = l;
	// }
	//
	// @Override
	// public String toString() {
	// return super.toString() + "{" + loc + "}";
	// }
	// }

}
