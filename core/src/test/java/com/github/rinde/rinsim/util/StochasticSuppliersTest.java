/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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
package com.github.rinde.rinsim.util;

import static com.github.rinde.rinsim.util.StochasticSuppliers.checked;
import static com.github.rinde.rinsim.util.StochasticSuppliers.constant;
import static com.github.rinde.rinsim.util.StochasticSuppliers.uniformInt;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.junit.Test;

import com.github.rinde.rinsim.util.StochasticSupplier;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.common.base.Predicate;
import com.google.common.collect.Multiset;
import com.google.common.collect.Range;
import com.google.common.collect.TreeMultiset;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;

public class StochasticSuppliersTest {

  @Test
  public void testUniform() {
    final RandomGenerator rng = new MersenneTwister(123L);
    final StochasticSupplier<Integer> sup = uniformInt(2, 10);
    final IntegerDistribution id = new UniformIntegerDistribution(2, 10);

    final Multiset<Integer> ms = TreeMultiset.create();
    for (int i = 0; i < 1000; i++) {
      ms.add(sup.get(rng.nextLong()));
    }
    final List<Integer> observations = newArrayList();
    final List<Double> expectations = newArrayList();
    for (final Multiset.Entry<Integer> entry : ms.entrySet()) {
      observations.add(entry.getCount());
      expectations.add(id.probability(entry.getElement()));
    }
    assertTrue(chiSquare(expectations, observations, .01));
  }

  boolean chiSquare(List<? extends Number> expectations,
      List<? extends Number> observations, double confidence) {
    final double chi = TestUtils.chiSquareTest(Doubles.toArray(expectations),
      Longs.toArray(observations));
    return !(chi < confidence);
  }

  /**
   * Tests for
   * {@link StochasticSuppliers#checked(StochasticSupplier, Predicate)}.
   */
  @Test
  public void testCheckedSupplier() {
    final Predicate<Double> positive = Range.closedOpen(0d,
      Double.POSITIVE_INFINITY);

    checked(constant(0d), positive).get(0);
    checked(constant(453453453.34), positive).get(0);
    checked(constant(Double.MAX_VALUE), positive).get(0);

    boolean fail = false;
    try {
      checked(constant(Double.POSITIVE_INFINITY), positive).get(0);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
    fail = false;
    try {
      checked(constant(-0.0000000001), positive).get(0);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Tests whether the rescaling of the mean of a truncated normal distribution
   * is implemented correctly.
   */
  @Test
  public void testNormalScaleMean() {
    final double[] means = new double[] {1d, 2d, 3d, 10d, 100d};
    final double[] sds = new double[] {1d, 1d, 3d, 5d, 100d};

    for (int i = 0; i < means.length; i++) {
      final StochasticSupplier<Double> ss = StochasticSuppliers.normal()
        .mean(means[i])
        .std(sds[i])
        .lowerBound(0)
        .scaleMean()
        .redrawWhenOutOfBounds()
        .buildDouble();

      final RandomGenerator rng = new MersenneTwister(123);
      final SummaryStatistics stats = new SummaryStatistics();
      for (int j = 0; j < 10000; j++) {
        stats.addValue(ss.get(rng.nextLong()));
      }
      // 1 % deviation from mean is acceptable
      final double allowedDeviation = 0.01 * means[i];
      assertEquals(means[i], stats.getMean(), allowedDeviation);
    }

  }
}
