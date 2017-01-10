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
package com.github.rinde.rinsim.scenario.generator;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.scenario.generator.IntensityFunctions.SineIntensity;
import com.github.rinde.rinsim.scenario.generator.TimeSeries.NonHomogenous;
import com.github.rinde.rinsim.scenario.generator.TimeSeries.PoissonProcess;
import com.github.rinde.rinsim.testutil.TestUtil;
import com.google.common.math.DoubleMath;

/**
 * @author Rinde van Lon
 * 
 */
@RunWith(Parameterized.class)
public class PoissonProcessTest {

  private final PoissonProcess poisson;

  /**
   * @param atg The process to test.
   */
  public PoissonProcessTest(PoissonProcess atg) {
    poisson = atg;
    TestUtil.testPrivateConstructor(TimeSeries.class);
  }

  /**
   * @return Test configs
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
      {TimeSeries.homogenousPoisson(60d, 10)},
      {TimeSeries.homogenousPoisson(60d, 100)},
      {TimeSeries.homogenousPoisson(180d, 10)},
      {TimeSeries.homogenousPoisson(180d, 100)},
      {TimeSeries.nonHomogenousPoisson(60d, IntensityFunctions
        .sineIntensity()
        .period(60).area(10).build())},
      {TimeSeries.nonHomogenousPoisson(60d, IntensityFunctions
        .sineIntensity()
        .period(60).area(10).phaseShift(2).build())},
      {TimeSeries.nonHomogenousPoisson(60d, IntensityFunctions
        .sineIntensity()
        .period(60).area(10).height(1).build())},
      {TimeSeries.nonHomogenousPoisson(300d, IntensityFunctions
        .sineIntensity()
        .period(60).area(10).height(1).build())},
      {TimeSeries.nonHomogenousPoisson(600d, IntensityFunctions
        .sineIntensity()
        .period(600).area(200).height(1).build())}
    });
  }

  /**
   * Tests whether the Poisson process has a Poisson distribution.
   */
  @Test
  public void testPoissonDistribution() {
    final double length = poisson.getLength();
    final Frequency f = new Frequency();

    final RandomGenerator rng = new MersenneTwister(0);
    for (int i = 0; i < 1000; i++) {

      final List<Double> doubles = poisson.generate(rng.nextLong());
      final List<Long> list = newArrayList();
      for (final double d : doubles) {
        list.add(DoubleMath.roundToLong(d, RoundingMode.HALF_UP));
      }
      ascendingOrderTest(list);
      // add the number of announcements
      f.addValue(list.size());
    }
    final double averageIntensity;
    if (poisson instanceof NonHomogenous
      && ((NonHomogenous) poisson).lambd instanceof SineIntensity) {
      final SineIntensity si = (SineIntensity) ((NonHomogenous) poisson).lambd;

      final double period = 1d / si.getFrequency();
      final double periods = length / period;
      final double totalEvents = si.area() * periods;
      averageIntensity = totalEvents / length;
    } else {
      averageIntensity = poisson.intensity;
    }
    assertTrue(isPoissonProcess(f, averageIntensity, length, 0.001));
    assertFalse(isPoissonProcess(f, 2, length, 0.01));
    assertFalse(isPoissonProcess(f, 0.1, length, 0.0001));
    assertFalse(isPoissonProcess(f, 15, length, 0.001));
    assertFalse(isPoissonProcess(f, 1000, length, 0.0001));
  }

  /**
   * Tests determinism of arrival times generators, given the same random number
   * generator and seed they should always return the same sequence.
   */
  @Test
  public void determinismTest() {
    final RandomGenerator outer = new MersenneTwister(123);

    for (int i = 0; i < 100; i++) {
      final long seed = outer.nextLong();
      final RandomGenerator inner = new MersenneTwister(seed);
      final List<Double> list1 = poisson.generate(inner.nextLong());
      for (int j = 0; j < 100; j++) {
        inner.setSeed(seed);
        final List<Double> list2 = poisson.generate(inner.nextLong());
        assertEquals(list1, list2);
      }
    }
  }

  /**
   * Checks whether the observations conform to a Poisson process with the
   * specified intensity. Uses a chi square test with the specified confidence.
   * The null hypothesis is that the observations are the result of a poisson
   * process.
   * @param observations
   * @param intensity
   * @param confidence
   * @return <code>true</code> if the observations
   */
  static boolean isPoissonProcess(Frequency observations, double intensity,
      double length, double confidence) {
    final PoissonDistribution pd = new PoissonDistribution(length * intensity);

    final Iterator<?> it = observations.valuesIterator();
    final long[] observed = new long[observations.getUniqueCount()];
    final double[] expected = new double[observations.getUniqueCount()];

    int index = 0;
    while (it.hasNext()) {
      final Long l = (Long) it.next();
      observed[index] = observations.getCount(l);
      expected[index] = pd.probability(l.intValue())
        * observations.getSumFreq();
      if (expected[index] == 0) {
        return false;
      }
      index++;
    }
    final double chi = TestUtils.chiSquareTest(expected, observed);
    return !(chi < confidence);
  }

  static void ascendingOrderTest(List<? extends Number> arrivalTimes) {
    Number prev = 0;
    for (final Number l : arrivalTimes) {
      assertTrue(prev.doubleValue() <= l.doubleValue());
      prev = l;
    }
  }
}
