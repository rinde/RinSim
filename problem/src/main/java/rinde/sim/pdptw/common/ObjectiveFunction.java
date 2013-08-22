/**
 * 
 */
package rinde.sim.pdptw.common;

import rinde.sim.pdptw.common.StatsTracker.StatisticsDTO;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface ObjectiveFunction {

  boolean isValidResult(StatisticsDTO stats);

  double computeCost(StatisticsDTO stats);

  String printHumanReadableFormat(StatisticsDTO stats);

}
