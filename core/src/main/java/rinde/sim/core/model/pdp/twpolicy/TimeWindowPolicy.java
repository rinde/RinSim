/**
 * 
 */
package rinde.sim.core.model.pdp.twpolicy;

import rinde.sim.util.TimeWindow;

/**
 * Implementations of this interface can define a policy that says when pickups
 * and deliveries are allowed based on the specified time windows.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface TimeWindowPolicy {

  /**
   * @param tw The {@link TimeWindow} to assess.
   * @param time The time.
   * @param duration The pickup duration.
   * @return <code>true</code> if with the current combination of time window,
   *         time and duration a pickup is allowed, <code>false</code>
   *         otherwise.
   */
  boolean canPickup(TimeWindow tw, long time, long duration);

  /**
   * @param tw The {@link TimeWindow} to assess.
   * @param time The time.
   * @param duration The delivery duration.
   * @return <code>true</code> if with the current combination of time window,
   *         time and duration a delivery is allowed, <code>false</code>
   *         otherwise.
   */
  boolean canDeliver(TimeWindow tw, long time, long duration);
}
