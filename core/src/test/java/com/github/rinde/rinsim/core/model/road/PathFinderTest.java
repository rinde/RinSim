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

import static com.github.rinde.rinsim.geom.Graphs.pathLength;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.core.TimeLapseFactory;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.MultimapGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.github.rinde.rinsim.util.TrivialRoadUser;
import com.google.common.base.Predicate;
import com.google.common.math.DoubleMath;

/**
 * TODO this class should only be testing Graph classes. RoadModel related stuff
 * should be splitted
 * @author Rinde van Lon
 */
@RunWith(Parameterized.class)
public class PathFinderTest {

  @Parameters
  public static Collection<Object[]> configs() {
    return Arrays.asList(new Object[][] { { MultimapGraph.class },
        { TableGraph.class } });
  }

  public PathFinderTest(Class<? extends Graph<LengthData>> clazz) {
    rmType = clazz;
  }

  Class<? extends Graph<LengthData>> rmType;

  private final double EPSILON = 0.02;

  RoadModel rm;
  Graph<?> graph;
  Point a, b, c, d, e, f, g;

  RoadUser o1, o2, o3, o4, o5, o6;
  List<RoadUser> allObjects;

  @Before
  public void setUp() throws InstantiationException, IllegalAccessException {
    graph = rmType.newInstance();
    rm = new GraphRoadModel(graph, SI.KILOMETER, NonSI.KILOMETERS_PER_HOUR);

    a = new Point(0, 0);
    b = new Point(10, 0);
    c = new Point(15, 15);
    d = new Point(15, 20);
    e = new Point(22, 0);
    f = new Point(8, 20);
    g = new Point(0, 12);

    graph.addConnection(a, b);
    graph.addConnection(a, c);
    // rs.addConnection(a, d);

    graph.addConnection(b, a);
    graph.addConnection(b, c);

    graph.addConnection(c, d);
    graph.addConnection(c, e);

    graph.addConnection(d, c);
    graph.addConnection(d, f);

    graph.addConnection(e, b);

    graph.addConnection(f, g);

    graph.addConnection(g, a);

    o1 = new StringRoadUser("object1");
    o2 = new StringRoadUser("object2");
    o3 = new StringRoadUser("object3");
    o4 = new LongRoadUser(444L);
    o5 = new LongRoadUser(555L);
    o6 = new LongRoadUser(666L);
    allObjects = Arrays.asList(o1, o2, o3, o4, o5, o6);

    rm.addObjectAt(o1, a);
    rm.addObjectAt(o2, b);
    rm.addObjectAt(o3, c);
    rm.addObjectAt(o4, d);
    rm.addObjectAt(o5, e);
    rm.addObjectAt(o6, f);

  }

  class StringRoadUser extends TrivialRoadUser {
    public final String name;

    public StringRoadUser(String pName) {
      name = pName;
    }

    @Override
    public String toString() {
      return name;
    }

  }

  class LongRoadUser extends TrivialRoadUser {
    public final long number;

    public LongRoadUser(long pNumber) {
      number = pNumber;
    }

    @Override
    public String toString() {
      return "" + number;
    }
  }

  @Test
  public void shortestDistance() {
    final List<Point> t = Graphs.shortestPathEuclideanDistance(graph, a, d);
    compatibilityCheck(t);
    assertEquals(asList(a, c, d), t);

    final List<Point> t2 = Graphs.shortestPathEuclideanDistance(graph, d, a);
    compatibilityCheck(t2);
    assertEquals(asList(d, f, g, a), t2);

    final List<Point> t3 = Graphs.shortestPathEuclideanDistance(graph, g, e);
    compatibilityCheck(t3);
    assertEquals(asList(g, a, c, e), t3);

    final List<Point> t4 = Graphs.shortestPathEuclideanDistance(graph, a, e);
    compatibilityCheck(t4);
    assertEquals(asList(a, c, e), t4);

    final List<Point> t5 = Graphs.shortestPathEuclideanDistance(graph, a, c);
    compatibilityCheck(t5);
    assertEquals(asList(a, c), t5);

    final List<Point> t6 = Graphs.shortestPathEuclideanDistance(graph, e, g);
    compatibilityCheck(t6);
    assertEquals(asList(e, b, c, d, f, g), t6);
  }

  @Test(expected = RuntimeException.class)
  public void impossiblePath() throws InstantiationException,
      IllegalAccessException {
    final Graph<?> gg = rmType.newInstance();
    final GraphRoadModel roads = new GraphRoadModel(gg, SI.KILOMETER,
        NonSI.KILOMETERS_PER_HOUR);
    gg.addConnection(a, b);
    gg.addConnection(b, c);

    Graphs.shortestPathEuclideanDistance(roads.getGraph(), b, a);
  }

  public void compatibilityCheck(List<Point> t) {
    final MovingRoadUser truck = new TrivialRoadUser();
    rm.addObjectAt(truck, t.get(0));
    final double len = pathLength(t);
    // speed of trivial truck is 1 len per hour thus we need to travel 'len'
    // hours
    final MoveProgress progress = rm.followPath(
        truck,
        new LinkedList<Point>(t),
        TimeLapseFactory.create(NonSI.HOUR, 0,
            DoubleMath.roundToLong(len, RoundingMode.CEILING)));
    assertEquals(len, progress.distance().getValue(), EPSILON);
  }

  @Test
  public void checkRutgerBug() throws InstantiationException,
      IllegalAccessException {
    final Graph<?> g = rmType.newInstance();
    final Point q = new Point(0, 10);
    final Point r = new Point(10, 15);
    final Point s = new Point(10, 5);
    final Point t = new Point(20, 10);

    g.addConnection(q, r);
    g.addConnection(q, s);
    g.addConnection(s, t);

    // DotExporter.saveToDot(graph.getGraph(), "files/test/rutgerbug");

    // this shouldn't fail
    Graphs.shortestPathEuclideanDistance(g, q, t);
  }

  @Test
  public void findClosestObjectTest() {
    // actually, o1 and o2 have the same distance to ref (but o1 is added
    // earlier)
    assertEquals(o1, RoadModels.findClosestObject(new Point(5, 5), rm));
    assertEquals(o2, RoadModels.findClosestObject(new Point(5.000001, 5), rm));

    assertEquals(o6,
        RoadModels.findClosestObject(new Point(5, 5), rm, LongRoadUser.class));

    assertEquals(null, RoadModels.findClosestObject(new Point(5, 5), rm,
        new Predicate<RoadUser>() {
          @Override
          public boolean apply(RoadUser input) {
            return false;
          }
        }));
  }

  @Test
  public void findClosestObjectsTest() {
    assertEquals(Arrays.asList(o1, o2, o3, o6, o5, o4),
        RoadModels.findClosestObjects(new Point(5, 5), rm));
    assertEquals(Arrays.asList(o1, o2, o3),
        RoadModels.findClosestObjects(new Point(5, 5), rm, 3));
    assertEquals(Arrays.asList(o6, o5, o4), RoadModels.findClosestObjects(
        new Point(5, 5), rm, LongRoadUser.class, 300));
    assertEquals(Arrays.asList(), RoadModels.findClosestObjects(
        new Point(5, 5), rm, EmptyRoadUser.class, 1));

    assertEquals(Arrays.asList(o3, o6, o4, o5), RoadModels.findClosestObjects(
        new Point(8, 8), rm, new Predicate<RoadUser>() {
          @Override
          public boolean apply(RoadUser input) {
            return input instanceof LongRoadUser
                || rm.getPosition(input).equals(new Point(15, 15));
          }
        }, 99));
  }

  /**
   * Test non positive n value.
   */
  @Test(expected = IllegalArgumentException.class)
  public void findClosestObjectsTestFail() {
    RoadModels.findClosestObjects(new Point(5, 5), rm, 0);
  }

  @Test
  public void findObjectsWithinRadiusTest() {
    final Point ref = new Point(10, 10);
    assertArrayEquals(asList(o1, o2, o3, o4, o6).toArray(), RoadModels
        .findObjectsWithinRadius(ref, rm, 15).toArray());
    assertArrayEquals(asList(o3).toArray(),
        RoadModels.findObjectsWithinRadius(ref, rm, 10).toArray());
    assertArrayEquals(asList(o2, o3).toArray(), RoadModels
        .findObjectsWithinRadius(ref, rm, 10.000000001).toArray());
    assertArrayEquals(asList().toArray(),
        RoadModels.findObjectsWithinRadius(ref, rm, 5).toArray());

    assertArrayEquals(asList(o1, o2, o3).toArray(), RoadModels
        .findObjectsWithinRadius(ref, rm, 15, StringRoadUser.class).toArray());
    assertArrayEquals(asList(o3).toArray(),
        RoadModels.findObjectsWithinRadius(ref, rm, 10, StringRoadUser.class)
            .toArray());
    assertArrayEquals(allObjects.toArray(),
        RoadModels.findObjectsWithinRadius(ref, rm, 30, RoadUser.class)
            .toArray());
    assertArrayEquals(asList().toArray(),
        RoadModels.findObjectsWithinRadius(ref, rm, 30, EmptyRoadUser.class)
            .toArray());

  }

  class EmptyRoadUser implements RoadUser {
    @Override
    public void initRoadUser(RoadModel model) {
      throw new RuntimeException("NOT meant to be executed");
    }
  }
}
