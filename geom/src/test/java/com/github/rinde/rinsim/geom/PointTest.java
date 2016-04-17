/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import org.junit.Test;

import com.github.rinde.rinsim.geom.Point;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class PointTest {

  final double EPSILON = 0.0000000001;

  @Test
  public void distanceTest() {
    Point p1 = new Point(0, 0);
    Point p2 = new Point(10, 0);
    Point p3 = new Point(10, 10);
    Point p4 = new Point(0, 10);

    assertEquals(10, Point.distance(p1, p2), EPSILON);
    assertEquals(14.14, Point.distance(p1, p3), 0.01);
    assertEquals(14.14, Point.distance(p3, p1), 0.01);
    assertEquals(10, Point.distance(p1, p4), EPSILON);

    assertEquals(14.14, Point.distance(p2, p4), 0.01);
    assertEquals(14.14, Point.distance(p4, p2), 0.01);
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
    assertFalse(new Point(0, 0).equals((Object) new Point(1, 0)));
  }

}
