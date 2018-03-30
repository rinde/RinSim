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
package com.github.rinde.rinsim.central;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.PostProcessors;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.pdptw.common.RouteRenderer;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.StatsStopConditions;
import com.github.rinde.rinsim.pdptw.common.TimeLinePanel;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.collect.ImmutableList;

/**
 *
 * @author Rinde van Lon
 */
public class SolverDelayTest {

  static final double EPSILON = .0000001;

  static final Gendreau06ObjectiveFunction OBJ_FUNC =
    Gendreau06ObjectiveFunction.instance(50d);

  @SuppressWarnings("null")
  Scenario scenario;

  @Before
  public void setUp() {

  }

  @Test
  public void test() {
    final Scenario scen1 = createScenario();
    final Scenario scen2 = createScenario(0, 0, 250);

    final StatisticsDTO stats1 = simulate(scen1);
    final StatisticsDTO stats2 = simulate(scen2);

    final double tt1 = OBJ_FUNC.travelTime(stats1);
    final double tt2 = OBJ_FUNC.travelTime(stats2);
    final double tard1 = OBJ_FUNC.tardiness(stats1);
    final double tard2 = OBJ_FUNC.tardiness(stats2);
    final double ot1 = OBJ_FUNC.overTime(stats1);
    final double ot2 = OBJ_FUNC.overTime(stats2);

    final double twoTicks = 500d / 60000d;
    final double eightTicks = 4 * twoTicks;

    // expected extra travel time is 2 ticks
    assertThat(tt2).isWithin(EPSILON).of(tt1 + twoTicks);
    assertThat(tard2).isWithin(EPSILON).of(tard1 + eightTicks);
    assertThat(ot2).isWithin(EPSILON).of(ot1 + twoTicks);
  }

  static Scenario createScenario(long... delays) {
    final long endTime = 15 * 60 * 1000;
    final VehicleDTO vehicle = VehicleDTO.builder()
      .startPosition(new Point(5, 5))
      .availabilityTimeWindow(TimeWindow.create(0, endTime))
      .build();

    final Scenario.Builder scenario = Scenario.builder()
      .addEvent(AddDepotEvent.create(-1, new Point(5, 5)))
      .addEvent(AddVehicleEvent.create(-1, vehicle))
      .addEvent(AddVehicleEvent.create(-1, vehicle))
      .addEvent(TimeOutEvent.create(endTime))
      .addModel(PDPRoadModel.builder(RoadModelBuilders.plane())
        .withAllowVehicleDiversion(true))
      .addModel(DefaultPDPModel.builder())
      .addModel(TimeModel.builder().withTickLength(250))
      .setStopCondition(StopConditions.and(
        StatsStopConditions.vehiclesDoneAndBackAtDepot(),
        StatsStopConditions.timeOutEvent()));

    final long[] dls = new long[3];
    System.arraycopy(delays, 0, dls, 0, delays.length);

    scenario
      .addEvent(createParcel(0, dls[0], new Point(1, 1), new Point(9, 1)));
    scenario
      .addEvent(createParcel(1, dls[1], new Point(1, 2), new Point(9, 2)));
    scenario
      .addEvent(createParcel(2, dls[2], new Point(9, 9), new Point(1, 9)));

    return scenario.build();
  }

  static AddParcelEvent createParcel(int i, long delay, Point p1, Point p2) {
    final long announceTime = (i + 1) * 60 * 1000;
    return AddParcelEvent.create(
      Parcel.builder(p1, p2)
        .orderAnnounceTime(announceTime + delay)
        .pickupTimeWindow(
          TimeWindow.create(announceTime + delay,
            announceTime + 1 * 60 * 1000))
        .deliveryTimeWindow(
          TimeWindow.create(announceTime + delay + 5 * 60 * 1000,
            announceTime + 6 * 60 * 1000))
        .serviceDuration(5 * 60 * 1000)
        .buildDTO());
  }

  static StatisticsDTO simulate(Scenario scenario) {
    final ExperimentResults results =
      Experiment.builder()
        .addScenario(scenario)
        .addConfiguration(Central.solverConfiguration(
          StochasticSuppliers.constant(TestSolvers.lazyInsertion())))
        .withThreads(1)
        .usePostProcessor(PostProcessors.statisticsPostProcessor(OBJ_FUNC))
        .showGui(View.builder()
          .withAutoPlay()
          // .withAutoClose()
          .withSpeedUp(4)
          // .withFullScreen()
          .withTitleAppendix("AAMAS 2016 Experiment")
          .with(RoadUserRenderer.builder()
            .withToStringLabel())
          .with(PDPModelRenderer.builder())
          .with(RouteRenderer.builder())
          .with(PlaneRoadModelRenderer.builder())
          .with(TimeLinePanel.builder())
          .withResolution(1280, 1024))
        .showGui(false)
        .perform();

    final SimulationResult res = results.getResults().iterator().next();

    final StatisticsDTO stats = (StatisticsDTO) res.getResultObject();
    return stats;
  }

  static class TestSolver implements Solver {
    @Override
    public ImmutableList<ImmutableList<Parcel>> solve(GlobalStateObject state)
        throws InterruptedException {

      final ImmutableList.Builder<ImmutableList<Parcel>> schedule =
        ImmutableList.builder();
      for (int i = 0; i < state.getVehicles().size(); i++) {
        schedule.add(ImmutableList.<Parcel>of());
      }
      return schedule.build();
    }
  }

}
