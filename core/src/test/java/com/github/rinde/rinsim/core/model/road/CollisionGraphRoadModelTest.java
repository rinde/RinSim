/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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

import static com.github.rinde.rinsim.core.TimeLapseFactory.hour;
import static com.github.rinde.rinsim.core.TimeLapseFactory.ms;
import static com.github.rinde.rinsim.geom.PointAssert.assertPointEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.RoundingMode;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.google.common.math.DoubleMath;

/**
 * @author Rinde van Lon
 *
 */
public class CollisionGraphRoadModelTest {
  @SuppressWarnings("null")
  Point SW, SE, NE, NW;
  @SuppressWarnings("null")
  ListenableGraph<LengthData> graph;
  @SuppressWarnings("null")
  CollisionGraphRoadModel model;

  /**
   * Set up a simple squared graph.
   */
  @Before
  public void setUp() {
    graph = new ListenableGraph<>(new TableGraph<LengthData>(LengthData.EMPTY));
    model = CollisionGraphRoadModel.builder(graph).build();
    SW = new Point(0, 0);
    SE = new Point(10, 0);
    NE = new Point(10, 10);
    NW = new Point(0, 10);
    Graphs.addBiPath(graph, SW, SE, NE, NW, SW);
    assertEquals(8, graph.getNumberOfConnections());
    assertEquals(4, graph.getNumberOfNodes());
  }

  /**
   * {@link CollisionGraphRoadModel#addObjectAtSamePosition(RoadUser, RoadUser)}
   * is not supported.
   */
  @Test
  public void testAddObjectAtSamePosition() {
    final MovingRoadUser agv1 = new TestRoadUser();
    final MovingRoadUser agv2 = new TestRoadUser();
    model.addObjectAt(agv1, SW);
    boolean fail = false;
    try {
      model.addObjectAtSamePosition(agv2, agv1);
    } catch (final UnsupportedOperationException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Adding an object at the same position is not allowed.
   */
  @Test
  public void testAddObjectAtSamePos() {
    final MovingRoadUser agv1 = new TestRoadUser();
    final MovingRoadUser agv2 = new TestRoadUser();
    model.addObjectAt(agv1, SW);
    boolean fail = false;
    try {
      model.addObjectAt(agv2, SW);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  @Test
  public void testDetectDeadLock() {
    final MovingRoadUser agv1 = new TestRoadUser();
    final MovingRoadUser agv2 = new TestRoadUser();
    model.addObjectAt(agv1, SW);
    model.addObjectAt(agv2, NW);

    model.moveTo(agv1, NW, hour(5));
    boolean fail = false;
    try {
      model.moveTo(agv2, SW, hour(6));
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  @Test
  public void testTailCollisionAvoidance() {
    final MovingRoadUser agv1 = new TestRoadUser();
    final MovingRoadUser agv2 = new TestRoadUser();
    model.addObjectAt(agv1, SW);
    model.addObjectAt(agv2, NW);

    model.moveTo(agv1, SE, meter(3));
    model.moveTo(agv2, SW, meter(1));
    model.moveTo(agv2, SE, meter(20));

    assertPointEquals(new Point(3, 0), model.getPosition(agv1),
        GraphRoadModel.DELTA);
    assertPointEquals(new Point(1.75, 0), model.getPosition(agv2),
        GraphRoadModel.DELTA);
  }

  static TimeLapse meter(double m) {
    return ms(DoubleMath.roundToLong(3600d * m, RoundingMode.HALF_UP));
  }
}
