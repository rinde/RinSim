package rinde.sim.core.model.road;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Queue;

import rinde.sim.core.graph.Point;

/**
 * Represents the distance traveled and time spend in
 * {@link RoadModel#followPath(MovingRoadUser, Queue, rinde.sim.core.TimeLapse)}
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public final class PathProgress {
	/**
	 * distance traveled in the
	 * {@link RoadModel#followPath(MovingRoadUser, Queue, rinde.sim.core.TimeLapse)}
	 */
	public final double distance;
	/**
	 * time spend on traveling the distance
	 */
	public final long time;

	public final List<Point> travelledNodes;

	PathProgress(double dist, long pTime, List<Point> pTravelledNodes) {
		checkArgument(dist >= 0, "distance must be greater than or equal to 0");
		checkArgument(pTime >= 0, "time must be greather than or equal to 0");
		checkArgument(pTravelledNodes != null, "travelledNodes can not be null");
		distance = dist;
		time = pTime;
		travelledNodes = pTravelledNodes;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("{PathProgress distance:").append(distance).append(" time:").append(time)
				.append(" travelledNodes:").append(travelledNodes).append("}").toString();
	}
}