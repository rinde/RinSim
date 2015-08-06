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
package com.github.rinde.rinsim.central.rt;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.github.rinde.rinsim.central.RandomSolver;
import com.github.rinde.rinsim.central.rt.RtCentral.VehicleChecker;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.RandomVehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Parser;
import com.github.rinde.rinsim.testutil.TestUtil;

/**
 * Test for {@link RtCentral}.
 * @author Rinde van Lon
 */
public class RtCentralTest {

  /**
   * Tests that the configuration works and always produces the same result.
   */
  @Test
  public void testConfig() {
    final List<TimedEvent> events = Gendreau06Parser.parse(
      new File("../scenario-util/files/test/gendreau06/req_rapide_1_240_24"))
        .getEvents().subList(0, 20);

    final Scenario s = Scenario.builder(Gendreau06Parser.parse(
      new File("../scenario-util/files/test/gendreau06/req_rapide_1_240_24")))
        .removeModelsOfType(TimeModel.AbstractBuilder.class)
        .addModel(TimeModel.builder()
            .withRealTime()
            .withStartInClockMode(ClockMode.SIMULATED))
        .clearEvents()
        .addEvents(events)
        .addEvent(TimeOutEvent.create(3 * 60 * 60 * 1000))
        .build();

    final ExperimentResults er = Experiment
        .build(Gendreau06ObjectiveFunction.instance())
        .addScenario(s)
        .withThreads(1)
        .addConfiguration(
          RtCentral.solverConfigurationAdapt(RandomSolver.supplier(), ""))
        .perform();

    final double objVal = Gendreau06ObjectiveFunction.instance()
        .computeCost(er.getResults().asList().get(0).getStats());
    assertThat(objVal).isWithin(0.0001).of(495.4718);
  }

  /**
   * Tests that only correct vehicles are allowed.
   */
  @Test
  public void testVehicleConstraintCheck() {
    TestUtil.testEnum(VehicleChecker.class);
    final Simulator sim = Simulator.builder()
        .addModel(TimeModel.builder().withRealTime())
        .addModel(PDPRoadModel.builder(RoadModelBuilders.plane()))
        .addModel(DefaultPDPModel.builder())
        .addModel(RtCentral.builderAdapt(RandomSolver.supplier()))
        .build();

    sim.register(new Depot(new Point(1, 1)));

    boolean fail = false;
    try {
      sim.register(new RandomVehicle(VehicleDTO.builder().build()));
    } catch (final IllegalArgumentException e) {
      fail = true;
      assertThat(e.getMessage())
          .contains("requires that all registered vehicles are a subclass of");
    }
    assertThat(fail).isTrue();

    fail = false;
    try {
      sim.register(
        new RouteFollowingVehicle(VehicleDTO.builder().build(), false));
    } catch (final IllegalArgumentException e) {
      fail = true;
      assertThat(e.getMessage()).contains("allow delayed route changing");
    }
    assertThat(fail).isTrue();
  }
}
