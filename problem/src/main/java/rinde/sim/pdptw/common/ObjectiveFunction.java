/**
 * 
 */
package rinde.sim.pdptw.common;

/**
 * Instances of this interface define an objective on which a .. can be
 * measured.
 * <p>
 * Implementations should not keep any internal state.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface ObjectiveFunction {

  boolean isValidResult(StatisticsDTO stats);

  double computeCost(StatisticsDTO stats);

  String printHumanReadableFormat(StatisticsDTO stats);

}
