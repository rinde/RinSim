package com.github.rinde.rinsim.pdptw.experiment;

import java.io.PrintStream;

import com.github.rinde.rinsim.pdptw.experiment.Experiment.SimulationResult;

/**
 * A {@link ResultListener} that writes simple progress reports to a
 * {@link PrintStream}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class CommandLineProgress implements ResultListener {
  private final PrintStream printStream;
  private int total;
  private int received;

  /**
   * Create a new instance.
   * @param stream The stream to write the progress reports to.
   */
  public CommandLineProgress(PrintStream stream) {
    printStream = stream;
  }

  @Override
  public void startComputing(int numberOfSimulations) {
    printStream.println("Start computing: " + numberOfSimulations);
    total = numberOfSimulations;
    received = 0;
  }

  @Override
  public void receive(SimulationResult result) {
    received++;
    printStream.println(received + "/" + total);
  }

  @Override
  public void doneComputing() {
    printStream.println("Computing done.");
  }
}
