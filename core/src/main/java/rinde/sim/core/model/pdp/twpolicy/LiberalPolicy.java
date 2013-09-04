/**
 * 
 */
package rinde.sim.core.model.pdp.twpolicy;

import rinde.sim.util.TimeWindow;

/**
 * Everything is fine, everything is going to be allright. Treats time windows
 * as a soft constraints.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class LiberalPolicy implements TimeWindowPolicy {

  /**
   * Instantiate a new liberal policy.
   */
  public LiberalPolicy() {}

  @Override
  public boolean canPickup(TimeWindow tw, long time, long duration) {
    return true;
  }

  @Override
  public boolean canDeliver(TimeWindow tw, long time, long duration) {
    return true;
  }
}
