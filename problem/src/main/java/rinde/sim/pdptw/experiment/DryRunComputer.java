package rinde.sim.pdptw.experiment;

import java.io.PrintStream;
import java.util.Set;

import rinde.sim.pdptw.experiment.Experiment.Builder;
import rinde.sim.pdptw.experiment.Experiment.Computers;
import rinde.sim.pdptw.experiment.Experiment.SimArgs;

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

    printStream
        .println("===================== RinSim Experiment start dry run =====================");

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
    printStream
        .println("     # configurations = " + builder.configurationsSet.size());
    printStream
        .println("          # scenarios = "
            + builder.getNumScenarios());
    printStream.println("        # repetitions = " + builder.repetitions);
    printStream.println("------------------------------------ x");
    printStream.println("  total # simulations = " + inputs.size());
    printStream.println();

    if (verbose) {
      printStream
          .println("scenario-class,scenario-problem-class,scenario-instance-id,config,seed,obj-func,gui,post-processor,ui-creator");
      for (final SimArgs args : inputs) {
        printStream.println(args);
      }
    }
    printStream
        .println("===================== RinSim Experiment finished dry run =====================");
    return new ExperimentResults(builder,
        ImmutableSet.<Experiment.SimulationResult> of());
  }

}
