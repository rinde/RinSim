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
package com.github.rinde.rinsim.core.model.road;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.MultimapGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.PointTestUtil;
import com.github.rinde.rinsim.geom.TableGraph;

/**
 * Test whether points are value objects
 * @author Bartosz Michalik
 *
 */

@RunWith(Parameterized.class)
public class PointsEqualityTest {

  @Parameters
  public static Collection<Object[]> configs() {
    return Arrays.asList(new Object[][] {{new MultimapGraph()},
      {new TableGraph()}});
  }

  private final Graph<LengthData> graph;

  public PointsEqualityTest(Graph<LengthData> g) {
    graph = g;
  }

  @Test
  public void pointsEqual() {
    final Point p1 = new Point(0.1, 0.2);
    final Point p2 = new Point(0.1, 0.2);
    assertEquals(p1, p1);
    assertEquals(p2, p2);
    assertEquals(p1, p2);

    assertEquals(p1, PointTestUtil.duplicate(p1));
    assertEquals(p1, PointTestUtil.duplicate(p2));
  }

  /**
   * Check consistency wrt. handling value objects
   */
  @Test
  public void graphDataConsistency() {
    Point A, B, C, D;
    A = new Point(0, 0);
    B = new Point(0, 13);
    C = new Point(13, 17);
    D = new Point(17, 0);

    graph.addConnection(A, B);
    graph.addConnection(B, C);
    graph.addConnection(B, D);

    checkAssertions(A, B, C, D);

    // check if for other objects
    Point Aa, Bb;
    Aa = new Point(0, 0);
    Bb = PointTestUtil.duplicate(B);

    final Point Cc = PointTestUtil.duplicate(C);
    final Point Dd = PointTestUtil.duplicate(D);

    checkAssertions(Aa, Bb, C, D);
    checkAssertions(A, B, Cc, Dd);
    checkAssertions(Aa, Bb, Cc, Dd);
  }

  private void checkAssertions(Point A, Point B, Point C, Point D) {
    // contain nodes
    assertTrue("contains A", graph.containsNode(A));
    assertTrue("contains B", graph.containsNode(B));
    assertTrue("contains C", graph.containsNode(C));
    assertTrue("contains D", graph.containsNode(D));

    assertTrue("connection A->B", graph.hasConnection(A, B));
    assertTrue("connection B->C", graph.hasConnection(B, C));
    assertTrue("connection B->D", graph.hasConnection(B, D));

    assertFalse("!connection B->A", graph.hasConnection(B, A));
    assertFalse("!connection C->B", graph.hasConnection(A, C));
    assertFalse("!connection D->B", graph.hasConnection(A, D));
    assertFalse("!connection A->C", graph.hasConnection(A, C));
    assertFalse("!connection A->D", graph.hasConnection(A, D));
  }

  @Test
  public void testNaN() {
    assertEquals(0, Double.compare(Double.NaN, Double.NaN), 0);
  }

}
