/**
 * 
 */
package rinde.sim.problem.common;

import rinde.sim.problem.common.StatsTracker.StatisticsDTO;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface ObjectiveFunction {

	boolean isValidResult(StatisticsDTO stats);

	double computeCost(StatisticsDTO stats);

}
