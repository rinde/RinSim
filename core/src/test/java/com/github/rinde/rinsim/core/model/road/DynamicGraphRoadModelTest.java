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
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;

/**
 * @author Rinde van Lon
 *
 */
public class DynamicGraphRoadModelTest {

  protected Point SW;
  protected Point SE;
  protected Point NE;
  protected Point NW;

  ListenableGraph<LengthData> graph;
  DynamicGraphRoadModel model;

  @Before
  public void setUp() {
    graph = new ListenableGraph<>(new TableGraph<LengthData>(LengthData.EMPTY));
    model = new DynamicGraphRoadModel(graph);
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
    assertEquals(38d, mp.distance.getValue().doubleValue(),
        GraphRoadModel.DELTA);
    assertEquals(NE, model.getPosition(tru));
    assertEquals(asList(NW, SW, SE, NE), mp.travelledNodes);

    model.moveTo(tru, SE, hour(1));
    // a shortest path to SW is found: [cur -> SE -> SW]
    model.moveTo(tru, SW, hour(1));

    // a new path is created, therefore the shortest path changes to: [cur -> SE
    // -> X -> SW]
    final Point X = new Point(5, 5);
    graph.addConnection(SE, X, new LengthData(1d));
    graph.addConnection(X, SE, new LengthData(1d));
    graph.addConnection(X, SW, new LengthData(1d));
    graph.addConnection(SW, X, new LengthData(1d));

    final MoveProgress mp2 = model.moveTo(tru, SW, hour(10));

    assertEquals(10d, mp2.distance.getValue().doubleValue(),
        GraphRoadModel.DELTA);
    assertEquals(asList(SE, X, SW), mp2.travelledNodes);
    assertEquals(SW, model.getPosition(tru));

    model.moveTo(tru, NW, hour(10));
    model.moveTo(tru, SW, hour(1));
    model.moveTo(tru, SE, hour(1));
    // right now, the shortest path to SE is: [cur -> SW -> X -> SE].

    graph.setConnectionData(SW, X, new LengthData(10d));
    // connection length is changed so new path becomes: [cur -> SW -> SE].
    final MoveProgress mp3 = model.moveTo(tru, SE, hour(18));

    assertEquals(18d, mp3.distance.getValue().doubleValue(),
        GraphRoadModel.DELTA);
    assertEquals(asList(SW, SE), mp3.travelledNodes);
    assertEquals(SE, model.getPosition(tru));
  }

  @Test
  public void testRemoveConnNotAllowed() {
    boolean fail = false;
    model.addObjectAt(new TestRoadUser(), SW);
    model.getGraph().removeConnection(SW, SE);
    model.getGraph().removeConnection(SE, SW);
    model.getGraph().removeConnection(SW, NW);
    try {
      model.getGraph().removeConnection(NW, SW);
    } catch (final IllegalStateException e) {
      fail = true;
    }
    assertTrue(fail);

    // TODO extend test case with other situations

    // TODO also add a case where a previously occupied connection is removed

    // model.getGraph().addConnection(connection);
  }
}
