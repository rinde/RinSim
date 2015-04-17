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

import static com.github.rinde.rinsim.core.model.time.TimeLapseFactory.hour;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;

/**
 * Tests for {@link DynamicGraphRoadModel}.
 * @author Rinde van Lon
 */
public class DynamicGraphRoadModelTest {
  @SuppressWarnings("null")
  Point SW, SE, NE, NW;
  @SuppressWarnings("null")
  ListenableGraph<LengthData> graph;
  @SuppressWarnings("null")
  DynamicGraphRoadModel model;

  /**
   * Set up a simple squared graph.
   */
  @Before
  public void setUp() {
    graph = new ListenableGraph<>(new TableGraph<LengthData>());
    model = RoadModelBuilders.dynamicGraph(graph).build(
      mock(DependencyProvider.class));
    SW = new Point(0, 0);
    SE = new Point(10, 0);
    NE = new Point(10, 10);
    NW = new Point(0, 10);
    Graphs.addBiPath(graph, SW, SE, NE, NW, SW);
    assertEquals(8, graph.getNumberOfConnections());
    assertEquals(4, graph.getNumberOfNodes());
  }

  /**
   * Tests that after removal of a connection the previously computed shortest
   * path of an object is updated.
   */
  @Test
  public void testModifyGraphUpdateShortestPath() {
    final TestRoadUser tru = new TestRoadUser();
    model.addObjectAt(tru, SW);
    model.moveTo(tru, NW, hour(1));

    // a shortest path to NE is found: [cur -> NW -> NE]
    model.moveTo(tru, NE, hour(1));
    // a connection along this path is removed
    graph.removeConnection(NW, NE);
    // therefore the new shortest path towards NE is: [cur -> NW -> SW -> SE ->
    // NE]
    final MoveProgress mp = model.moveTo(tru, NE, hour(38));
    assertEquals(38d, mp.distance().getValue().doubleValue(),
      GraphRoadModel.DELTA);
    assertEquals(NE, model.getPosition(tru));
    assertEquals(asList(NW, SW, SE, NE), mp.travelledNodes());

    model.moveTo(tru, SE, hour(1));
    // a shortest path to SW is found: [cur -> SE -> SW]
    model.moveTo(tru, SW, hour(1));

    // a new path is created, therefore the shortest path changes to: [cur -> SE
    // -> X -> SW]
    final Point X = new Point(5, 5);
    graph.addConnection(SE, X, LengthData.create(1d));
    graph.addConnection(X, SE, LengthData.create(1d));
    graph.addConnection(X, SW, LengthData.create(1d));
    graph.addConnection(SW, X, LengthData.create(1d));

    final MoveProgress mp2 = model.moveTo(tru, SW, hour(10));

    assertEquals(10d, mp2.distance().getValue().doubleValue(),
      GraphRoadModel.DELTA);
    assertEquals(asList(SE, X, SW), mp2.travelledNodes());
    assertEquals(SW, model.getPosition(tru));

    model.moveTo(tru, NW, hour(10));
    model.moveTo(tru, SW, hour(1));
    model.moveTo(tru, SE, hour(1));
    // right now, the shortest path to SE is: [cur -> SW -> X -> SE].

    graph.setConnectionData(SW, X, LengthData.create(10d));
    // connection length is changed so new path becomes: [cur -> SW -> SE].
    final MoveProgress mp3 = model.moveTo(tru, SE, hour(18));

    assertEquals(18d, mp3.distance().getValue().doubleValue(),
      GraphRoadModel.DELTA);
    assertEquals(asList(SW, SE), mp3.travelledNodes());
    assertEquals(SE, model.getPosition(tru));
  }

  /**
   * A corner is occupied, at least one connection to this corner should remain.
   */
  @Test
  public void testRemoveConnCornerOccupied() {
    boolean fail = false;
    model.addObjectAt(new TestRoadUser(), SW);
    model.getGraph().removeNode(SE);
    model.getGraph().removeConnection(SW, NW);
    try {
      model.getGraph().removeConnection(NW, SW);
    } catch (final IllegalStateException e) {
      fail = true;
      // repair
      model.getGraph().addConnection(NW, SW);
    }
    assertTrue(fail);
    fail = false;
    try {
      model.getGraph().removeNode(NW);
    } catch (final IllegalStateException e) {
      // repair
      model.getGraph().addConnection(NW, SW);
      fail = true;
    }
    assertTrue(fail);

    // add another connection
    model.getGraph().addConnection(SW, NE);
    // removal of NW should be allowed now
    model.getGraph().removeNode(NW);
  }

  /**
   * Tests removals when a connection is occupied.
   */
  @Test
  public void testRemoveConEdgeOccupied() {
    final MovingRoadUser user = new TestRoadUser();
    model.addObjectAt(user, SW);
    model.moveTo(user, NW, hour(1));

    // remove everything except the connection with the user on it
    model.getGraph().removeConnection(NW, SW);
    model.getGraph().removeNode(NE);
    model.getGraph().removeNode(SE);
    assertEquals(1, model.getGraph().getConnections().size());
    assertEquals(2, model.getGraph().getNodes().size());

    // attempt removal of occupied conn, should fail
    boolean fail = false;
    try {
      model.getGraph().removeConnection(SW, NW);
    } catch (final IllegalStateException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Tests the removal of a connection which was previously occupied.
   */
  @Test
  public void testRemovePrevOccupiedCon() {
    final MovingRoadUser user = new TestRoadUser();
    model.addObjectAt(user, SW);
    model.moveTo(user, NW, hour(1));
    assertTrue(model.hasRoadUserOn(SW, NW));
    model.moveTo(user, NE, hour(10));
    assertFalse(model.hasRoadUserOn(SW, NW));
    assertTrue(model.hasRoadUserOn(NW, NE));
    model.getGraph().removeConnection(SW, NW);
  }

  /**
   * Test for isOccupied.
   */
  @Test
  public void testIsOccupied() {
    final MovingRoadUser car = new TestRoadUser();
    model.addObjectAt(car, SW);
    assertTrue(model.hasRoadUserOn(SW, NW));
    assertTrue(model.hasRoadUserOn(NW, SW));
    assertTrue(model.hasRoadUserOn(SW, SE));
    assertTrue(model.hasRoadUserOn(SE, SW));
    assertFalse(model.hasRoadUserOn(NE, SE));

    boolean fail = false;
    try {
      model.hasRoadUserOn(SW, NE);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);

    model.moveTo(car, NW, hour(1));
    assertTrue(model.hasRoadUserOn(SW, NW));
    assertFalse(model.hasRoadUserOn(NW, SW));
    assertFalse(model.hasRoadUserOn(SW, SE));
    assertFalse(model.hasRoadUserOn(SE, SW));
    assertFalse(model.hasRoadUserOn(NE, SE));

    model.removeObject(car);
    assertFalse(model.hasRoadUserOn(SW, NW));
  }

  /**
   * Test removal of object.
   */
  @Test
  public void testRemoveObject() {
    final MovingRoadUser car = new TestRoadUser();
    boolean fail = false;
    try {
      model.removeObject(car);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);

    model.addObjectAt(car, NW);
    model.removeObject(car);
    assertTrue(model.getObjects().isEmpty());

    assertFalse(model.hasRoadUserOn(NW, NE));
  }

  /**
   * Test adding of objects at the same position.
   */
  @Test
  public void testAddObjectAtSamePosition() {
    final MovingRoadUser car1 = new TestRoadUser();
    final MovingRoadUser car2 = new TestRoadUser();
    final MovingRoadUser car3 = new TestRoadUser();

    model.addObjectAt(car1, SW);
    assertTrue(model.hasRoadUserOn(SW, SE));

    model.addObjectAtSamePosition(car2, car1);
    assertTrue(model.hasRoadUserOn(SW, SE));

    model.removeObject(car1);
    assertTrue(model.hasRoadUserOn(SW, SE));

    model.moveTo(car2, NW, hour(1));
    assertFalse(model.hasRoadUserOn(SW, SE));
    assertTrue(model.hasRoadUserOn(SW, NW));

    model.addObjectAtSamePosition(car3, car2);
    model.moveTo(car2, NE, hour(10));
    assertTrue(model.hasRoadUserOn(SW, NW));
    assertTrue(model.hasRoadUserOn(NW, NE));

    model.clear();
    assertFalse(model.hasRoadUserOn(SW, NW));
    assertFalse(model.hasRoadUserOn(NW, NE));
  }
}
