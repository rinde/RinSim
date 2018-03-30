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

import static com.google.common.base.Preconditions.checkState;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.experiment.PostProcessor.FailureStrategy;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.StatsTracker;

/**
 * Common {@link PostProcessor} implementations.
 * @author Rinde van Lon
 */
public final class PostProcessors {

  private PostProcessors() {}

  public static PostProcessor<Object> defaultPostProcessor() {
    return Default.INSTANCE;
  }

  public static PostProcessor<StatisticsDTO> statisticsPostProcessor(
      ObjectiveFunction objectiveFunction) {
    return new StatisticsPostProcessor(objectiveFunction,
      FailureStrategy.ABORT_EXPERIMENT_RUN);
  }

  public static PostProcessor<StatisticsDTO> statisticsPostProcessor(
      ObjectiveFunction objectiveFunction, FailureStrategy failureStrategy) {
    return new StatisticsPostProcessor(objectiveFunction, failureStrategy);
  }

  static class StatisticsPostProcessor implements PostProcessor<StatisticsDTO> {
    final ObjectiveFunction objectiveFunction;
    final FailureStrategy failureStrategy;

    StatisticsPostProcessor(ObjectiveFunction objFunc, FailureStrategy fs) {
      objectiveFunction = objFunc;
      failureStrategy = fs;
    }

    @Override
    public StatisticsDTO collectResults(Simulator sim, SimArgs args) {
      final StatisticsDTO stats =
        sim.getModelProvider().getModel(StatsTracker.class).getStatistics();
      final boolean isValidResult = objectiveFunction.isValidResult(stats);

      // if we are going to include failures anyway, we shouldn't throw an
      // exception
      if (failureStrategy == FailureStrategy.INCLUDE) {
        return stats;
      }
      checkState(isValidResult,
        "The simulation did not result in a valid result: %s, SimArgs: %s.",
        stats, args);
      return stats;
    }

    @Override
    public FailureStrategy handleFailure(Exception e, Simulator sim,
        SimArgs args) {
      return failureStrategy;
    }

    @Override
    public String toString() {
      return PostProcessors.class.getSimpleName()
        + ".statisticsPostProcessor()";
    }
  }

  enum Default implements PostProcessor<Object> {
    INSTANCE {
      @Override
      public Object collectResults(Simulator sim, SimArgs args) {
        return String.format("simulation duration: %d", sim.getCurrentTime());
      }

      @Override
      public FailureStrategy handleFailure(Exception e, Simulator sim,
          SimArgs args) {
        return FailureStrategy.ABORT_EXPERIMENT_RUN;
      }

      @Override
      public String toString() {
        return PostProcessors.class.getSimpleName() + ".defaultPostProcessor()";
      }
    };
  }
}
