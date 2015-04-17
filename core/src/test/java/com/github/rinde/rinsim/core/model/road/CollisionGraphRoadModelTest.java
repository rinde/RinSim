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
package com.github.rinde.rinsim.core.model.road;

import static com.github.rinde.rinsim.core.model.time.TimeLapseFactory.ms;
import static com.github.rinde.rinsim.geom.PointAssert.assertPointEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.math.RoundingMode;

import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.google.common.math.DoubleMath;

/**
 * Tests for {@link CollisionGraphRoadModel}.
 * @author Rinde van Lon
 */
public class CollisionGraphRoadModelTest {
  @SuppressWarnings("null")
  Point NW, NE, SE, SW;
  @SuppressWarnings("null")
  ListenableGraph<LengthData> graph;
  @SuppressWarnings("null")
  CollisionGraphRoadModel model;

  /**
   * Set up a simple squared graph.
   */
  @Before
  public void setUp() {
    graph = new ListenableGraph<>(new TableGraph<LengthData>());
    model = RoadModelBuilders.dynamicGraph(graph)
      .avoidCollisions()
      .setDistanceUnit(SI.METER)
      .setVehicleLength(1d)
      .setMinDistance(0)
      .build(mock(DependencyProvider.class));
    NW = new Point(0, 0);
    NE = new Point(10, 0);
    SE = new Point(10, 10);
    SW = new Point(0, 10);
    Graphs.addBiPath(graph, NW, NE, SE, SW, NW);
    assertEquals(8, graph.getNumberOfConnections());
    assertEquals(4, graph.getNumberOfNodes());
  }

  /**
   * Test that <code>addObjectAtSamePosition</code> throws an unsupported
   * operation exception.
   */
  @SuppressWarnings("deprecation")
  @Test
  public void testAddObjectAtSamePosition() {
    final MovingRoadUser agv1 = new TestRoadUser();
    final MovingRoadUser agv2 = new TestRoadUser();
    model.addObjectAt(agv1, NW);
    boolean fail = false;
    try {
      model.addObjectAtSamePosition(agv2, agv1);
    } catch (final UnsupportedOperationException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Adding an object to an occupied node is not allowed, but when the node is
   * free it is allowed.
   */
  @Test
  public void testAddObject() {
    final MovingRoadUser agv1 = new TestRoadUser();
    final MovingRoadUser agv2 = new TestRoadUser();
    model.addObjectAt(agv1, NW);
    boolean fail = false;
    try {
      model.addObjectAt(agv2, NW);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);

    // max distance to travel while still staying within node area
    model.moveTo(agv1, SW, meter(.9997222222));

    fail = false;
    try {
      model.addObjectAt(agv2, NW);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);

    // exiting node area, adding to SW is allowed now
    model.moveTo(agv1, SW, meter(0.0002777777778));
    model.addObjectAt(agv2, NW);
    assertEquals(NW, model.getPosition(agv2));
  }

  /**
   * Test for detection of a dead lock situation between two AGVs. The AGVs
   * drive on the same connection from opposite ends. An
   * {@link IllegalArgumentException} should be thrown at the moment the second
   * AGV tries to enter the connection.
   */
  @Test
  public void testDetectDeadLock() {
    final MovingRoadUser agv1 = new TestRoadUser();
    final MovingRoadUser agv2 = new TestRoadUser();
    model.addObjectAt(agv1, NW);
    model.addObjectAt(agv2, SW);

    model.moveTo(agv1, SW, meter(5));
    boolean fail = false;
    try {
      model.moveTo(agv2, NW, meter(1));
    } catch (final DeadlockException e) {
      assertEquals(SW, e.getConnection().from());
      assertEquals(NW, e.getConnection().to());
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Test for checking that an AGV can not overtake another AGV on the same
   * connection. The AGV that is behind should be forced to stay behind.
   */
  @Test
  public void testHeadTailCollisionAvoidance() {
    final MovingRoadUser agv1 = new TestRoadUser();
    final MovingRoadUser agv2 = new TestRoadUser();
    model.addObjectAt(agv1, NW);
    model.addObjectAt(agv2, SW);

    model.moveTo(agv1, NE, meter(3));
    model.moveTo(agv2, NW, meter(1));
    model.moveTo(agv2, NE, meter(20));

    assertPointEquals(new Point(3, 0), model.getPosition(agv1),
      GraphRoadModel.DELTA);
    assertPointEquals(new Point(2, 0), model.getPosition(agv2),
      GraphRoadModel.DELTA);

    // moving is not allowed
    checkNoMovement(model.moveTo(agv2, NE, meter(20)));

    // when the object is removed, moving is allowed
    model.removeObject(agv1);
    model.moveTo(agv2, NE, meter(20));
    assertPointEquals(NE, model.getPosition(agv2), GraphRoadModel.DELTA);
  }

  /**
   * Test for avoidance of a collision on a node. When one AGV comes near ( < of
   * its length/2) a node, this node becomes blocked for any other node.
   */
  @Test
  public void testNodeCollisionAvoidance() {
    final Point X = new Point(0, -10);
    model.getGraph().addConnection(NW, X);

    final MovingRoadUser agv1 = new TestRoadUser("agv1");
    final MovingRoadUser agv2 = new TestRoadUser("agv2");
    model.addObjectAt(agv1, NE);
    model.addObjectAt(agv2, SW);

    // still outside the node (but very very close!)
    model.moveTo(agv1, NW, meter(9));
    assertFalse(model.isOccupied(NW));
    // this represents the smallest travelable distance to come within the area
    // of a node.
    final TimeLapse tl = meter(.0002777777778);
    assertEquals(1L, tl.getTimeLeft());
    // agv1 is within the node area of NW now.
    model.moveTo(agv1, NW, tl);
    assertTrue(model.isOccupied(NW));

    model.moveTo(agv2, NW, meter(10));

    assertPointEquals(new Point(1d - 0.0002777777778, 0),
      model.getPosition(agv1), GraphRoadModel.DELTA);
    assertPointEquals(new Point(0, 1.0), model.getPosition(agv2),
      GraphRoadModel.DELTA);

    // moving agv2 is not allowed
    checkNoMovement(model.moveTo(agv2, NW, meter(20)));

    // agv1 moves to center of node, moving agv2 is still not allowed
    model.moveTo(agv1, NW, meter(1));
    assertEquals(NW, model.getPosition(agv1));
    checkNoMovement(model.moveTo(agv2, NW, meter(20)));

    // this represents the maximum distance to travel while still staying within
    // the node's area, moving agv2 is still not allowed
    final TimeLapse tl2 = meter(.9997222222);
    assertEquals(3599L, tl2.getTimeLeft());
    model.moveTo(agv1, X, tl2);
    checkNoMovement(model.moveTo(agv2, NW, meter(20)));

    // this represents the distance to travel to get outside of the node's area,
    // moving agv2 is now allowed
    final TimeLapse tl3 = meter(0.0002777777778);
    assertEquals(1L, tl3.getTimeLeft());
    model.moveTo(agv1, X, tl3);
    assertPointEquals(new Point(0, -1), model.getPosition(agv1),
      GraphRoadModel.DELTA);
    model.moveTo(agv2, NW, meter(1));
    assertEquals(NW, model.getPosition(agv2));
  }

  /**
   * Tests valid and invalid values for vehicleLength.
   */
  @Test
  public void testBuilderVehicleLength() {
    // vehicle length must be > 0
    boolean fail = false;
    try {
      RoadModelBuilders.dynamicGraph(graph)
        .avoidCollisions()
        .setVehicleLength(0d);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);

    // vehicle length may not be infinite
    fail = false;
    try {
      RoadModelBuilders.dynamicGraph(graph)
        .avoidCollisions()
        .setVehicleLength(Double.POSITIVE_INFINITY);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);

    final CollisionGraphRoadModel cgr1 = RoadModelBuilders.dynamicGraph(graph)
      .avoidCollisions()
      .setDistanceUnit(SI.METER)
      .setVehicleLength(5d)
      .build(mock(DependencyProvider.class));
    assertEquals(5d, cgr1.getVehicleLength(), 0);
  }

  /**
   * Tests valid and invalid values for minDistance.
   */
  @Test
  public void testBuilderMinDistance() {
    assertEquals(0d, RoadModelBuilders.dynamicGraph(graph)
      .avoidCollisions()
      .setMinDistance(0d)
      .setDistanceUnit(SI.METER)
      .build(mock(DependencyProvider.class))
      .getMinDistance(),
      0);

    assertEquals(2d, RoadModelBuilders.dynamicGraph(graph)
      .setDistanceUnit(SI.METER)
      .avoidCollisions()
      .setMinDistance(2d)
      .build(mock(DependencyProvider.class))
      .getMinDistance(),
      0);

    // min distance may not be > 2 * vehicle length
    boolean fail = false;
    try {
      RoadModelBuilders.dynamicGraph(graph)
        .avoidCollisions()
        .setVehicleLength(1d)
        .setMinDistance(2.000000001)
        .build(mock(DependencyProvider.class));
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);

    // min distance may not be negative
    fail = false;
    try {
      RoadModelBuilders.dynamicGraph(graph)
        .avoidCollisions()
        .setMinDistance(-1d);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Some graphs are not compatible with {@link CollisionGraphRoadModel}.
   */
  @Test
  public void testDetectInvalidConnAtConstruction() {
    final ListenableGraph<?> g = new ListenableGraph<>(
      new TableGraph<LengthData>());
    // this connection is allowed:
    g.addConnection(new Point(0, 0), new Point(1, 0));
    // this connection is not allowed:
    g.addConnection(new Point(0, 0), new Point(.99, 0));
    boolean fail = false;
    try {
      RoadModelBuilders.dynamicGraph(g)
        .avoidCollisions()
        .setVehicleLength(1d)
        .setMinDistance(.25)
        .build(mock(DependencyProvider.class));
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Test the addition of a connection that is too short.
   */
  @Test
  public void testDetectAddInvalidConnLive() {
    final Point a = new Point(0, 0);
    final Point b = new Point(1, 0);
    model.getGraph().addConnection(a, b);
    assertTrue(model.getGraph().hasConnection(a, b));

    boolean fail = false;
    try {
      model.getGraph().addConnection(new Point(0, 0), new Point(.99, 0));
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Test a change of a conn such that it becomes too short.
   */
  @Test
  public void testDetectChangeConnInvalidLive() {
    // this is allowed
    graph.setConnectionData(NW, SW, LengthData.create(1d));

    boolean fail = false;
    try {
      // this is too short
      graph.setConnectionData(NW, SW, LengthData.create(0.99));
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Test whether a vehicle is correctly occupying two nodes when it is driving
   * in between them and the nodes are next to each other.
   */
  @Test
  public void testVehicleOccupiesTwoNodes() {
    final Point SSW = new Point(0, 11);
    final Point SSW2 = new Point(0, 12.998);
    final MovingRoadUser agv1 = new TestRoadUser();

    graph.addConnection(SW, SSW);
    graph.addConnection(SSW, SSW2);

    model.addObjectAt(agv1, NW);
    assertTrue(model.isOccupied(NW));
    assertFalse(model.isOccupied(SW));
    assertFalse(model.isOccupied(SSW));
    assertFalse(model.isOccupied(SSW2));

    // nothing is occupied
    model.moveTo(agv1, SSW, meter(9));
    assertFalse(model.isOccupied(NW));
    assertFalse(model.isOccupied(SW));
    assertFalse(model.isOccupied(SSW));
    assertFalse(model.isOccupied(SSW2));

    model.moveTo(agv1, SSW, meter(0.0002777777778));
    assertFalse(model.isOccupied(NW));
    assertTrue(model.isOccupied(SW));
    assertFalse(model.isOccupied(SSW));
    assertFalse(model.isOccupied(SSW2));

    model.moveTo(agv1, SSW, meter(1));
    assertFalse(model.isOccupied(NW));
    assertTrue(model.isOccupied(SW));
    assertTrue(model.isOccupied(SSW));
    assertFalse(model.isOccupied(SSW2));

    model.moveTo(agv1, SSW, meter(.5));
    assertFalse(model.isOccupied(NW));
    assertTrue(model.isOccupied(SW));
    assertTrue(model.isOccupied(SSW));
    assertFalse(model.isOccupied(SSW2));

    model.moveTo(agv1, SSW, meter(.5));
    assertEquals(SSW, model.getPosition(agv1));
    assertFalse(model.isOccupied(NW));
    assertFalse(model.isOccupied(SW));
    assertTrue(model.isOccupied(SSW));
    assertFalse(model.isOccupied(SSW2));

    model.moveTo(agv1, SSW2, meter(.5));
    assertFalse(model.isOccupied(NW));
    assertFalse(model.isOccupied(SW));
    assertTrue(model.isOccupied(SSW));
    assertFalse(model.isOccupied(SSW2));

    // we are at the border between the two nodes. the back of the agv is still
    // occupying SSW while the front is already occupying SSW2
    model.moveTo(agv1, SSW2, meter(.4997222222));
    assertFalse(model.isOccupied(NW));
    assertFalse(model.isOccupied(SW));
    assertTrue(model.isOccupied(SSW));
    assertTrue(model.isOccupied(SSW2));

    // move over the border, no longer occupying SSW
    model.moveTo(agv1, SSW2, meter(.0002777777778));
    assertFalse(model.isOccupied(NW));
    assertFalse(model.isOccupied(SW));
    assertFalse(model.isOccupied(SSW));
    assertTrue(model.isOccupied(SSW2));
  }

  /**
   * Helper function that constructs {@link TimeLapse} instances that allow
   * traveling <code>n</code> meters.
   * @param m
   * @return
   */
  static TimeLapse meter(double m) {
    return ms(DoubleMath.roundToLong(3600d * m, RoundingMode.HALF_UP));
  }

  static void checkNoMovement(MoveProgress mp) {
    assertTrue(mp.travelledNodes().isEmpty());
    assertEquals(0L, mp.time().getValue().longValue());
    assertEquals(0d, mp.distance().getValue().doubleValue(), 0);
  }
}
