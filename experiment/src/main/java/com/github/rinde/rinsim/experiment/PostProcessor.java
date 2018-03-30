/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;

/**
 * A post-processor should collect results from a {@link Simulator}.
 * @author Rinde van Lon
 * @param <T> The results object type.
 */
public interface PostProcessor<T> {

  /**
   * Collects results from the provided {@link Simulator}.
   * @param sim The simulator.
   * @param args The arguments of the simulation that has just finished.
   * @return An object containing simulation results.
   */
  T collectResults(Simulator sim, SimArgs args);

  /**
   * Is called when an exception occurred during the simulation.
   * @param e The exception that was thrown during simulation.
   * @param sim The simulator object in which the exception was thrown, this
   *          object can be used for querying the simulator state which could
   *          aid diagnosing the problem.
   * @param args The arguments of the simulation.
   * @return The {@link FailureStrategy} indicates how this failure should be
   *         treated.
   */
  FailureStrategy handleFailure(Exception e, Simulator sim, SimArgs args);

  /**
   * A failure strategy indicates what should happen with the experiment in
   * progress when a single simulation fails.
   * @author Rinde van Lon
   */
  enum FailureStrategy {
    /**
     * When a single simulation fails, the entire experiment will be aborted.
     */
    ABORT_EXPERIMENT_RUN,

    /**
     * When a single simulation fails, this simulation will be rerun. This can
     * continue indefinitely.
     */
    RETRY,

    /**
     * When a single simulation fails, the failure is taken as the result and is
     * included in the experiment results.
     */
    INCLUDE
  }
}
