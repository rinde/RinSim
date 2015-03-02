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

import static com.github.rinde.rinsim.scenario.generator.TimeSeries.filter;
import static com.github.rinde.rinsim.scenario.generator.TimeSeries.homogenousPoisson;
import static com.github.rinde.rinsim.scenario.generator.TimeSeries.numEventsPredicate;
import static org.junit.Assert.assertEquals;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

import com.github.rinde.rinsim.scenario.generator.TimeSeries;
import com.github.rinde.rinsim.scenario.generator.TimeSeries.TimeSeriesGenerator;

/**
 * Tests for {@link TimeSeries}.
 * @author Rinde van Lon 
 */
public class TimeSeriesTest {

  /**
   * Test whether the number of events filter works.
   */
  @Test
  public void testFilter() {
    final TimeSeriesGenerator original = homogenousPoisson(500, 20);
    final TimeSeriesGenerator filtered = filter(original, numEventsPredicate(20));
    final RandomGenerator rng = new MersenneTwister(123L);
    for (int i = 0; i < 10; i++) {
      assertEquals(20, filtered.generate(rng.nextLong()).size());
    }
  }
}
