package com.github.rinde.rinsim.pdptw.common;

/**
 * Instances of this interface define an objective on which a
 * {@link StatisticsDTO} can be measured.
 * <p>
 * Implementations should not keep any internal state.
 * @author Rinde van Lon 
 */
public interface ObjectiveFunction {

  /**
   * Determines whether a simulation run is valid. Usually this checks whether
   * certain requirements have been met during the simulation, e.g. have all
   * vehicles returned to the depot?
   * @param stats The {@link StatisticsDTO} to inspect.
   * @return <code>true</code> if the {@link StatisticsDTO} represents a valid
   *         result according to this objective function, <code>false</code>
   *         otherwise.
   */
  boolean isValidResult(StatisticsDTO stats);

  /**
   * Computes the cost (i.e. objective value) of the specified
   * {@link StatisticsDTO}.
   * @param stats The {@link StatisticsDTO} to inspect.
   * @return The cost.
   */
  double computeCost(StatisticsDTO stats);

  /**
   * @param stats The {@link StatisticsDTO} to inspect.
   * @return Should return a human readable string containing the objective
   *         value and any other relevant measures.
   */
  String printHumanReadableFormat(StatisticsDTO stats);

}
