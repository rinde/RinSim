package rinde.sim.core.model;

/**
 * Used to represent road users that want to reposition itself using 
 * the {@link RoadModel#followPath(RoadUser, java.util.Queue, long)} method
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public interface MovingRoadUser extends RoadUser {
	/**
	 * Get speed of the road user
	 * @return speed
	 */
	double getSpeed();
}
