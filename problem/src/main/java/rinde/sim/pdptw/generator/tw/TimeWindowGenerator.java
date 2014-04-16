/**
 * 
 */
package rinde.sim.pdptw.generator.tw;

import rinde.sim.core.graph.Point;
import rinde.sim.util.TimeWindow;

import com.google.common.collect.ImmutableList;

/**
 * Generator of {@link TimeWindow}s for pickup and delivery problems.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface TimeWindowGenerator {
  /**
   * Should return two {@link TimeWindow}s, a pickup time window and a delivery
   * time window. These time windows should be theoretically feasible, meaning
   * that they should be serviceable such that there is enough time for a
   * vehicle to return to the depot.
   * @param seed Random seed.
   * @param orderAnnounceTime The time at which the order is announced.
   * @param pickup Position of pickup.
   * @param delivery Position of delivery.
   * @return A list containing exactly two {@link TimeWindow}s. The first
   *         indicates the <i>pickup time window</i> the second indicates the
   *         <i>delivery time window</i>.
   */
  ImmutableList<TimeWindow> generate(long seed, long orderAnnounceTime,
      Point pickup, Point delivery);
}
