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
package com.github.rinde.rinsim.scenario.generator;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.generator.Locations.LocationGenerator;

/**
 * Tests for {@link Locations}.
 * @author Rinde van Lon 
 */
public class LocationsTest {

  /**
   * Tests whether the fixed location generator handles the inputs correctly.
   */
  @Test
  public void testFixed() {
    final LocationGenerator lg = Locations.builder().buildFixed(
        asList(new Point(0, 0), new Point(1, 1)));
    assertAlwaysEquals(lg);

    assertEquals(new Point(.5, .5), lg.getCenter());
    assertEquals(new Point(0, 0), lg.getMin());
    assertEquals(new Point(1, 1), lg.getMax());

    final LocationGenerator lg2 = Locations.builder()
        .min(-6.3)
        .max(9.3)
        .mean(0.1)
        .buildFixed(
            asList(new Point(0, 0), new Point(1, 1)));

    assertAlwaysEquals(lg2);
    assertEquals(new Point(.1, .1), lg2.getCenter());
    assertEquals(new Point(-6.3, -6.3), lg2.getMin());
    assertEquals(new Point(9.3, 9.3), lg2.getMax());
  }

  /**
   * An error should be thrown when a wrong number of locations is requested.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testFixedFail() {
    Locations.builder().buildFixed(asList(new Point(0, 0), new Point(1, 1)))
        .generate(0L, 3);
  }

  static void assertAlwaysEquals(LocationGenerator lg) {
    final RandomGenerator rng = new MersenneTwister(123);
    final List<Point> points = lg.generate(rng.nextLong(), 2);
    for (int i = 0; i < 5; i++) {
      final List<Point> points2 = lg.generate(rng.nextLong(), 2);
      assertEquals(points, points2);
    }
  }

}
