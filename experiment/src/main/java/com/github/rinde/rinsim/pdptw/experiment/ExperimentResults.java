/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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
package com.github.rinde.rinsim.pdptw.experiment;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.experiment.Experiment.Builder;
import com.github.rinde.rinsim.pdptw.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.scenario.Scenario;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

/**
 * Value object containing all the results of a single experiment as performed
 * by {@link Builder#perform()}.
 * @author Rinde van Lon 
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
   * The set of individual simulation results. Note that this set has an
   * undefined iteration order, if you want a sorted view on the results use
   * {@link #sortedResults()}.
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

  /**
   * @return A unmodifiable {@link List} containing the results sorted by its
   *         comparator.
   */
  public List<SimulationResult> sortedResults() {
    List<SimulationResult> list = newArrayList(results);
    Collections.sort(list);
    return Collections.unmodifiableList(list);
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
