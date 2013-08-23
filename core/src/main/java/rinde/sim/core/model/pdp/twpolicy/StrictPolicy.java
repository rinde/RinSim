/**
 * 
 */
package rinde.sim.core.model.pdp.twpolicy;

import rinde.sim.util.TimeWindow;

/**
 * Only allows pickups and deliveries which fit in the time windows, treats the
 * time windows as a hard constraint.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public final class StrictPolicy implements TimeWindowPolicy {

  public StrictPolicy() {}

  @Override
  public boolean canPickup(TimeWindow tw, long time, long duration) {
    return tw.isIn(time);
  }

  @Override
  public boolean canDeliver(TimeWindow tw, long time, long duration) {
    return tw.isIn(time);
  }

}
