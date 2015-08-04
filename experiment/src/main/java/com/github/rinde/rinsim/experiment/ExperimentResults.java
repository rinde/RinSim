/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collections;
import java.util.List;

import com.github.rinde.rinsim.experiment.Experiment.Builder;
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.scenario.Scenario;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

/**
 * Value object containing all the results of a single experiment as performed
 * by {@link Builder#perform()}.
 * @author Rinde van Lon
 */
@AutoValue
public abstract class ExperimentResults {

  /**
   * @return The {@link ObjectiveFunction} that was used for this experiment.
   */
  public abstract ObjectiveFunction getObjectiveFunction();

  /**
   * @return The configurations that were used in this experiment.
   */
  public abstract ImmutableSet<MASConfiguration> getConfigurations();

  /**
   * @return The scenarios that were used in this experiment.
   */
  public abstract ImmutableSet<Scenario> getScenarios();

  /**
   * @return Indicates whether the experiment was executed with or without the
   *         graphical user interface.
   */
  public abstract boolean isShowGui();

  /**
   * @return The number of repetitions for each run (with a different seed).
   */
  public abstract int getRepetitions();

  /**
   * @return The seed of the master random generator.
   */
  public abstract long getMasterSeed();

  /**
   * @return The set of individual simulation results. Note that this set has an
   *         undefined iteration order, if you want a sorted view on the results
   *         use {@link #sortedResults()}.
   */
  public abstract ImmutableSet<SimulationResult> getResults();

  /**
   * @return A unmodifiable {@link List} containing the results sorted by its
   *         comparator.
   */
  public List<SimulationResult> sortedResults() {
    final List<SimulationResult> list = newArrayList(getResults());
    Collections.sort(list);
    return Collections.unmodifiableList(list);
  }

  static ExperimentResults create(Builder exp,
      ImmutableSet<SimulationResult> res) {
    return new AutoValue_ExperimentResults(
        exp.objectiveFunction,
        ImmutableSet.copyOf(exp.configurationsSet),
        exp.scenariosBuilder.build(),
        exp.showGui,
        exp.repetitions,
        exp.masterSeed,
        res);
  }
}
