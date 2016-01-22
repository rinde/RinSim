/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.core.model.road;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;

/**
 * Integration test for {@link CollisionGraphRoadModel} and
 * {@link DefaultPDPModel}.
 * @author Rinde van Lon
 */
public class CollisionGraphRoadModelPDPIntegrationTest {
  @SuppressWarnings("null")
  Point NW, NE, SE, SW;
  @SuppressWarnings("null")
  Simulator simulator;
  @SuppressWarnings("null")
  CollisionGraphRoadModel rm;
  @SuppressWarnings("null")
  DefaultPDPModel pm;

  /**
   * Sets up a simple environment.
   */
  @Before
  public void setUp() {
    final ListenableGraph<LengthData> graph = new ListenableGraph<>(
        new TableGraph<LengthData>());
    NW = new Point(0, 0);
    NE = new Point(10, 0);
    SE = new Point(10, 10);
    SW = new Point(0, 10);
    Graphs.addBiPath(graph, NW, NE, SE, SW, NW);
    assertEquals(8, graph.getNumberOfConnections());
    assertEquals(4, graph.getNumberOfNodes());
    simulator = Simulator.builder()
        .setTickLength(1)
        .setTimeUnit(SI.SECOND)
        .addModel(
            RoadModelBuilders.dynamicGraph(graph)
                .withCollisionAvoidance()
                .withVehicleLength(1d)
                .withDistanceUnit(SI.METER)
                .withMinDistance(0)
                .withSpeedUnit(SI.METERS_PER_SECOND))
        .addModel(DefaultPDPModel.builder())
        .build();

    rm = simulator.getModelProvider().getModel(CollisionGraphRoadModel.class);
    pm = simulator.getModelProvider().getModel(DefaultPDPModel.class);
  }

  /**
   * Tests that a vehicle can move to the position of a parcel (i.e. as parcel
   * should be non blocking).
   */
  @Test
  public void testMoveAndPickup() {
    final Parcel parcel1 = Parcel.builder(NW, NE).build();
    final TestAgent agent1 = new TestAgent(VehicleDTO.builder()
        .startPosition(NE)
        .speed(1d)
        .build());

    simulator.register(parcel1);
    simulator.register(agent1);

    assertEquals(NE, rm.getPosition(agent1));
    assertEquals(NW, rm.getPosition(parcel1));

    assertFalse(rm.isOccupied(NW));

    for (int i = 0; i < 9; i++) {
      simulator.tick();
    }

    assertEquals(new Point(1, 0), rm.getPosition(agent1));

    simulator.tick();

    assertEquals(NW, rm.getPosition(parcel1));
    assertEquals(NW, rm.getPosition(agent1));
    assertTrue(pm.getContents(agent1).isEmpty());

    simulator.tick();

    assertFalse(rm.containsObject(parcel1));
    assertEquals(pm.getContents(agent1), newLinkedHashSet(asList(parcel1)));
  }

  class TestAgent extends Vehicle {

    public TestAgent(VehicleDTO pDto) {
      super(pDto);
    }

    @Override
    protected void tickImpl(TimeLapse time) {
      final Parcel p = getPDPModel().getParcels(ParcelState.values())
          .iterator().next();

      getRoadModel().moveTo(this, p, time);

      if (getRoadModel().equalPosition(this, p) && time.hasTimeLeft()) {
        getPDPModel().pickup(this, p, time);
      }
    }

  }
}
