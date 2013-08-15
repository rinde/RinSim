/**
 * 
 */
package rinde.sim.pdptw.generator;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.util.TimeWindow;

import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface TimeWindowGenerator {

	/**
	 * Should return two {@link TimeWindow}s, a pickup time window and a
	 * delivery time window. These time windows should be theoretically
	 * feasible, meaning that they should be servicable such that there is
	 * enough time for a vehicle to return to the depot.
	 * @param pickup
	 * @param delivery
	 * @param rng
	 * @return
	 */
	ImmutableList<TimeWindow> generate(long orderAnnounceTime, Point pickup, Point delivery, RandomGenerator rng);
}
