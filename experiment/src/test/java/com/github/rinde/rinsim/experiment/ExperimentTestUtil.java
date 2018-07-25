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

import java.io.Serializable;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.RandomVehicle;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.scenario.IScenario;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 *
 * @author Rinde van Lon
 */
public final class ExperimentTestUtil {

  public static PostProcessor<ImmutableList<Point>> testPostProcessor() {
    return new TestPostProcessor();
  }

  public static PostProcessor<String> retryOncePostProcessor() {
    return new RetryPostProcessor();
  }

  public static TimedEventHandler<AddVehicleEvent> randomVehicle() {
    return VehicleHandler.INSTANCE;
  }

  public static MASConfiguration testConfig(String name) {
    return MASConfiguration.pdptwBuilder()
      .setName(name)
      .addEventHandler(AddVehicleEvent.class,
        ExperimentTestUtil.randomVehicle())
      .build();
  }

  public static Simulator init(IScenario scenario,
      MASConfiguration config, long seed) {
    return Experiment.init(scenario, config, seed, false,
      Optional.<ModelBuilder<?, ?>>absent());
  }

  public static Simulator init(IScenario scenario,
      MASConfiguration config, long seed, boolean showGui,
      ModelBuilder<?, ?> guiBuilder) {
    return Experiment.init(scenario, config, seed, showGui,
      Optional.<ModelBuilder<?, ?>>of(guiBuilder));
  }

  public static StatisticsDTO singleRun(IScenario scenario,
      MASConfiguration c, long seed, ObjectiveFunction objFunc,
      boolean showGui) {
    return (StatisticsDTO) Experiment
      .singleRun(scenario, c, seed, showGui,
        PostProcessors.statisticsPostProcessor(objFunc), null)
      .getResultObject();
  }

  private enum VehicleHandler implements TimedEventHandler<AddVehicleEvent> {
    INSTANCE {
      @Override
      public void handleTimedEvent(AddVehicleEvent event, SimulatorAPI sim) {
        sim.register(new RandomVehicle(event.getVehicleDTO()));
      }
    }
  }

  private static class TestPostProcessor implements
      PostProcessor<ImmutableList<Point>>, Serializable {
    private static final long serialVersionUID = -2166760289557525263L;

    @Override
    public ImmutableList<Point> collectResults(Simulator sim, SimArgs args) {
      final RoadModel rm = sim.getModelProvider().getModel(RoadModel.class);
      return ImmutableList.copyOf(rm.getObjectPositions());
    }

    @Override
    public FailureStrategy handleFailure(Exception e, Simulator sim,
        SimArgs args) {
      return FailureStrategy.ABORT_EXPERIMENT_RUN;
    }
  }

  private static class RetryPostProcessor
      implements PostProcessor<String>, Serializable {
    private static final long serialVersionUID = -7290588629306009527L;
    boolean firstTime = true;

    @Override
    public String collectResults(Simulator sim, SimArgs args) {
      if (firstTime) {
        firstTime = false;
        throw new IllegalStateException("YOLO");
      }
      return "SUCCESS";
    }

    @Override
    public FailureStrategy handleFailure(Exception e, Simulator sim,
        SimArgs args) {
      return FailureStrategy.RETRY;
    }
  }
}
