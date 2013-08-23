/**
 * 
 */
package rinde.sim.core.model.pdp.twpolicy;

import rinde.sim.util.TimeWindow;

/**
 * Everything is fine, everything is going to be allright. Treats the time
 * window as a soft constraint.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class LiberalPolicy implements TimeWindowPolicy {

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
