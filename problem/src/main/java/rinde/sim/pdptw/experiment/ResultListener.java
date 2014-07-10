package rinde.sim.pdptw.experiment;

import rinde.sim.pdptw.experiment.Experiment.SimulationResult;

public interface ResultListener {

  void startComputing(int numberOfSimulations);

  void receive(SimulationResult result);

  void doneComputing();

}
