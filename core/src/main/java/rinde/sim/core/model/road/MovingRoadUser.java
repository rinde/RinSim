package rinde.sim.core.model.road;

import javax.measure.Measure;
import javax.measure.quantity.Velocity;

/**
 * Used to represent road users that want to reposition itself using the
 * {@link RoadModel#followPath(MovingRoadUser, java.util.Queue, rinde.sim.core.TimeLapse)}
 * method.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * @since 2.0
 */
public interface MovingRoadUser extends RoadUser {
  /**
   * Get preferred speed of the road user. This speed is used as a maximum
   * speed, generally there is no guarantee that the object will always be
   * moving using this speed.
   * @return The preferred speed of this {@link MovingRoadUser}.
   * @see RoadModel#followPath(MovingRoadUser, java.util.Queue,
   *      rinde.sim.core.TimeLapse)
   */
  Measure<Double, Velocity> getSpeed();
}
