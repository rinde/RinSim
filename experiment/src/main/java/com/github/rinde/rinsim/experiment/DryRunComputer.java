/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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
import java.util.Set;

import com.github.rinde.rinsim.experiment.Experiment.Builder;
import com.github.rinde.rinsim.experiment.Experiment.Computers;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;

class DryRunComputer implements Computer {

  private final Supplier<Computer> originalComputer;
  private final boolean verbose;
  private final PrintStream printStream;
  private final PrintStream errorStream;

  DryRunComputer(Supplier<Computer> c, boolean v, PrintStream stream,
      PrintStream error) {
    originalComputer = c;
    verbose = v;
    printStream = stream;
    errorStream = error;
  }

  @Override
  public ExperimentResults compute(Builder builder, Set<SimArgs> inputs) {

    printStream.println(
      "===================== RinSim Experiment start dry run =================="
        + "===");

    if (originalComputer == Computers.LOCAL) {
      printStream.println("Using local computation.");
      printStream.println("numThreads = " + builder.numThreads);
    } else if (originalComputer == Computers.DISTRIBUTED) {
      printStream.println("Using distributed computing using JPPF.");
      printStream.println("numBatches = " + builder.numBatches);
    } else {
      errorStream.println("Found unknown computer: " + originalComputer);
    }
    printStream.println("masterSeed = " + builder.masterSeed);
    printStream.println();
    printStream.println("Factorial experiment setup:");
    printStream.println();
    printStream.println("     # configurations = "
      + builder.configurationsSet.size());
    printStream.println("          # scenarios = "
      + builder.getNumScenarios());
    printStream.println("        # repetitions = " + builder.repetitions);
    printStream.println("   # seed repetitions = " + builder.seedRepetitions);
    printStream.println("------------------------------------ x");
    printStream.println("  total # simulations = " + inputs.size());
    printStream.println();
    printStream.println("experiment ordering = " + builder.experimentOrdering);
    printStream
      .println("experiment warmup period = " + builder.warmupPeriodMs + " ms");
    printStream.println();
    if (verbose) {
      printStream.println(
        "scenario-class,scenario-problem-class,scenario-instance-id,"
          + "config,seed,obj-func,gui,post-processor,ui-creator");
      for (final SimArgs args : inputs) {
        printStream.println(args);
      }
    }
    printStream.println(
      "===================== RinSim Experiment finished dry run ==============="
        + "======");
    return ExperimentResults.create(builder,
      ImmutableSet.<Experiment.SimulationResult>of());
  }

}
