package com.github.rinde.rinsim.pdptw.experiment;

import java.util.Set;

import com.github.rinde.rinsim.pdptw.experiment.Experiment.Builder;
import com.github.rinde.rinsim.pdptw.experiment.Experiment.SimArgs;

interface Computer {

  /**
   * Should compute all simulations as specified by the inputs.
   * @param builder The builder.
   * @param inputs The inputs which define which simulations to compute.
   * @return An instance of {@link ExperimentResults} containing the results.
   */
  ExperimentResults compute(Builder builder, Set<SimArgs> inputs);

}
