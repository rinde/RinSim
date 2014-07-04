package rinde.sim.pdptw.experiment;

import rinde.sim.pdptw.experiment.Experiment.Builder;
import rinde.sim.pdptw.experiment.Experiment.Builder.SimArgs;

interface Computer {

  ExperimentResults compute(Builder builder, Iterable<SimArgs> inputs);

}
