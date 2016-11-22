/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import org.joda.time.Duration;
import org.joda.time.format.PeriodFormat;

import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.experiment.PostProcessor.FailureStrategy;
import com.github.rinde.rinsim.scenario.Scenario;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

/**
 * A {@link ResultListener} that writes simple progress reports to a
 * {@link PrintStream}.
 * @author Rinde van Lon
 */
public class CommandLineProgress implements ResultListener {
  private static final String SLASH = "/";

  private final PrintStream printStream;
  private int total;
  private int received;
  private int failures;
  private long startTime;

  /**
   * Create a new instance.
   * @param stream The stream to write the progress reports to.
   */
  public CommandLineProgress(PrintStream stream) {
    printStream = stream;
  }

  @Override
  public void startComputing(int numberOfSimulations,
      ImmutableSet<MASConfiguration> configurations,
      ImmutableSet<Scenario> scenarios,
      int repetitions,
      int seedRepetitions) {
    startTime = System.currentTimeMillis();
    printStream.print("Start computing: ");
    printStream.print(numberOfSimulations);
    printStream.print(" simulations (=");
    printStream.print(configurations.size());
    printStream.print(" configurations x ");
    printStream.print(scenarios.size());
    printStream.print(" scenarios x ");
    printStream.print(repetitions);
    printStream.print(" repetitions x ");
    printStream.print(seedRepetitions);
    printStream.println(" seed repetitions)");

    total = numberOfSimulations;
    received = 0;
    failures = 0;
  }

  @Override
  public void receive(SimulationResult result) {
    if (result.getResultObject() instanceof FailureStrategy) {
      failures++;
    } else {
      received++;
    }
    final Runtime r = Runtime.getRuntime();

    final long freeM = r.freeMemory() / 1024 / 1024;
    final long totalM = r.totalMemory() / 1024 / 1024;
    final long maxM = r.maxMemory() / 1024 / 1024;

    final Duration dur = new Duration(startTime, System.currentTimeMillis());
    printStream.println(Joiner.on("")
      .join(received, SLASH, total, " (failures: ", failures, ", duration: ",
        PeriodFormat.getDefault().print(dur.toPeriod()),
        ", memory free/total/max (M): ", freeM, SLASH, totalM, SLASH, maxM,
        ")"));
  }

  @Override
  public void doneComputing(ExperimentResults results) {
    final Duration dur = new Duration(startTime, System.currentTimeMillis());
    printStream.println("Computing done, duration: "
      + PeriodFormat.getDefault().print(dur.toPeriod()) + ".");
  }
}
