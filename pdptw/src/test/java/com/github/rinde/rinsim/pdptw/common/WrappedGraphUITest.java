/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.pdptw.common;

import java.math.RoundingMode;
import java.util.Collections;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.RandomVehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioController;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.math.DoubleMath;

/**
 *
 * @author Rinde van Lon
 */
public class WrappedGraphUITest {

  Point N = new Point(5, 0);
  Point NE = new Point(10, 0);
  Point E = new Point(10, 5);
  Point SE = new Point(10, 10);
  Point S = new Point(5, 10);
  Point SW = new Point(0, 10);
  Point W = new Point(0, 5);
  Point NW = new Point(0, 0);
  Point C = new Point(5, 5);

  /**
   * Tests that {@link GraphRoadModelRenderer} can render a
   * {@link PDPGraphRoadModel}.
   */
  @Test
  public void test() {
    final Graph<LengthData> g = new TableGraph<>();
    Graphs.addBiPath(g, N, NE, E, SE, S, SW, W, NW, N);
    Graphs.addBiPath(g, N, C, S);
    Graphs.addBiPath(g, NE, C, SW);
    Graphs.addBiPath(g, NW, C, SE);
    Graphs.addBiPath(g, E, C, W);

    final Scenario.Builder b = Scenario.builder()
      .addModel(
        PDPGraphRoadModel.builderForGraphRm(RoadModelBuilders.staticGraph(g)))
      .addModel(DefaultPDPModel.builder())
      .addEvents(
        Collections
          .nCopies(
            10,
            AddVehicleEvent.create(-1, VehicleDTO
              .builder()
              .startPosition(C)
              .build())));

    final int endTime = 3 * 60 * 60 * 1000;
    final RandomGenerator rng = new MersenneTwister(123);
    for (int i = 0; i < 20; i++) {
      final long announceTime = rng.nextInt(DoubleMath.roundToInt(
        endTime * .8, RoundingMode.FLOOR));
      b.addEvent(AddParcelEvent.create(Parcel
        .builder(g.getRandomNode(rng), g.getRandomNode(rng))
        .orderAnnounceTime(announceTime)
        .pickupTimeWindow(TimeWindow.create(announceTime, endTime))
        .deliveryTimeWindow(TimeWindow.create(announceTime, endTime))
        .neededCapacity(0).buildDTO()));
    }

    b.addEvent(TimeOutEvent.create(endTime))
      .scenarioLength(endTime)
      .setStopCondition(StopConditions.limitedTime(endTime));

    final Simulator sim = Simulator.builder()
      .addModel(ScenarioController.builder(b.build())
        .withEventHandler(AddParcelEvent.class, AddParcelEvent.defaultHandler())
        .withEventHandler(TimeOutEvent.class,
          TimeOutEvent.ignoreHandler())
        .withEventHandler(AddVehicleEvent.class,
          new TimedEventHandler<AddVehicleEvent>() {
            @Override
            public void handleTimedEvent(AddVehicleEvent event,
                SimulatorAPI simulator) {
              simulator
                .register(new RandomVehicle(event.getVehicleDTO()));
            }
          }))
      .addModel(View.builder()
        .withTitleAppendix("Wrapped GraphRoadModel UI test")
        .with(GraphRoadModelRenderer.builder().withMargin(1))
        .with(RoadUserRenderer.builder())
        .withSimulatorEndTime(30 * 1000)
        .withAutoPlay()
        .withAutoClose())
      .build();

    sim.start();

  }

}
