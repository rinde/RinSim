package rinde.sim.core.model;

/**
 * Used to represent road users that want to reposition itself using the
 * {@link RoadModel#followPath(MovingRoadUser, java.util.Queue, long)} method
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public interface MovingRoadUser extends RoadUser {
	/**
	 * Get speed of the road user. The speed is expressed in graph units
	 * (typically meters) per hour.
	 * @see
	 * @return speed
	 */
	double getSpeed();
}
