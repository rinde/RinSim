/**
 * 
 */
package rinde.sim.core.model.pdp.twpolicy;

import rinde.sim.util.TimeWindow;

/**
 * Strategy pattern (Gamma et al. 1994)
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface TimeWindowPolicy {

    boolean canPickup(TimeWindow tw, long time, long duration);

    boolean canDeliver(TimeWindow tw, long time, long duration);

}
