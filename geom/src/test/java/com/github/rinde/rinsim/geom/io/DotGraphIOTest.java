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
package com.github.rinde.rinsim.geom.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.github.rinde.rinsim.geom.io.DotGraphIO.LengthDataIO;
import com.github.rinde.rinsim.geom.io.DotGraphIO.MultiAttributeDataIO;
import com.github.rinde.rinsim.geom.io.Filters.SimpleFilters;
import com.github.rinde.rinsim.testutil.TestUtil;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon
 *
 */
public class DotGraphIOTest {

  /**
   * For handling exceptions.
   */
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @SuppressWarnings("null")
  private Graph<LengthData> simpleLDGraph;
  @SuppressWarnings("null")
  private Graph<MultiAttributeData> simpleMAGraph;

  private static final ImmutableList<String> LEGACY_FORMAT = ImmutableList.of(
      "digraph mapgraph {",
      "n0[p=\"3296724.2131123254,2.5725043247255992E7\"]",
      "n1[p=\"3296796.1359189367,2.572491905319646E7\"]",
      "n2[p=\"3296880.4785663,2.5724779870530557E7\"]",
      "n3[p=\"3296883.3554785643,2.5724671468889512E7\"]",
      "n19663[p=\"3296782.7337179,2.5724994399343655E7\"]",
      "n16767[p=\"3296661.5525598335,2.5725117255271256E7\"]",
      "n0 -> n1[d=\"1.4\", s=\"50000.0\"]",
      "n0 -> n19663[d=\"0.8\"]",
      "n0 -> n16767[d=\"1.0\", s=\"50000.0\"]",
      "n1 -> n0[d=\"1.4\", s=\"50000.0\"]",
      "n1 -> n2[d=\"1.6\", s=\"50000.0\"]",
      "n2 -> n1[d=\"1.6\", s=\"50000.0\"]",
      "n2 -> n3[d=\"1.1\", s=\"50000.0\"]",
      "n3 -> n2[d=\"1.1\", s=\"50000.0\"]",
      "}"
      );

  /**
   * Sets up two maps.
   */
  @Before
  public void setUp() {
    Point a = new Point(0, 0);
    Point b = new Point(10, 0);
    Point c = new Point(10, 10);
    Point d = new Point(0, 10);

    simpleLDGraph = new TableGraph<>();
    Graphs.addBiPath(simpleLDGraph, a, b, c, d);
    simpleLDGraph.addConnection(a, c, LengthData.create(10));

    simpleMAGraph = new TableGraph<>();
    simpleMAGraph.addConnection(a, b,
        MultiAttributeData.builder()
            .setLength(0)
            .build());
    simpleMAGraph.addConnection(b, c,
        MultiAttributeData.builder()
            .setMaxSpeed(10d)
            .build());
    simpleMAGraph.addConnection(c, d,
        MultiAttributeData.builder()
            .putAttributes("key", "specialValue")
            .build());

    simpleMAGraph.addConnection(a, d);

    simpleMAGraph.addConnection(a, c,
        MultiAttributeData.builder()
            .setLength(10d)
            .setMaxSpeed(100d)
            .putAttributes("hello", "world")
            .build());
    TestUtil.testEnum(LengthDataIO.class);
    TestUtil.testEnum(MultiAttributeDataIO.class);
    TestUtil.testEnum(SimpleFilters.class);
    TestUtil.testPrivateConstructor(Filters.class);
  }

  /**
   * Missing data block closing.
   * @throws IOException Should not happen.
   */
  @Test
  public void testInvalidDataBlock() throws IOException {
    DotGraphIO<LengthData> io = DotGraphIO.getLengthGraphIO();
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(CoreMatchers.startsWith("Data block"));
    io.read(new StringReader("n3 -> n2[d=\"1.1\", s=\"50000.0\""));
  }

  /**
   * Duplicate key in data.
   * @throws IOException Should not happen.
   */
  @Test
  public void testDuplicateKeyInData() throws IOException {
    DotGraphIO<LengthData> io = DotGraphIO.getLengthGraphIO();
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(CoreMatchers.startsWith("Found a duplicate"));
    io.read(new StringReader("n3 -> n2[d=1.1, d=\"50000.0\"]"));
  }

  /**
   * Cannot use reserved attribute name.
   * @throws IOException Should not happen.
   */
  @Test
  public void testInvalidAttributeName() throws IOException {
    DotGraphIO<MultiAttributeData> io = DotGraphIO.getMultiAttributeGraphIO();
    Graph<MultiAttributeData> g = new TableGraph<>();
    g.addConnection(Connection.create(new Point(0, 0), new Point(1, 1),
        MultiAttributeData.builder()
            .putAttributes("d", "invalid")
            .build()));

    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(CoreMatchers.startsWith("Attribute key"));
    io.write(g, new StringWriter());
  }

  /**
   * Tests whether self cycles are indeed filtered out.
   * @throws IOException Should not happen.
   */
  @SuppressWarnings("static-method")
  @Test
  public void testSelfCycleFilter() throws IOException {
    StringReader sr = new StringReader(
        "n0[p=\"5,5\"]\nn1[p=\"4,4\"]\nn0 -> n0\nn0 -> n1");
    Graph<?> g = DotGraphIO.getLengthGraphIO(Filters.selfCycleFilter())
        .read(sr);
    assertEquals(1, g.getConnections().size());
  }

  /**
   * Tests reading a dot file in legacy format.
   * @throws IOException In case of IO problems.
   */
  @SuppressWarnings("static-method")
  @Test
  public void testLegacy() throws IOException {
    Graph<LengthData> ldGraph = DotGraphIO.getLengthGraphIO().read(
        new StringReader(Joiner.on("\n").join(LEGACY_FORMAT)));

    Graph<MultiAttributeData> maGraph = DotGraphIO.getMultiAttributeGraphIO()
        .read(new StringReader(Joiner.on("\n").join(LEGACY_FORMAT)));

    testLegacyFormat(ldGraph);
    testLegacyFormat(maGraph);

    Point n0 = new Point(3296724.2131123254, 2.5725043247255992E7);
    Point n19663 = new Point(3296782.7337179, 2.5724994399343655E7);
    for (Connection<MultiAttributeData> conn : maGraph.getConnections()) {
      if (conn.from().equals(n0) && conn.to().equals(n19663)) {
        assertFalse(conn.data().get().getMaxSpeed().isPresent());
      } else {
        assertEquals(50000d, conn.data().get().getMaxSpeed().get()
            .doubleValue(),
            0);
      }
    }
  }

  private static void testLegacyFormat(Graph<?> graph) {
    Point n0 = new Point(3296724.2131123254, 2.5725043247255992E7);
    Point n1 = new Point(3296796.1359189367, 2.572491905319646E7);
    Point n2 = new Point(3296880.4785663, 2.5724779870530557E7);
    Point n3 = new Point(3296883.3554785643, 2.5724671468889512E7);
    Point n19663 = new Point(3296782.7337179, 2.5724994399343655E7);
    Point n16767 = new Point(3296661.5525598335, 2.5725117255271256E7);

    assertEquals(6, graph.getNodes().size());
    assertTrue(graph.containsNode(n0));
    assertTrue(graph.containsNode(n1));
    assertTrue(graph.containsNode(n2));
    assertTrue(graph.containsNode(n3));
    assertTrue(graph.containsNode(n19663));
    assertTrue(graph.containsNode(n16767));

    assertEquals(8, graph.getConnections().size());
    assertTrue(graph.hasConnection(n0, n1));
    assertTrue(graph.hasConnection(n0, n19663));
    assertTrue(graph.hasConnection(n0, n16767));
    assertTrue(graph.hasConnection(n1, n0));
    assertTrue(graph.hasConnection(n1, n2));
    assertTrue(graph.hasConnection(n2, n1));
    assertTrue(graph.hasConnection(n2, n3));
    assertTrue(graph.hasConnection(n3, n2));

    assertEquals(1.4, graph.getConnection(n0, n1).data().get().getLength()
        .get().doubleValue(), 0);
    assertEquals(.8, graph.getConnection(n0, n19663).data().get().getLength()
        .get().doubleValue(), 0);
    assertEquals(1.0, graph.getConnection(n0, n16767).data().get().getLength()
        .get().doubleValue(), 0);
    assertEquals(1.4, graph.getConnection(n1, n0).data().get().getLength()
        .get().doubleValue(), 0);
    assertEquals(1.6, graph.getConnection(n1, n2).data().get().getLength()
        .get().doubleValue(), 0);
    assertEquals(1.6, graph.getConnection(n2, n1).data().get().getLength()
        .get().doubleValue(), 0);
    assertEquals(1.1, graph.getConnection(n2, n3).data().get().getLength()
        .get().doubleValue(), 0);
    assertEquals(1.1, graph.getConnection(n3, n2).data().get().getLength()
        .get().doubleValue(), 0);
  }

  /**
   * Tests read/write of length data graph.
   * @throws IOException Should not happen.
   */
  @Test
  public void testLengthDataGraph() throws IOException {
    StringWriter sw = new StringWriter();
    DotGraphIO.getLengthGraphIO().write(simpleLDGraph, sw);
    Graph<LengthData> g = DotGraphIO.getLengthGraphIO().read(
        new StringReader(sw.toString()));
    assertEquals(simpleLDGraph, g);
  }

  /**
   * Tests read/write of multi attribute data graph.
   * @throws IOException Should not happen.
   */
  @Test
  public void testMultiAttributeDataGraph() throws IOException {
    StringWriter sw = new StringWriter();
    DotGraphIO.getMultiAttributeGraphIO().write(simpleMAGraph, sw);
    Graph<MultiAttributeData> g = DotGraphIO.getMultiAttributeGraphIO().read(
        new StringReader(sw.toString()));
    assertEquals(simpleMAGraph, g);
  }
}
