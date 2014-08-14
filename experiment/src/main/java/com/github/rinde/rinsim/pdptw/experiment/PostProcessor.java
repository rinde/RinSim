package com.github.rinde.rinsim.pdptw.experiment;

import com.github.rinde.rinsim.core.Simulator;

/**
 * A post-processor should collect results from a {@link Simulator}.
 * @param <T> The results object type.
 * 
 * @author Rinde van Lon 
 */
public interface PostProcessor<T> {

  /**
   * Collects results from the provided {@link Simulator}.
   * @param sim The simulator.
   * @return An object containing simulation results.
   */
  T collectResults(Simulator sim);
}
