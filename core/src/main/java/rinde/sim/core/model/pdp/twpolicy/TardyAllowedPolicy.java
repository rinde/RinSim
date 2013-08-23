/**
 * 
 */
package rinde.sim.core.model.pdp.twpolicy;

import rinde.sim.util.TimeWindow;

/**
 * Being tardy (late) is allowed, being early is NOT! Earliness is a hard
 * constraint, tardiness is a soft constraint.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public final class TardyAllowedPolicy implements TimeWindowPolicy {

  public TardyAllowedPolicy() {}

  @Override
  public boolean canPickup(TimeWindow tw, long time, long duration) {
    return tw.isAfterStart(time);
  }

  @Override
  public boolean canDeliver(TimeWindow tw, long time, long duration) {
    return tw.isAfterStart(time);
  }

}
