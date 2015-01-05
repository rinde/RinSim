package com.github.rinde.rinsim.experiment.base;

import java.util.Set;

interface Computer {

  /**
   * Should compute all simulations as specified by the inputs.
   * @param builder The builder.
   * @param inputs The inputs which define which simulations to compute.
   * @return An instance of {@link ExperimentResults} containing the results.
   */
  ExperimentResults compute(AbstractExperimentBuilder<?> builder, Set<SimArgs> inputs);

}
