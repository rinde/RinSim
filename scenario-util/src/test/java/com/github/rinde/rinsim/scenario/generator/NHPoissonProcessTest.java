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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.junit.Ignore;
import org.junit.Test;

import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.generator.IntensityFunctions.IntensityFunction;
import com.github.rinde.rinsim.scenario.generator.TimeSeries.TimeSeriesGenerator;
import com.github.rinde.rinsim.scenario.measure.Metrics;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;

public class NHPoissonProcessTest {

  @Ignore
  @Test
  public void test() throws IOException {

    final int numSamples = 100;
    final long lengthOfScenario = 4 * 60 * 60 * 1000;
    final double period = 30 * 60 * 1000;
    final int[] orders = new int[] {10, 20, 30, 40, 50, 75, 100, 150, 200,
      500};
    final List<Point> dataPoints = newArrayList();
    final RandomGenerator rng = new MersenneTwister(123);

    final List<Double> relHeights = newArrayList();
    for (int i = 0; i < 10; i++) {
      relHeights.add(-.999 + i * .001);
    }
    for (int i = 0; i < 100; i++) {
      relHeights.add(-.99 + i * .05);
    }
    // for (int i = 0; i < 50; i++) {
    // relHeights.add(3.99 + (i * .5));
    // }
    Files.createParentDirs(new File("files/test/times/relheight-dynamism.txt"));
    final BufferedWriter writer = Files.newWriter(new File(
      "files/test/times/relheight-dynamism.txt"), Charsets.UTF_8);

    for (int k = 0; k < orders.length; k++) {
      for (int i = 0; i < relHeights.size(); i++) {
        final double d = relHeights.get(i);
        final double relHeight = d;// -.99 + (j * .05);
        // final double period = 3600d;
        final double ordersPerPeriod = orders[k] / (lengthOfScenario / period);

        final IntensityFunction intensity = IntensityFunctions.sineIntensity()
          .height(d)
          .period(period)
          .area(ordersPerPeriod)
          .build();

        System.out
          .printf("%1d relative height: %1.3f%n", i, relHeight);

        // final List<Double> sineTimes = FluentIterable
        // .from(
        // ContiguousSet.create(Range.closedOpen(0L, lengthOfScenario),
        // DiscreteDomain.longs()))
        // .transform(Conversion.LONG_TO_DOUBLE)
        // .transform(intensity)
        // .toList();

        // Analysis
        // .writeLoads(
        // sineTimes,
        // new File(
        // "files/test/times/sine/sine-"
        // + Strings.padStart(Integer.toString(i), 2, '0')
        // + ".intens"));

        final TimeSeriesGenerator generator = TimeSeries.nonHomogenousPoisson(
          lengthOfScenario, intensity);

        double max = 0;
        double sum = 0;

        final StandardDeviation sd = new StandardDeviation();
        final List<Double> dynamismValues = newArrayList();
        for (int j = 0; j < numSamples; j++) {

          List<Double> times = generator.generate(rng.nextLong());
          while (times.size() < 2) {
            times = generator.generate(rng.nextLong());
          }

          final double dyn = Metrics.measureDynamism(times, lengthOfScenario);
          dynamismValues.add(dyn);
          sd.increment(dyn);
          sum += dyn;
          max = Math.max(max, dyn);
          // if (j < 3) {
          // // System.out.printf("%1.3f%% %d%n", dyn * 100, times.size());
          // Analysis.writeTimes(
          // lengthOfScenario,
          // times,
          // new File(
          // "files/test/times/orders"
          // + Strings.padStart(Integer.toString(i), 2, '0') + "_"
          // + j
          // + "-" + (dyn * 100)
          // + ".times"));
          // }
        }

        try {
          writer.append(Double.toString(relHeight));
          writer.append(" ");
          writer.append(Integer.toString(orders[k]));
          writer.append(" ");
          writer.append(Joiner.on(" ").join(dynamismValues).toString());
          writer.append("\n");

        } catch (final IOException e) {
          checkState(false);
        }
        System.out.printf(" > dyn %1.3f+-%1.3f%n",
          +(sum / numSamples), sd.getResult());
        dataPoints.add(new Point(relHeight, sum / numSamples));
      }
    }
    writer.close();
    // Analysis.writeLocationList(dataPoints, new File(
    // "files/test/times/intensity-analysis.txt"));
  }

  // @Test
  // public void test2() {
  // final RandomGenerator rng = new MersenneTwister(123);
  // final double relHeight = DynamismModel.getRelHeightForDynamism(.3);
  // final long lengthOfScenario = 1 * 3600000;
  // final IntensityFunction intensityFunction = func(relHeight,
  // lengthOfScenario, 20);
  // final ArrivalTimesGenerator generator = new NHPoissonProcess(
  // lengthOfScenario, intensityFunction);
  //
  // final List<Double> dynVals = newArrayList();
  // for (int i = 0; i < 10000; i++) {
  // final List<Double> times = generator.generate(rng);
  // final double dyn = Metrics.measureDynamism(times, lengthOfScenario);
  // // System.out.println(dyn);
  // dynVals.add(dyn);
  // }
  // System.out.println();
  // System.out.println((new Mean()).evaluate(Doubles.toArray(dynVals)));
  // System.out.println((new StandardDeviation()).evaluate(Doubles
  // .toArray(dynVals)));
  //
  // }

  /**
   * Check whether the
   * {@link TimeSeries#nonHomogenousPoisson(double, StochasticSupplier)}
   * function behaves as expected.
   */
  @Test
  public void test2() {

    final IntensityFunction func1 = IntensityFunctions.sineIntensity()
      .period(60)
      .height(-.5)
      .build();
    final IntensityFunction func2 = IntensityFunctions.sineIntensity()
      .period(30)
      .height(1.5)
      .build();

    final StochasticSupplier<IntensityFunction> funcSup = StochasticSuppliers
      .fromIterable(Iterables.cycle(func1, func2));

    final TimeSeriesGenerator tsg = TimeSeries.nonHomogenousPoisson(120,
      funcSup);

    // note that these tests will not work for all results of the
    // TimeSeriesGenerator, it is possible (but less likely) that the produced
    // dynamism values are different from the expectations as expressed in the
    // tests below.
    for (int i = 0; i < 20; i++) {
      assertTrue(Metrics.measureDynamism(tsg.generate(i), 120) < .25);
      assertTrue(Metrics.measureDynamism(tsg.generate(i), 120) > .40);
    }
  }

  private static class DynamismModel {
    private static double X_MIN = -.99;
    private static double X_MAX = 1.0;

    private static double INTERCEPT = 0.1860437;
    private static double X = 0.1744785;

    public static double getRelHeightForDynamism(double dyn) {
      return (dyn - INTERCEPT) / X;
    }

  }

  // static IntensityFunction func(double relHeight, long lengthOfScenario,
  // int numberOfOrders) {
  // final double freq = 1d / 3600000d;
  // final SineIntensity intensity = SineIntensity.builder()
  // .period(3600000d)
  // .amplitude(1d)
  // .frequency(freq)
  // .height(relHeight)
  // .build();
  //
  // final UnivariateIntegrator ri = new RombergIntegrator(16, 32);// new
  //
  // final double val = ri.integrate(10000000, new IntensityFunctionWrapper(
  // intensity), 0, lengthOfScenario);
  //
  // final double newAmpl = numberOfOrders / val;
  //
  // final SineIntensity finalIntensity = new SineIntensity(
  // newAmpl, freq, relHeight);
  // return finalIntensity;
  // }

  enum Conversion implements Function<Long, Double> {
    LONG_TO_DOUBLE {
      @Override
      @Nonnull
      public Double apply(@Nullable Long input) {
        return new Double(checkNotNull(input));
      }
    },

  }
}
