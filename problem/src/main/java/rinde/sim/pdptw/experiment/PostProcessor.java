package rinde.sim.pdptw.experiment;

import rinde.sim.core.Simulator;

public interface PostProcessor<T> {

  T collectResults(Simulator sim);
}
