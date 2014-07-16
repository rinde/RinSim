package rinde.sim.pdptw.experiment;

import java.util.Set;

import rinde.sim.pdptw.experiment.Experiment.Builder;
import rinde.sim.pdptw.experiment.Experiment.Builder.SimArgs;
import rinde.sim.pdptw.experiment.Experiment.Computers;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;

class DryRunComputer implements Computer {

  private final Supplier<Computer> originalComputer;
  private final boolean verbose;

  DryRunComputer(Supplier<Computer> c, boolean v) {
    originalComputer = c;
    verbose = v;
  }

  @Override
  public ExperimentResults compute(Builder builder, Set<SimArgs> inputs) {

    System.out
        .println("===================== RinSim Experiment start dry run =====================");

    if (originalComputer == Computers.LOCAL) {
      System.out.println("Using local computation.");
      System.out.println("numThreads = " + builder.numThreads);
    } else if (originalComputer == Computers.DISTRIBUTED) {
      System.out.println("Using distributed computing using JPPF.");
      System.out.println("numBatches = " + builder.numBatches);
    } else {
      System.err.println("Found unknown computer: " + originalComputer);
    }
    System.out.println("masterSeed = " + builder.masterSeed);
    System.out.println();
    System.out.println("Factorial experiment setup:");
    System.out.println();
    System.out
        .println("     # configurations = " + builder.configurationsSet.size());
    System.out
        .println("          # scenarios = "
            + builder.scenariosBuilder.build().size());
    System.out.println("        # repetitions = " + builder.repetitions);
    System.out.println("------------------------------------ x");
    System.out.println("  total # simulations = " + inputs.size());
    System.out.println();

    if (verbose) {
      System.out
          .println("scenario-class,scenario-problem-class,scenario-instance-id,config,seed,obj-func,gui,post-processor,ui-creator");
      for (final SimArgs args : inputs) {
        System.out.println(args);
      }
    }
    System.out
        .println("===================== RinSim Experiment finished dry run =====================");
    return new ExperimentResults(builder,
        ImmutableSet.<Experiment.SimulationResult> of());
  }

}
