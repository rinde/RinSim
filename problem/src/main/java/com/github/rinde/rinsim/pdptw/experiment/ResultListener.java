package com.github.rinde.rinsim.pdptw.experiment;

import com.github.rinde.rinsim.pdptw.experiment.Experiment.SimulationResult;

/**
 * Implementors get notified of the progress of an {@link Experiment}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface ResultListener {
  /**
   * This method is called to signal the start of an experiment.
   * @param numberOfSimulations The number of simulations that is going to be
   *          executed.
   */
  void startComputing(int numberOfSimulations);

  /**
   * This method is called to signal the completion of a single experiment.
   * @param result The {@link SimulationResult} of the simulation that is
   *          finished.
   */
  void receive(SimulationResult result);

  /**
   * This method is called to signal the end of the experiment.
   */
  void doneComputing();
}
