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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 *
 */
public class PointTest {

  final double EPSILON = 0.0000000001;

  @Test
  public void distanceTest() {
    final Point p1 = new Point(0, 0);
    final Point p2 = new Point(10, 0);
    final Point p3 = new Point(10, 10);
    final Point p4 = new Point(0, 10);

    assertEquals(10, Point.distance(p1, p2), EPSILON);
    assertEquals(14.14, Point.distance(p1, p3), 0.01);
    assertEquals(14.14, Point.distance(p3, p1), 0.01);
    assertEquals(10, Point.distance(p1, p4), EPSILON);

    assertEquals(14.14, Point.distance(p2, p4), 0.01);
    assertEquals(14.14, Point.distance(p4, p2), 0.01);
  }

  @Test
  public void testAdd() {
    final Point p1 = new Point(1, 2);
    final Point p2 = new Point(3, 4);

    final Point result = Point.add(p1, p2);

    assertEquals(new Point(4, 6), result);
  }

  @Test
  public void testDifference() {
    final Point p1 = new Point(4, 3);
    final Point p2 = new Point(1, 2);

    final Point result = Point.diff(p1, p2);

    assertEquals(new Point(3, 1), result);
  }

  @Test
  public void testDifferenceNegative() {
    final Point p1 = new Point(1, 2);
    final Point p2 = new Point(4, 3);

    final Point result = Point.diff(p1, p2);

    assertEquals(new Point(-3, -1), result);
  }

  @Test
  public void testMultiply() {
    final Point p1 = new Point(1, 2);

    final Point result = Point.multiply(p1, 3);

    assertEquals(new Point(3, 6), result);
  }

  @Test
  public void testDivide() {
    final Point p1 = new Point(1, 2);

    final Point result = Point.divide(p1, 4);

    assertEquals(new Point(0.25, 0.5), result);
  }

  @Test
  public void testDivideByZero() {
    final Point p1 = new Point(0, 2);

    final Point result = Point.divide(p1, 0);

    assertTrue(Double.isNaN(result.x));
    assertTrue(Double.isInfinite(result.y));
  }

  @Test
  public void testCenterPoint() {
    final List<Point> points = new ArrayList<>();
    Point A, B, C, D;

    A = new Point(0, 0);
    B = new Point(2, 0);
    C = new Point(0, 2);
    D = new Point(2, 2);

    points.add(A);
    points.add(B);
    points.add(C);
    points.add(D);

    final Point result = Point.centroid(points);
    assertEquals(new Point(1, 1), result);
  }

  @Test
  public void pointFuncs() {
    assertEquals(new Point(0, 0),
      Point.diff(new Point(10, 0), new Point(10, 0)));
    assertEquals(new Point(234, 333.3),
      PointTestUtil
        .duplicate(Point.parsePoint(new Point(234, 333.3).toString())));
    assertFalse(new Point(0, 0).equals(null));
    assertFalse(new Point(0, 0).equals(new Point(0, 1)));
    assertFalse(new Point(0, 0).equals(new Point(1, 0)));
    assertFalse(new Point(0, 0).equals(new Point(1, 1)));
    assertTrue(new Point(0, 0).equals(new Point(0, 0)));
    assertFalse(new Point(0, 0).equals(new Object()));
    assertFalse(new Point(0, 0).equals(new Point(1, 0)));
  }

}
