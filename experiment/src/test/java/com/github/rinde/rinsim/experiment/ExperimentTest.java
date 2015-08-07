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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.List;

import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.RandomVehicle;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.ScenarioTestUtil;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.StatsTracker;
import com.github.rinde.rinsim.pdptw.common.TestObjectiveFunction;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.testutil.TestUtil;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon
 *
 */
public class ExperimentTest {

  public static StatisticsDTO singleRun(Scenario scenario,
      MASConfiguration c, long seed, ObjectiveFunction objFunc,
      boolean showGui) {
    return (StatisticsDTO) Experiment
        .singleRun(scenario, c, seed, objFunc, showGui,
            PostProcessors.statisticsPostProcessor(), null)
        .getResultObject();
  }

  public static Simulator init(Scenario scenario,
      MASConfiguration config, long seed, boolean showGui) {
    return Experiment.init(scenario, config, seed, showGui, null);
  }

  @Test
  public void testCustomPostProcessor() {
    TestUtil.testEnum(Experiment.Computers.class);

    final Scenario scenario = ScenarioTestUtil.createRandomScenario(123L,
        StatsTracker.builder());
    final Experiment.Builder builder = Experiment
        .build(TestObjectiveFunction.INSTANCE)
        .addScenario(scenario)
        .addConfiguration(testConfig("test"))
        .usePostProcessor(new TestPostProcessor())
        .withRandomSeed(123);

    final ExperimentResults er = builder.perform();
    assertEquals(123, er.getMasterSeed());
    assertEquals(123,
        er.getResults().asList().get(0).getSimArgs().getRandomSeed());

    @SuppressWarnings("unchecked")
    final List<Point> positions =
        (List<Point>) er.getResults().asList().get(0).getResultObject();
    assertEquals(10, positions.size());
  }

  /**
   * Tests default processor.
   */
  @Test
  public void testDefaultPostProcessor() {
    final Scenario scenario = ScenarioTestUtil.createRandomScenario(123L,
        StatsTracker.builder());
    final Experiment.Builder builder = Experiment
        .build(TestObjectiveFunction.INSTANCE)
        .addScenario(scenario)
        .addConfiguration(testConfig("test"))
        .withRandomSeed(123);

    final ExperimentResults er = builder.perform();

    assertThat(er.getResults().asList().get(0).getResultObject())
        .isInstanceOf(String.class);
    assertThat(er.getResults().asList().get(0).getResultObject())
        .isEqualTo("simulation duration: 10801000");
  }

  /**
   * Test correct handling of failure during simulation.
   */
  @Test
  public void testDefaultPostProcessorFailure() {
    final Scenario scenario = ScenarioTestUtil.createRandomScenario(123L,
        StatsTracker.builder(), FailureModel.builder());
    final Experiment.Builder builder = Experiment
        .build(TestObjectiveFunction.INSTANCE)
        .addScenario(scenario)
        .addConfiguration(testConfig("test"))
        .withThreads(1)
        .repeat(3)
        .withRandomSeed(123);

    boolean fail = false;
    try {
      builder.perform();
    } catch (final AbortExperimentException e) {
      assertThat(e.getCause().getMessage()).contains("FailureModel");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  @Test
  public void testRetryPostProcessor() {
    final Scenario scenario = ScenarioTestUtil.createRandomScenario(123L,
        StatsTracker.builder());
    final Experiment.Builder builder = Experiment
        .build(TestObjectiveFunction.INSTANCE)
        .addScenario(scenario)
        .addConfiguration(testConfig("test"))
        .withThreads(1)
        .usePostProcessor(new RetryPostProcessor())
        .repeat(3)
        .withRandomSeed(123);

    builder.perform();
  }

  static class TestPostProcessor implements
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

  static class RetryPostProcessor implements PostProcessor<Object> {

    boolean firstTime = true;

    @Override
    public Object collectResults(Simulator sim, SimArgs args) {
      if (firstTime) {
        firstTime = false;
        throw new IllegalStateException("YOLO");
      }
      return new Object();
    }

    @Override
    public FailureStrategy handleFailure(Exception e, Simulator sim,
        SimArgs args) {
      return FailureStrategy.RETRY;
    }
  }

  static class FailureModel extends AbstractModelVoid implements TickListener {

    FailureModel() {}

    @Override
    public void tick(TimeLapse timeLapse) {
      if (timeLapse.getStartTime() == 5000) {
        throw new IllegalStateException(
            "Hello! Yes? This is the FailureModel speaking.");
      }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {

    }

    static ModelBuilder<?, ?> builder() {
      return new AutoValue_ExperimentTest_FailureModel_FailureModelBuilder();
    }

    @AutoValue
    static class FailureModelBuilder
        extends AbstractModelBuilder<FailureModel, Void> {

      @Override
      public FailureModel build(DependencyProvider dependencyProvider) {
        return new FailureModel();
      }
    }

  }

  public static MASConfiguration testConfig(String name) {
    return MASConfiguration.pdptwBuilder()
        .setName(name)
        .addEventHandler(AddVehicleEvent.class, VehicleHandler.INSTANCE)
        .build();
  }

  enum VehicleHandler implements TimedEventHandler<AddVehicleEvent> {
    INSTANCE {
      @Override
      public void handleTimedEvent(AddVehicleEvent event, SimulatorAPI sim) {
        sim.register(new RandomVehicle(event.getVehicleDTO()));
      }
    }
  }
}
