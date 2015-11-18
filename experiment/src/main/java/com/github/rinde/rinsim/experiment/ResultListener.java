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

import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.scenario.Scenario;
import com.google.common.collect.ImmutableSet;

/**
 * Implementors get notified of the progress of an {@link Experiment}.
 * @author Rinde van Lon
 */
public interface ResultListener {
  /**
   * This method is called to signal the start of an experiment. The number of
   * simulations equals:
   * <code>|configurations| x |scenarios| x repetitions</code>.
   * @param numberOfSimulations The number of simulations that is going to be
   *          executed.
   * @param configurations The set of configurations that will be used.
   * @param scenarios The set of scenarios.
   * @param repetitions The number of repetitions.
   */
  void startComputing(int numberOfSimulations,
      ImmutableSet<MASConfiguration> configurations,
      ImmutableSet<Scenario> scenarios,
      int repetitions);

  // TODO extend description, the SimulationResult may be a failed simulation
  /**
   * This method is called to signal the completion of a single experiment.
   * @param result The {@link SimulationResult} of the simulation that is
   *          finished.
   */
  void receive(SimulationResult result);

  /**
   * This method is called to signal the end of the experiment.
   */
  void doneComputing();
}
