/**
 * 
 */
package rinde.sim.core.model.road;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.min;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.math.random.RandomGenerator;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.util.SpeedConverter;
import rinde.sim.util.TimeUnit;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be) TODO add class comment
 */
public class PlaneRoadModel extends AbstractRoadModel<Point> {

	public final Point min;
	public final Point max;
	public final double width;
	public final double height;
	public final double maxSpeed;

	// TODO add comments to PlaneRoadModel

	public PlaneRoadModel(Point pMin, Point pMax, double pMaxSpeed) {
		checkArgument(pMin.x < pMax.x && pMin.y < pMax.y, "min should have coordinates smaller than max");
		checkArgument(pMaxSpeed > 0, "max speed must be positive");
		min = pMin;
		max = pMax;
		width = max.x - min.x;
		height = max.y - min.y;
		maxSpeed = pMaxSpeed;
	}

	@Override
	public Point getRandomPosition(RandomGenerator rnd) {
		return new Point(min.x + (rnd.nextDouble() * width), min.y + (rnd.nextDouble() * height));
	}

	@Override
	public void addObjectAt(RoadUser obj, Point pos) {
		checkArgument(checkPointIsInBoundary(pos));
		super.addObjectAt(obj, pos);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.road.AbstractRoadModel#followPath(rinde.sim.core
	 * .model.road.MovingRoadUser, java.util.Queue, long)
	 */
	@Override
	public PathProgress followPath(MovingRoadUser object, Queue<Point> path, TimeLapse time) {
		checkArgument(containsObject(object), "object must exist in RoadModel");
		checkArgument(!path.isEmpty(), "path can not be empty");
		checkArgument(time.hasTimeLeft(), "there must be time left to execute follow path");
		// checkArgument(time > 0, "time must be a positive number");

		Point loc = objLocs.get(object);

		// long timeLeft = time.getTimeLeft();
		double traveled = 0;

		final SpeedConverter sc = new SpeedConverter();
		// speed in graph units per hour -> converting to milliseconds
		double speed = min(object.getSpeed(), maxSpeed);
		speed = sc.from(speed, TimeUnit.H).to(TimeUnit.MS);

		List<Point> travelledNodes = new ArrayList<Point>();
		while (time.hasTimeLeft() && path.size() > 0) {
			checkArgument(checkPointIsInBoundary(path.peek()), "points in the path must be within the predefined boundary of the plane");

			// distance that can be traveled with timeleft
			double travelDistance = speed * time.getTimeLeft();
			double stepLength = Point.distance(loc, path.peek());
			double perc = travelDistance / stepLength;

			if (perc + DELTA >= 1) {
				loc = path.remove();
				travelledNodes.add(loc);
				time.consume(Math.round(stepLength / speed));
				traveled += stepLength;
			} else {
				Point diff = Point.diff(path.peek(), loc);
				loc = new Point(loc.x + perc * diff.x, loc.y + perc * diff.y);
				time.consume(Math.round(travelDistance / speed));
				traveled += travelDistance;
			}
		}
		objLocs.put(object, loc);
		return new PathProgress(traveled, time.getTimeConsumed(), travelledNodes);
	}

	protected static final double DELTA = 0.000001;

	@Override
	public List<Point> getShortestPathTo(Point from, Point to) {
		checkArgument(checkPointIsInBoundary(from), "from must be within the predefined boundary of the plane");
		checkArgument(checkPointIsInBoundary(to), "to must be within the predefined boundary of the plane");
		return asList(from, to);
	}

	@Override
	protected Point locObj2point(Point locObj) {
		return locObj;
	}

	@Override
	protected Point point2LocObj(Point point) {
		return point;
	}

	protected boolean checkPointIsInBoundary(Point p) {
		return p.x >= min.x && p.x <= max.x && p.y >= min.y && p.y <= max.y;
	}

}
