package rinde.sim.pdptw.experiment;

import rinde.sim.core.Simulator;

/**
 * A post-processor should collect results from a {@link Simulator}.
 * @param <T> The results object type.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface PostProcessor<T> {

  /**
   * Collects results from the provided {@link Simulator}.
   * @param sim The simulator.
   * @return An object containing simulation results.
   */
  T collectResults(Simulator sim);
}
