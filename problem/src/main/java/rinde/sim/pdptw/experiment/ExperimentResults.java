package rinde.sim.pdptw.experiment;

import javax.annotation.Nullable;

import rinde.sim.pdptw.common.ObjectiveFunction;
import rinde.sim.pdptw.experiment.Experiment.Builder;
import rinde.sim.pdptw.experiment.Experiment.SimulationResult;
import rinde.sim.scenario.Scenario;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

/**
 * Value object containing all the results of a single experiment as performed
 * by {@link Builder#perform()}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class ExperimentResults {
  /**
   * The {@link ObjectiveFunction} that was used for this experiment.
   */
  public final ObjectiveFunction objectiveFunction;

  /**
   * The configurations that were used in this experiment.
   */
  public final ImmutableSet<MASConfiguration> configurations;

  /**
   * The scenarios that were used in this experiment.
   */
  public final ImmutableSet<Scenario> scenarios;

  /**
   * Indicates whether the experiment was executed with or without the graphical
   * user interface.
   */
  public final boolean showGui;

  /**
   * The number of repetitions for each run (with a different seed).
   */
  public final int repetitions;

  /**
   * The seed of the master random generator.
   */
  public final long masterSeed;

  /**
   * The list of individual simulation results.
   */
  public final ImmutableSet<SimulationResult> results;

  ExperimentResults(Builder exp, ImmutableSet<SimulationResult> res) {
    objectiveFunction = exp.objectiveFunction;
    configurations = ImmutableSet.copyOf(exp.configurationsSet);
    scenarios = exp.scenariosBuilder.build();
    showGui = exp.showGui;
    repetitions = exp.repetitions;
    masterSeed = exp.masterSeed;
    results = res;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(objectiveFunction, configurations, scenarios,
        showGui, repetitions, masterSeed, results);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == null) {
      return false;
    }
    if (other.getClass() != getClass()) {
      return false;
    }
    final ExperimentResults er = (ExperimentResults) other;
    return Objects.equal(objectiveFunction, er.objectiveFunction)
        && Objects.equal(configurations, er.configurations)
        && Objects.equal(scenarios, er.scenarios)
        && Objects.equal(showGui, er.showGui)
        && Objects.equal(repetitions, er.repetitions)
        && Objects.equal(masterSeed, er.masterSeed)
        && Objects.equal(results, er.results);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("objectiveFunction", objectiveFunction)
        .add("configurations", configurations)
        .add("scenarios", scenarios)
        .add("showGui", showGui)
        .add("repetitions", repetitions)
        .add("masterSeed", masterSeed)
        .add("results", results)
        .toString();
  }
}
