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

/**
 * Helper class for writing tests with {@link Point}s.
 * @author Rinde van Lon
 */
public class PointAssert {
  /**
   * Asserts that the x and y coordinates of the two points are within the
   * specified delta. If they are not an {@link AssertionError} is thrown.
   * @param expected Expected value.
   * @param actual Value to check.
   * @param delta The maximum delta between <code>expected</code> and
   *          <code>actual</code>.
   */
  public static void assertPointEquals(Point expected, Point actual,
      double delta) {
    assertEquals(expected.x, actual.x, delta);
    assertEquals(expected.y, actual.y, delta);
  }
}
