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
import com.github.rinde.rinsim.core.model.pdp.RandomVehicle;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.ScenarioTestUtil;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.StatsTracker;
import com.github.rinde.rinsim.pdptw.common.TestObjectiveFunction;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon
 * 
 */
public class ExperimentTest {

  public static StatisticsDTO singleRun(Scenario scenario,
    MASConfiguration c, long seed, ObjectiveFunction objFunc, boolean showGui) {
    return Experiment.singleRun(scenario, c, seed, objFunc, showGui, null,
      null).stats;
  }

  public static Simulator init(Scenario scenario,
    MASConfiguration config, long seed, boolean showGui) {
    return Experiment.init(scenario, config, seed, showGui, null);
  }

  @Test
  public void testPostProcessor() {
    final Scenario scenario = ScenarioTestUtil.createRandomScenario(123L,
      StatsTracker.builder());
    final Experiment.Builder builder = Experiment
      .build(TestObjectiveFunction.INSTANCE)
      .addScenario(scenario)
      .addConfiguration(testConfig("test"))
      .usePostProcessor(new TestPostProcessor())
      .withRandomSeed(123);

    final ExperimentResults er = builder.perform();
    assertEquals(123, er.masterSeed);
    assertEquals(123, er.results.asList().get(0).seed);

    @SuppressWarnings("unchecked")
    final List<Point> positions = (List<Point>) er.results.asList().get(0).simulationData
      .get();
    assertEquals(10, positions.size());
  }

  /**
   * Checks whether the ordering of results is as expected.
   */
  // FIXME is this test still applicable now that we use sets for the results?
  // i.e. ordering is no longer important
  @Test
  public void multiThreadedOrder() {
    final Experiment.Builder builder = Experiment
      .build(TestObjectiveFunction.INSTANCE)
      .addScenario(
        ScenarioTestUtil.createRandomScenario(456L, StatsTracker.builder()))
      .addConfiguration(testConfig("A"))
      .addConfiguration(testConfig("B"))
      .addConfiguration(testConfig("C"))
      .addConfiguration(testConfig("D"))
      .withThreads(4)
      .withRandomSeed(456);

    final ExperimentResults er = builder.perform();
    assertThat(er.results.asList().get(0).masConfiguration.getName()).endsWith(
      "A");
    assertThat(er.results.asList().get(1).masConfiguration.getName()).endsWith(
      "B");
    assertThat(er.results.asList().get(2).masConfiguration.getName()).endsWith(
      "C");
    assertThat(er.results.asList().get(3).masConfiguration.getName()).endsWith(
      "D");
  }

  static class TestPostProcessor implements
    PostProcessor<ImmutableList<Point>>, Serializable {
    private static final long serialVersionUID = -2166760289557525263L;

    @Override
    public ImmutableList<Point> collectResults(Simulator sim) {
      final RoadModel rm = sim.getModelProvider().getModel(RoadModel.class);
      return ImmutableList.copyOf(rm.getObjectPositions());
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
