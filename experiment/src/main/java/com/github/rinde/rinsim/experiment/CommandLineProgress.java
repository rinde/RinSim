/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.experiment;

import java.io.PrintStream;

import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;

/**
 * A {@link ResultListener} that writes simple progress reports to a
 * {@link PrintStream}.
 * @author Rinde van Lon 
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
