package rinde.sim.pdptw.experiment;

import rinde.sim.pdptw.experiment.Experiment.SimulationResult;

public class CommandLineProgress implements ResultListener {

  int total;
  int received;

  @Override
  public void startComputing(int numberOfSimulations) {
    System.out.println("Start computing: " + numberOfSimulations);
    total = numberOfSimulations;
    received = 0;
  }

  @Override
  public void receive(SimulationResult result) {
    received++;
    System.out.println(received + "/" + total);
  }

  @Override
  public void doneComputing() {
    System.out.println("Computing done.");
  }

}
