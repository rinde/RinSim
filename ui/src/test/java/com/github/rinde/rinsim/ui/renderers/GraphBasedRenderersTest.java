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
package com.github.rinde.rinsim.ui.renderers;

import javax.measure.unit.SI;

import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.github.rinde.rinsim.testutil.GuiTests;
import com.github.rinde.rinsim.ui.View;
import com.google.common.base.Optional;

/**
 * @author Rinde van Lon
 *
 */
@Category(GuiTests.class)
public class GraphBasedRenderersTest {

  /**
   * Tests graph related renderers.
   */
  @Test
  public void testGraphRenderers() {
    final ListenableGraph<LengthData> graph = new ListenableGraph<>(
      new TableGraph<LengthData>());

    Graphs.addPath(graph, new Point(0, 0), new Point(10, 0), new Point(10,
      10), new Point(0, 10), new Point(0, 0));

    final Simulator sim = Simulator.builder()
      .addModel(RoadModelBuilders.dynamicGraph(graph)
        .withCollisionAvoidance()
        .withDistanceUnit(SI.METER))
      .addModel(
        View.builder()
          .with(WarehouseRenderer.builder()
            .withMargin(0)
            .withOneWayStreetArrows()
            .withNodeOccupancy()
            .withNodes())
          .with(AGVRenderer.builder()
            .withVehicleCreationNumber()
            .withDifferentColorsForVehicles()
            .withVehicleCoordinates()
            .withVehicleOrigin())
          .with(GraphRoadModelRenderer.builder()
            .withDirectionArrows()
            .withNodeCoordinates()
            .withMargin(1)
            .withNodeCircles())
          .withAutoPlay()
          .withSimulatorEndTime(300 * 1000L)
          .withAutoClose()
          .withSpeedUp(8)
          .withTitleAppendix("testGraphRenderers()"))

      .build();

    graph.addConnection(new Point(0, 0), new Point(20, 20));
    Graphs.addPath(graph, new Point(0, 0), new Point(0, 20),
      new Point(20, 20), new Point(20, 0), new Point(0, 0));
    Graphs.addBiPath(graph, new Point(20, 0), new Point(40, 0),
      new Point(40, 20), new Point(20, 20));

    Graphs.addBiPath(graph, new Point(20, 20), new Point(40, 35), new Point(60,
      20), new Point(60, 10), new Point(40, 20));

    Graphs.addPath(graph, new Point(60, 10), new Point(60, 6),
      new Point(56, 6),
      new Point(60, 2), new Point(40, 0));

    Graphs.addBiPath(graph, new Point(20, 20), new Point(20, 30), new Point(20,
      40));

    for (int i = 0; i < 4; i++) {
      sim.register(new Agent(sim.getRandomGenerator()));
    }
    sim.register(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        if (timeLapse.getTime() == 100 * 1000L) {
          graph.addConnection(new Point(22, 2), new Point(38, 2));
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });

    sim.start();

  }

  /**
   * Tests whether the renderers update their views when the graph changes.
   */
  @Test
  public void testGraphUpdates() {
    final ListenableGraph<LengthData> graph = new ListenableGraph<>(
      new TableGraph<LengthData>());

    Graphs.addPath(graph, new Point(0, 0), new Point(10, 0), new Point(10,
      10), new Point(0, 10), new Point(0, 0));

    final Simulator sim = Simulator.builder()
      .addModel(RoadModelBuilders.dynamicGraph(graph)
        .withCollisionAvoidance()
        .withDistanceUnit(SI.METER))
      .addModel(
        View.builder()
          .with(WarehouseRenderer.builder()
            .withMargin(0)
            .withOneWayStreetArrows()
            .withNodeOccupancy()
            .withNodes())
          .with(GraphRoadModelRenderer.builder()
            .withDirectionArrows()
            .withNodeCoordinates()
            .withMargin(1)
            .withNodeCircles())
          .withAutoPlay()
          .withAutoClose()
          .withSpeedUp(8)
          .withTitleAppendix("testGraphUpdates()"))

      .build();

    // when developing this test its probably easier to increase the following
    // value so that there is time to inspect the changes.
    final long timeMul = 6000L;

    sim.register(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        if (timeLapse.getTime() == 10 * timeMul) {
          Graphs.addPath(graph, new Point(0, 10), new Point(0, 20),
            new Point(20, 20), new Point(20, 0), new Point(10, 0));
        } else if (timeLapse.getTime() == 16 * timeMul) {
          Graphs.addBiPath(graph, new Point(20, 0), new Point(40, 0),
            new Point(40, 20), new Point(20, 20));
        } else if (timeLapse.getTime() == 20 * timeMul) {
          Graphs.addBiPath(graph, new Point(20, 20), new Point(40, 35),
            new Point(60, 20), new Point(60, 10), new Point(40, 20));
        } else if (timeLapse.getTime() == 25 * timeMul) {
          Graphs.addPath(graph, new Point(60, 10), new Point(60, 6),
            new Point(56, 6), new Point(60, 2), new Point(40, 0));
        } else if (timeLapse.getTime() == 30 * timeMul) {
          Graphs.addBiPath(graph, new Point(20, 20), new Point(20, 30),
            new Point(20, 40));
        } else if (timeLapse.getTime() == 35 * timeMul) {
          Graphs.addBiPath(graph, new Point(0, 0), new Point(-10, 0),
            new Point(-10, -10), new Point(0, -10), new Point(0, 0));
        } else if (timeLapse.getTime() == 40 * timeMul) {
          graph.removeNode(new Point(0, 0));
        } else if (timeLapse.getTime() == 45 * timeMul) {
          graph.addConnection(new Point(20, 40), new Point(20, 400));
        } else if (timeLapse.getTime() == 50 * 60000L) {
          graph.removeConnection(new Point(20, 40), new Point(20, 400));
        } else if (timeLapse.getTime() >= 55 * timeMul) {
          sim.stop();
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });

    sim.start();

  }

  static class Agent implements TickListener, MovingRoadUser {
    Optional<CollisionGraphRoadModel> model;
    Optional<Point> destination;
    RandomGenerator rng;
    boolean haveABreak;

    long timeOut = 0;

    Agent(RandomGenerator r) {
      rng = r;
      model = Optional.absent();
      destination = Optional.absent();
    }

    @Override
    public void initRoadUser(RoadModel m) {
      model = Optional.of((CollisionGraphRoadModel) m);

      Point p;
      while (model.get().isOccupied(p = model.get().getRandomPosition(rng))) {}

      model.get().addObjectAt(this, p);
      destination = Optional.of(model.get().getRandomPosition(rng));
    }

    @Override
    public double getSpeed() {
      return 1;
    }

    @Override
    public void tick(TimeLapse timeLapse) {
      // if (rng.nextInt(100) < 1 || timeOut > 0) {
      // if (timeOut == 0L) {
      // // timeOut = 100L;
      // } else {
      // timeOut--;
      // }
      //
      // } else {
      if (model.get().getPosition(this).equals(destination.get())) {
        destination = Optional.of(model.get().getRandomPosition(rng));
      }
      model.get().moveTo(this, destination.get(), timeLapse);
      // }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

  }
}
