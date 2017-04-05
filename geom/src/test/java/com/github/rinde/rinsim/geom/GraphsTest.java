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
package com.github.rinde.rinsim.geom;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.base.Function;

/**
 * @author Rinde van Lon
 *
 */
@RunWith(Parameterized.class)
public class GraphsTest {
  static final double DELTA = 0.0001;

  Graph<LengthData> graph;
  Class<? extends Graph<LengthData>> graphType;

  static final Connection<LengthData> DUMMY = Connection.create(
    new Point(0, 0), new Point(1, 1));

  @SuppressWarnings("null")
  public GraphsTest(Class<? extends Graph<LengthData>> c)
      throws InstantiationException, IllegalAccessException {
    graphType = c;
  }

  @Parameters
  public static Collection<Object[]> configs() {
    return Arrays.asList(new Object[][] {{MultimapGraph.class},
      {TableGraph.class}});
  }

  @Before
  public void setUp() throws InstantiationException, IllegalAccessException {
    graph = graphType.newInstance();
  }

  @Test(expected = IllegalArgumentException.class)
  public void addConnection2() {
    graph.addConnection(new Point(0, 0), new Point(0, 0));
  }

  @Test
  public void shortestPathConsistencyCheck() {
    Point A, B, C, D;
    A = new Point(0, 0);
    B = new Point(0, 10);
    C = new Point(10, 10);
    D = new Point(10, 0);
    Graphs.addBiPath(graph, A, B, C, D, A);

    List<Point> prevPath = Graphs.shortestPathEuclideanDistance(graph, A, C);
    for (int i = 0; i < 100; i++) {
      final List<Point> newPath = Graphs.shortestPathEuclideanDistance(graph,
        A, C);
      assertEquals(prevPath, newPath);
      prevPath = newPath;
    }
  }

  /**
   * In this test there are two paths of equal length between two nodes. The
   * function should always return the same path.
   */
  @Test
  public void shortestPathConsistencyCheck2() {
    Point N, NE, E, SE, S, SW, W, NW;
    N = new Point(0, 5);
    NE = new Point(5, 5);
    E = new Point(5, 0);
    SE = new Point(5, -5);
    S = new Point(0, -5);
    SW = new Point(-5, -5);
    W = new Point(-5, 0);
    NW = new Point(-5, 5);
    Graphs.addBiPath(graph, N, NE, E, SE, S, SW, W, NW);

    List<Point> prevPath = Graphs.shortestPathEuclideanDistance(graph, N, S);
    for (int i = 0; i < 100; i++) {
      final List<Point> newPath = Graphs.shortestPathEuclideanDistance(graph,
        N, S);
      assertEquals(prevPath, newPath);
      prevPath = newPath;
    }
  }
  
  @Test
  public void fastestPathSpeedLimitationTest() {
      final Graph<MultiAttributeData> attributeGraph =
        new TableGraph<>();
      Point A, B, C;
      A = new Point(0, 0);
      B = new Point(0, 1);
      C = new Point(1, 0);
      attributeGraph.addConnection(A, B,
        MultiAttributeData.builder().setMaxSpeed(1).build());
      attributeGraph.addConnection(A, C,
        MultiAttributeData.builder().setMaxSpeed(3).build());
      final GeomHeuristic heuristic = GeomHeuristics.time(0d);

      assertEquals(1d,
        heuristic.calculateTravelTime(attributeGraph, A, B, SI.KILOMETER,
          Measure.valueOf(2d, NonSI.KILOMETERS_PER_HOUR), NonSI.HOUR),
        DELTA);
      assertEquals(0.5d,
        heuristic.calculateTravelTime(attributeGraph, A, C, SI.KILOMETER,
          Measure.valueOf(2d, NonSI.KILOMETERS_PER_HOUR), NonSI.HOUR),
        DELTA);
    }

  /**
   * The shortest path changes based on the connection data.
   */
  @Test
  public void shortestPathConnData() {
    final Point a = new Point(0, 0);
    final Point b = new Point(10, 0);
    final Point c = new Point(5, 5);
    Graphs.addBiPath(graph, a, b, c, a);

    assertEquals(asList(a, b),
      Graphs.shortestPathEuclideanDistance(graph, a, b));

    graph.setConnectionData(a, c, LengthData.create(1d));
    graph.setConnectionData(c, b, LengthData.create(1d));

    assertEquals(asList(a, c, b),
      Graphs.shortestPathEuclideanDistance(graph, a, b));
  }

  /**
   * The fastest path changes based on the maximal allowed speed
   */
  @Test
  public void fastestPathConnData() {
    final Graph<MultiAttributeData> attributeGraph =
      new TableGraph<>();
    Point A, B, C, D;
    A = new Point(0, 0);
    B = new Point(0, 10);
    C = new Point(10, 10);
    D = new Point(10, 0);
    attributeGraph.addConnection(A, B,
      MultiAttributeData.builder().setMaxSpeed(2).build());
    attributeGraph.addConnection(B, C,
      MultiAttributeData.builder().setMaxSpeed(2).build());
    attributeGraph.addConnection(A, D,
      MultiAttributeData.builder().setMaxSpeed(1).build());
    attributeGraph.addConnection(D, C,
      MultiAttributeData.builder().setMaxSpeed(1).build());

    assertEquals(asList(A, B, C),
      Graphs.shortestPath(attributeGraph, A, C, GeomHeuristics.time(50d)));

    attributeGraph.setConnectionData(A, D,
      MultiAttributeData.builder().setMaxSpeed(10).build());
    attributeGraph.setConnectionData(D, C,
      MultiAttributeData.builder().setMaxSpeed(10).build());

    assertEquals(asList(A, D, C),
      Graphs.shortestPath(attributeGraph, A, C, GeomHeuristics.time(50d)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shortestPathNull() {
    Graphs.shortestPathEuclideanDistance(graph, null, new Point(2, 3));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shortestPathNotExistingPoint() {
    Graphs.shortestPathEuclideanDistance(graph, new Point(1, 2),
      new Point(2, 3));
  }

  @Test(expected = PathNotFoundException.class)
  public void noShortestPath() {
    final Point from = new Point(0, 0);
    Graphs.addBiPath(graph, from, new Point(1, 0));
    final Point to = new Point(10, 0);
    Graphs.addBiPath(graph, to, new Point(9, 0));
    Graphs.shortestPathEuclideanDistance(graph, from, to);
  }

  @Test
  public void connectionOrder() {
    Point N, NE, E, SE, S, SW, W, NW;
    N = new Point(0, 5);
    NE = new Point(5, 5);
    E = new Point(5, 0);
    SE = new Point(5, -5);
    S = new Point(0, -5);
    SW = new Point(-5, -5);
    W = new Point(-5, 0);
    NW = new Point(-5, 5);
    Graphs.addPath(graph, N, NE, E, SE, S, SW, W, NW);
    final List<Point> points = Arrays.asList(N, NE, E, SE, S, SW, W, NW);

    final List<Connection<LengthData>> connections = newArrayList(graph
      .getConnections());
    for (int i = 1; i < points.size(); i++) {
      assertSame(connections.get(i - 1).from(), points.get(i - 1));
      assertSame(connections.get(i - 1).to(), points.get(i));
    }
  }

  @Test
  public void incomingConnectionsOrder() {
    final Point incoming = new Point(0, 0);
    final Point p0 = new Point(1, 0);
    final Point p1 = new Point(2, 0);
    final Point p2 = new Point(3, 0);
    final Point p3 = new Point(4, 0);
    final Point p4 = new Point(5, 0);
    final Point p5 = new Point(6, 0);

    final List<Point> points = Arrays.asList(p0, p1, p2, p3, p4, p5);
    for (final Point p : points) {
      graph.addConnection(p, incoming);
    }

    final List<Point> incomingConn = new ArrayList<Point>(
      graph.getIncomingConnections(incoming));
    for (int i = 0; i < incomingConn.size(); i++) {
      assertSame(incomingConn.get(i), points.get(i));
    }
  }

  @Test
  public void outgoingConnectionsOrder() {
    final Point outgoing = new Point(0, 0);
    final Point p0 = new Point(1, 0);
    final Point p1 = new Point(2, 0);
    final Point p2 = new Point(3, 0);
    final Point p3 = new Point(4, 0);
    final Point p4 = new Point(5, 0);
    final Point p5 = new Point(6, 0);

    final List<Point> points = Arrays.asList(p0, p1, p2, p3, p4, p5);
    for (final Point p : points) {
      graph.addConnection(outgoing, p);
    }

    final List<Point> outgoingConn = new ArrayList<Point>(
      graph.getOutgoingConnections(outgoing));
    for (int i = 0; i < outgoingConn.size(); i++) {
      assertSame(outgoingConn.get(i), points.get(i));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void removeConnectionFail() {
    graph.removeConnection(new Point(0, 0), new Point(1, 0));
  }

  @Test
  public void isEmtpy() {
    assertTrue(graph.isEmpty());
    graph.addConnection(new Point(0, 0), new Point(1, 0));
    assertFalse(graph.isEmpty());
    graph.removeConnection(new Point(0, 0), new Point(1, 0));
    assertTrue(graph.isEmpty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void connectionLengthFail() {
    graph.connectionLength(new Point(0, 3), new Point(4, 5));
  }

  @Test
  public void connDataUsage() {
    final Point A = new Point(0, 0), B = new Point(0, 1), C = new Point(1, 0);

    graph.addConnection(A, B);
    graph.addConnection(Connection.create(B, A, LengthData.create(1.5)));
    graph.addConnection(B, C, LengthData.create(2));
    // explicit empty value
    graph.addConnection(A, C);

    assertFalse("existing but empty", graph.connectionData(A, B).isPresent());
    assertFalse("non existing", graph.connectionData(C, A).isPresent());

    assertTrue("existing B->A", graph.connectionData(B, A).isPresent());
    assertTrue("existing B->C", graph.connectionData(B, C).isPresent());

    // use of the connection data
    assertEquals(1, graph.connectionLength(A, B), DELTA);
    assertEquals(1.5, graph.connectionLength(B, A), DELTA);
    assertEquals(2, graph.connectionLength(B, C), DELTA);
    try {
      graph.connectionLength(C, B);
      fail();
    } catch (final IllegalArgumentException e) {}

  }

  @Test
  public void equalsTest() {
    assertFalse(graph.equals(new Object()));
    assertEquals(graph, graph);

    final Point N = new Point(0, 5);
    final Point E = new Point(5, 0);
    final Point S = new Point(0, -5);
    final Point W = new Point(-5, 0);

    Graphs.addBiPath(graph, N, E, S, W, N);
    assertEquals(graph, graph);

    final Graph<LengthData> g1 = new TableGraph<>();
    g1.merge(graph);
    assertEquals(g1, graph);

    final Graph<LengthData> g2 = new MultimapGraph<>();
    g2.merge(graph);
    assertEquals(g2, graph);
    assertEquals(g1, g2);

    g1.removeConnection(N, E);
    assertFalse(g1.equals(graph));

    g1.removeNode(N);
    assertFalse(g1.equals(graph));

    final Point C = new Point(0, 0);
    Graphs.addBiPath(g1, W, C, E);
    assertFalse(g1.equals(graph));

    graph.removeConnection(N, E);
    graph.addConnection(N, E, LengthData.create(10));
    assertFalse(g1.equals(graph));
    assertFalse(graph.equals(g1));

    final Graph<LengthData> g3 = new TableGraph<>();
    g3.merge(graph);
    assertEquals(graph, g3);

    g3.removeConnection(N, E);
    g3.addConnection(N, E, LengthData.create(9));
    assertFalse(g3.equals(graph));

    assertFalse(g2.equals(graph));
    assertFalse(g2.equals(g3));
    assertFalse(graph.equals(g2));
    assertFalse(g3.equals(g2));

  }

  @Test
  public void closestObjectsTest() {
    final Function<Point, Point> f = new Function<Point, Point>() {
      @Override
      public Point apply(Point input) {
        return input;
      }
    };

    final List<Point> points = Arrays.asList(new Point(10, 34), new Point(234,
      2),
      new Point(10, 10), new Point(1, 1));

    final List<Point> results = Graphs.findClosestObjects(new Point(0, 0),
      points, f, 2);
    assertEquals(results.size(), 2);
    assertEquals(new Point(1, 1), results.get(0));
    assertEquals(new Point(10, 10), results.get(1));

    final List<Point> results2 = Graphs.findClosestObjects(new Point(0, 0),
      points, f, 5);
    assertEquals(results2.size(), 4);
    assertEquals(new Point(1, 1), results2.get(0));
    assertEquals(new Point(10, 10), results2.get(1));
    assertEquals(new Point(10, 34), results2.get(2));
    assertEquals(new Point(234, 2), results2.get(3));

  }

  @Test(expected = IllegalArgumentException.class)
  public void nonExistingConnection() {
    graph.getConnection(new Point(1, 2), new Point(2, 3));
  }

  @Test
  public void testRandomNode() {
    final RandomGenerator rnd = new MersenneTwister(456);
    for (int i = 0; i < 500; i++) {
      Graphs.addBiPath(graph, new Point(rnd.nextInt(), rnd.nextInt()),
        new Point(rnd.nextInt(), rnd.nextInt()));
    }
    final Graph<LengthData> unmod = Graphs.unmodifiableGraph(graph);
    final Point p1 = graph.getRandomNode(new MersenneTwister(123));
    final Point p2 = unmod.getRandomNode(new MersenneTwister(123));
    assertEquals(p1, p2);
  }

  @Test(expected = IllegalStateException.class)
  public void randomNodeEmptyGraph() {
    graph.getRandomNode(new MersenneTwister(234));
  }

  @Test
  public void unmodifiable() {
    final Point N = new Point(0, 5);
    final Point E = new Point(5, 0);
    final Point S = new Point(0, -5);
    final Point W = new Point(-5, 0);

    Graphs.addBiPath(graph, N, E, S, W, N);
    final Graph<LengthData> g = Graphs.unmodifiableGraph(graph);
    g.hashCode();

    assertEquals(graph, g);
    assertEquals(g, graph);
    assertFalse(g.equals(new Object()));
    assertFalse(g.isEmpty());

    for (final Point p : g.getNodes()) {
      assertArrayEquals(graph.getIncomingConnections(p).toArray(), g
        .getIncomingConnections(p).toArray());
    }

    for (final Connection<LengthData> c : g.getConnections()) {
      assertEquals(graph.connectionLength(c.from(), c.to()),
        g.connectionLength(c.from(), c.to()), DELTA);
    }
  }

  @Test
  public void unmodifiable2() {
    final Point N = new Point(0, 5);
    final Point E = new Point(5, 0);
    final Point S = new Point(0, -5);
    final Point W = new Point(-5, 0);

    Graphs.addBiPath(graph, N, E, S, W, N);
    final Graph<LengthData> unmod = Graphs.unmodifiableGraph(graph);

    graph.addConnection(N, S);
    assertEquals(graph.getConnection(N, S), unmod.getConnection(N, S));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unmodAddConn() {
    Graphs.unmodifiableGraph(graph).addConnection(new Point(1, 2),
      new Point(2, 3));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unmodMerge() {
    Graphs.unmodifiableGraph(graph).merge(null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unmodAddConns() {
    Graphs.unmodifiableGraph(graph).addConnections(null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unmodRemoveNode() {
    Graphs.unmodifiableGraph(graph).removeNode(null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unmodRemoveConnection() {
    Graphs.unmodifiableGraph(graph).removeConnection(null, null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unmodAddConnection() {
    Graphs.unmodifiableGraph(graph).addConnection(null, null, null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unmodAddConnection2() {
    Graphs.unmodifiableGraph(graph).addConnection(null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unmodSetConnData() {
    Graphs.unmodifiableGraph(graph).setConnectionData(null, null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addExistingConnection() {
    final Point N = new Point(0, 5);
    final Point E = new Point(5, 0);
    Graphs.addBiPath(graph, N, E);
    Graphs.addBiPath(graph, N, E);
  }

  @Test
  public void testMultimapGraphConstructor() {
    final RandomGenerator rnd = new MersenneTwister(123);
    final List<Point> path = new ArrayList<Point>();
    for (int i = 0; i < 20; i++) {
      path.add(new Point(rnd.nextInt(50), rnd.nextInt(50)));
    }
    Graphs.addBiPath(graph, path.toArray(new Point[path.size()]));

    final MultimapGraph<LengthData> testGraph = new MultimapGraph<>();
    testGraph.merge(graph);

    final MultimapGraph<LengthData> newGraph = new MultimapGraph<>(
      testGraph.getMultimap());

    assertEquals(testGraph.getMultimap(), newGraph.getMultimap());
  }

  @Test
  public void setConnData() {
    final Point N = new Point(0, 5);
    final Point E = new Point(5, 0);
    final Point S = new Point(0, -5);
    final Point W = new Point(-5, 0);

    Graphs.addBiPath(graph, N, E, S, W, N);
    assertFalse(graph.setConnectionData(N, E, LengthData.create(100))
      .isPresent());
    assertEquals(LengthData.create(100),
      graph.removeConnectionData(N, E).get());
  }

  @Test
  public void removeNode() {
    final Point N = new Point(0, 5);
    final Point E = new Point(5, 0);
    final Point S = new Point(0, -5);
    final Point W = new Point(-5, 0);

    Graphs.addBiPath(graph, N, E, S, W, N);
    final Graph<LengthData> unmod = Graphs.unmodifiableGraph(graph);
    assertEquals(graph, unmod);
    assertEquals(4, graph.getNodes().size());
    assertEquals(8, graph.getConnections().size());
    graph.removeNode(N);
    assertEquals(graph, unmod);
    assertEquals(3, graph.getNodes().size());
    assertEquals(4, graph.getConnections().size());
  }

  @Test
  public void getRandomNodeImpossible() {

    Point A, B, C, D;
    A = new Point(0, 0);
    B = new Point(0, 10);
    C = new Point(10, 10);
    D = new Point(10, 0);
    Graphs.addBiPath(graph, A, B, C, D, A);

    final RandomGenerator rg = new RandomGenerator() {

      @Override
      public void setSeed(long arg0) {

      }

      @Override
      public void setSeed(@Nullable int[] arg0) {

      }

      @Override
      public void setSeed(int arg0) {

      }

      @Override
      public long nextLong() {
        return 0;
      }

      @Override
      public int nextInt(int arg0) {
        return arg0 + 1;
      }

      @Override
      public int nextInt() {
        return 0;
      }

      @Override
      public double nextGaussian() {
        return 0;
      }

      @Override
      public float nextFloat() {
        return 0;
      }

      @Override
      public double nextDouble() {
        return 0;
      }

      @Override
      public void nextBytes(@Nullable byte[] arg0) {}

      @Override
      public boolean nextBoolean() {
        return false;
      }
    };
    boolean flag = false;
    try {
      graph.getRandomNode(rg);
    } catch (final IllegalStateException e) {
      flag = true;
    }
    assertTrue(flag);
  }
}
