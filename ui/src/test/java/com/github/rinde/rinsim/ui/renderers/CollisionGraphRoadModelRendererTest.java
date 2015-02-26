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
package com.github.rinde.rinsim.ui.renderers;

import static org.junit.Assert.assertEquals;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;

/**
 * @author Rinde van Lon
 *
 */
public class CollisionGraphRoadModelRendererTest {

  @Test
  public void test2() {

    final Point a = new Point(1, 0);
    final Point b = new Point(3, 0);

    final Point c = new Point(0, 1);
    final Point d = new Point(0, 3);

    final Point z = new Point(0, 0);
    final Point e = new Point(10, 10);
    final Point f = new Point(0, 10);

    System.out.println(0d / 10d);
    System.out.println(1d / 0d);
    assertEquals(new Point(0, 0),
        CollisionGraphRoadModelRenderer.intersectionPoint(a, b, c, d).get());

    assertEquals(z,
        CollisionGraphRoadModelRenderer.intersectionPoint(z, e, f, z).get());

  }

  @Test
  public void test() {

    final Simulator sim = new Simulator(new MersenneTwister(123L),
        Measure.valueOf(1000L, SI.MILLI(SI.SECOND)));

    final ListenableGraph<LengthData> graph = new ListenableGraph<>(
        new TableGraph<LengthData>());

    // graph.addConnection(new Point(0, 0), new Point(0, 10));
    // graph.addConnection(new Point(0, 10), new Point(10, 10));
    graph.addConnection(new Point(0, 0), new Point(10, 10));
    // graph.addConnection(new Point(0, 0), new Point(10, 0));
    Graphs.addPath(graph, new Point(0, 0), new Point(0, 10),
        new Point(10, 10), new Point(10, 0), new Point(0, 0));
    Graphs.addBiPath(graph, new Point(10, 0), new Point(20, 0),
        new Point(20, 10), new Point(10, 10));

    Graphs.addBiPath(graph, new Point(10, 10), new Point(20, 15), new Point(30,
        10), new Point(30, 5), new Point(20, 10));

    sim.register(CollisionGraphRoadModel.builder(graph).build());
    sim.configure();

    // View.create(sim)
    // .with(new CollisionGraphRoadModelRenderer())
    // .show();

  }
}
