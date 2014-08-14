package com.github.rinde.rinsim.core;

/**
 * An interface that declares that a given simulation entity (e.g. agent)
 * requires the ability to get access to Simulator API
 * @author Bartosz Michalik 
 * 
 */
public interface SimulatorUser {
  /**
   * Through this method the user of the simulator receives a reference to the
   * {@link SimulatorAPI}.
   * @param api The simulator which this uses gets access to.
   */
  void setSimulator(SimulatorAPI api);
}
