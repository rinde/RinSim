package rinde.sim.pdptw.experiment;

import java.util.Set;

import rinde.sim.pdptw.experiment.Experiment.Builder;
import rinde.sim.pdptw.experiment.Experiment.Builder.SimArgs;

interface Computer {

  ExperimentResults compute(Builder builder, Set<SimArgs> inputs);

}
