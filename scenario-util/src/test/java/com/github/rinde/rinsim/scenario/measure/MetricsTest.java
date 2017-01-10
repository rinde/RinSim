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
package com.github.rinde.rinsim.scenario.measure;

import static com.github.rinde.rinsim.scenario.measure.Metrics.measureDynamism;
import static com.github.rinde.rinsim.scenario.measure.Metrics.measureLoad;
import static com.github.rinde.rinsim.scenario.measure.Metrics.sum;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.junit.Test;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.scenario.generator.TravelTimesUtil;
import com.github.rinde.rinsim.scenario.measure.Metrics.LoadPart;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.Range;
import com.google.common.io.Files;
import com.google.common.math.DoubleMath;

/**
 * @author Rinde van Lon
 *
 */
public class MetricsTest {
  static final double EPSILON = 0.00001;

  /**
   * Test of load metric with specific settings.
   */
  @Test
  public void testLoad1() {
    // distance is 1 km which is traveled in 2 minutes with 30km/h
    final ParcelDTO dto = Parcel.builder(new Point(0, 0), new Point(0, 1))
      .pickupTimeWindow(TimeWindow.create(0, 10))
      .deliveryTimeWindow(TimeWindow.create(10, 20))
      .neededCapacity(0)
      .orderAnnounceTime(0)
      .serviceDuration(5)
      .buildDTO();

    final List<LoadPart> parts = measureLoad(AddParcelEvent.create(dto),
      TravelTimesUtil.constant(2L));
    assertEquals(3, parts.size());

    // pickup load in [0,15), duration is 5 minutes, so load is 5/15 = 1/3
    assertEquals(0, parts.get(0).begin());
    assertEquals(1 / 3d, parts.get(0).get(0), EPSILON);
    assertEquals(15, parts.get(0).length());

    // travel load in [5,20), duration is 2 minutes, so load is 2/15
    assertEquals(5, parts.get(1).begin());
    assertEquals(2 / 15d, parts.get(1).get(5), EPSILON);
    assertEquals(15, parts.get(1).length());

    // delivery load in [10,25), duration is 5 minutes, so load is 5/15 =
    // 1/3
    assertEquals(10, parts.get(2).begin());
    assertEquals(1 / 3d, parts.get(2).get(10), EPSILON);
    assertEquals(15, parts.get(2).length());

    // summing results:
    // [0,5) - 5/15
    // [5,10) - 7/15
    // [10,15) - 12/15
    // [15,20) - 7/15
    // [20,25) - 5/15

    final List<Double> load = sum(0, parts, 1);
    checkRange(load, 0, 5, 5 / 15d);
    checkRange(load, 5, 10, 7 / 15d);
    checkRange(load, 10, 15, 12 / 15d);
    checkRange(load, 15, 20, 7 / 15d);
    checkRange(load, 20, 25, 5 / 15d);
    assertEquals(25, load.size());
  }

  /**
   * Test of load metric with specific settings.
   */
  @Test
  public void testLoad2() {
    // distance is 10km which is traveled in 20 minutes with 30km/h
    final ParcelDTO dto = Parcel.builder(new Point(0, 0), new Point(0, 10))
      .pickupTimeWindow(TimeWindow.create(15, 15))
      .deliveryTimeWindow(TimeWindow.create(15, 15))
      .neededCapacity(0)
      .orderAnnounceTime(0)
      .serviceDuration(5)
      .buildDTO();

    final List<LoadPart> parts = measureLoad(AddParcelEvent.create(dto),
      TravelTimesUtil.constant(20L));
    assertEquals(3, parts.size());

    // pickup load in [15,20), duration is 5 minutes, so load is 5/5 = 1
    assertEquals(15, parts.get(0).begin());
    assertEquals(1d, parts.get(0).get(15), EPSILON);
    assertEquals(0d, parts.get(0).get(20), EPSILON);
    assertEquals(5, parts.get(0).length());

    // travel load in [20,40), duration is 20 minutes, so load is 20/20 = 1
    assertEquals(20, parts.get(1).begin());
    assertEquals(1, parts.get(1).get(20), EPSILON);
    assertEquals(20, parts.get(1).length());

    // delivery load in [40,45), duration is 5 minutes, so load is 5/5 = 1
    assertEquals(40, parts.get(2).begin());
    assertEquals(1, parts.get(2).get(40), EPSILON);
    assertEquals(5, parts.get(2).length());

    // summing results:
    // [0,15) - 0
    // [15,45) - 1

    final List<Double> load = sum(0, parts, 1);
    checkRange(load, 0, 15, 0);
    checkRange(load, 15, 45, 1);
    assertEquals(45, load.size());
  }

  /**
   * Test of load metric with specific settings.
   */
  @Test
  public void testLoad3() {
    // distance is 3 km which is traveled in 6 minutes with 30km/h
    final ParcelDTO dto = Parcel.builder(new Point(0, 0), new Point(0, 3))
      .pickupTimeWindow(TimeWindow.create(10, 30))
      .deliveryTimeWindow(TimeWindow.create(50, 75))
      .neededCapacity(0)
      .orderAnnounceTime(0L)
      .pickupDuration(5L)
      .deliveryDuration(5L)
      .buildDTO();

    final List<LoadPart> parts = measureLoad(AddParcelEvent.create(dto),
      TravelTimesUtil.constant(6L));
    assertEquals(3, parts.size());

    // pickup load in [10,35), duration is 5 minutes, so load is 5/25 = 6/30
    assertEquals(10, parts.get(0).begin());
    assertEquals(6 / 30d, parts.get(0).get(10), EPSILON);
    assertEquals(25, parts.get(0).length());

    // travel load in [15,75), duration is 6 minutes, so load is 6/60 = 3/30
    assertEquals(15, parts.get(1).begin());
    assertEquals(3 / 30d, parts.get(1).get(15), EPSILON);
    assertEquals(60, parts.get(1).length());

    // delivery load in [50,80), duration is 5 minutes, so load is 5/30
    assertEquals(50, parts.get(2).begin());
    assertEquals(5 / 30d, parts.get(2).get(50), EPSILON);
    assertEquals(30, parts.get(2).length());

    // summing results:
    // [00,10) - 0/30
    // [10,15) - 6/30
    // [15,35) - 9/30
    // [35,50) - 3/30
    // [50,75) - 8/30
    // [75,80) - 5/30

    final List<Double> load = sum(0, parts, 1);
    checkRange(load, 0, 10, 0d);
    checkRange(load, 10, 15, 6 / 30d);
    checkRange(load, 15, 35, 9 / 30d);
    checkRange(load, 35, 50, 3 / 30d);
    checkRange(load, 50, 75, 8 / 30d);
    checkRange(load, 75, 80, 5 / 30d);
    assertEquals(80, load.size());
  }

  // checks whether the range [from,to) in list contains value val
  static void checkRange(List<Double> list, int from, int to, double val) {
    for (int i = from; i < to; i++) {
      assertEquals(val, list.get(i), EPSILON);
    }
  }

  static Times generateTimes(RandomGenerator rng, double intensity) {
    final ExponentialDistribution ed = new ExponentialDistribution(
      1000d / intensity);
    ed.reseedRandomGenerator(rng.nextLong());
    final List<Long> times = newArrayList();

    long sum = 0;
    while (sum < 1000) {
      sum += DoubleMath.roundToLong(ed.sample(), RoundingMode.HALF_DOWN);
      if (sum < 1000) {
        times.add(sum);
      }
    }
    return asTimes(1000, times);
  }

  static class Times {
    public final int length;
    public final ImmutableList<Double> list;

    public Times(int l, List<Double> li) {
      length = l;
      list = ImmutableList.copyOf(li);
    }
  }

  static Times asTimesDouble(int length, List<Double> li) {
    return new Times(length, li);
  }

  static Times asTimes(int length, List<Long> li) {
    final List<Double> newLi = newArrayList();
    for (final long l : li) {
      newLi.add((double) l);
    }
    return new Times(length, newLi);
  }

  static Times asTimes(int length, Long... longs) {
    return asTimes(length, asList(longs));
  }

  @Test
  public void dynamismTest2() {

    final double[] ordersPerHour = {15d};// , 20d, 50d, 100d, 1000d };

    final StandardDeviation sd = new StandardDeviation();

    final RandomGenerator rng = new MersenneTwister(123L);

    final List<Times> times = newArrayList();
    // for (int i = 0; i < 10; i++) {
    // times.add(generateTimes(rng));
    // }
    times.add(asTimes(1000, 250L, 500L, 750L));
    times.add(asTimes(1000, 100L, 500L, 750L));
    times.add(asTimes(1000, 100L, 200L, 300L, 400L, 500L, 600L, 700L, 800L,
      900L));
    times.add(asTimes(1000, 100L, 200L, 300L, 399L, 500L, 600L, 700L, 800L,
      900L));
    times
      .add(asTimes(1000, 10L, 150L, 250L, 350L, 450L, 550L, 650L, 750L, 850L,
        950L));
    times
      .add(asTimes(1000, 50L, 150L, 250L, 350L, 450L, 551L, 650L, 750L, 850L,
        950L));
    times.add(asTimes(1000, 250L, 500L, 750L));
    times.add(asTimes(1000, 0L, 50L, 55L, 57L, 59L, 60L, 100L, 150L, 750L));
    times.add(asTimes(1000, 5L, 5L, 5L, 5L));
    times.add(asTimes(1000, 4L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L,
      5L,
      5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L));
    times.add(asTimes(1000, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L,
      5L,
      5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L));
    times.add(asTimes(1000, 0L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L,
      5L,
      5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L,
      999L));
    times.add(asTimes(1000, 500L, 500L, 500L, 500L));
    times.add(asTimes(1000, 5L, 5L, 5L, 5L, 400L, 410L, 430L, 440L, 800L, 810L,
      820L,
      830L));
    times.add(asTimes(1000, 0L, 0L, 0L));
    times.add(asTimes(1000, 1L, 1L, 1L));
    times.add(asTimes(1000, 999L, 999L, 999L));
    times.add(asTimes(1000, 0L, 0L, 500L, 500L, 999L, 999L));
    times.add(asTimes(1000, 250L, 250L, 500L, 500L, 750L, 750L));
    times.add(asTimes(1000, 250L, 250L, 250L, 500L, 500L, 500L, 750L, 750L,
      750L));

    for (int i = 0; i < 10; i++) {
      times.add(generateTimes(rng, 10d));
    }

    for (int i = 0; i < 10; i++) {
      times.add(generateTimes(rng, 30d));
    }
    for (int i = 0; i < 5; i++) {

      final List<Double> ts = generateTimes(rng, 50d).list;

      final List<Double> newTs = newArrayList();
      for (final double l : ts) {
        newTs.add(l);
        newTs.add(Math.min(999, Math.max(0, l
          + DoubleMath.roundToLong(rng.nextDouble() * 6d - 3d,
            RoundingMode.HALF_EVEN))));
      }
      times.add(asTimesDouble(1000, newTs));
    }

    for (int i = 0; i < 5; i++) {

      final List<Double> ts = generateTimes(rng, 100d).list;

      final List<Double> newTs = newArrayList();
      System.out.println("num events: " + ts.size());
      for (final double l : ts) {
        newTs.add(l);
        newTs.add(Math.min(999, Math.max(0, l
          + DoubleMath.roundToLong(rng.nextDouble() * 2d - 1d,
            RoundingMode.HALF_EVEN))));
        newTs.add(Math.min(999, Math.max(0, l
          + DoubleMath.roundToLong(rng.nextDouble() * 2d - 1d,
            RoundingMode.HALF_EVEN))));
      }
      times.add(asTimesDouble(1000, newTs));
    }

    final List<Long> t2 = asList(10L, 30L, 50L, 70L, 90L);
    for (int i = 0; i < 5; i++) {
      final List<Long> c = newArrayList();
      for (int j = 0; j < i + 1; j++) {
        c.addAll(t2);
      }
      Collections.sort(c);
      times.add(asTimes(100, c));
    }

    final List<Long> t = asList(100L, 300L, 500L, 700L, 900L);

    for (int i = 0; i < 15; i++) {
      final List<Long> c = newArrayList();
      for (int j = 0; j < i + 1; j++) {
        c.addAll(t);
      }
      Collections.sort(c);
      times.add(asTimes(1000, c));
    }

    final List<Long> variant = newArrayList();
    variant.addAll(t);
    for (int i = 0; i < 70; i++) {
      variant.add(100L);
    }
    Collections.sort(variant);
    times.add(asTimes(1000, variant));
    checkState(variant.size() == 75);
    checkState(times.get(times.size() - 2).list.size() == 75, "",
      times.get(times.size() - 2).list.size());

    for (int i = 0; i < 10; i++) {
      times.add(generateTimes(rng, (i + 1) * 100d));
    }

    final ImmutableList<Long> all = ContiguousSet.create(
      Range.closedOpen(0L, 1000L),
      DiscreteDomain.longs()).asList();

    times.add(asTimes(1000, all));

    final List<Long> more = newArrayList(all);
    for (final long l : all) {
      if (l % 2 == 0) {
        more.add(l);
      }
    }
    Collections.sort(more);
    times.add(asTimes(1000, more));

    final List<Long> more2 = newArrayList(all);
    for (int i = 0; i < 200; i++) {
      more2.add(100L);
    }
    for (int i = 0; i < 100; i++) {
      more2.add(200L);
    }

    Collections.sort(more2);
    final List<Long> newMore = newArrayList();
    for (int i = 0; i < more2.size(); i++) {
      newMore.add(more2.get(i) * 10L);
    }
    times.add(asTimes(1000, more2));
    times.add(asTimes(10000, newMore));

    for (int k = 0; k < 5; k++) {
      final List<Double> ts = newArrayList(generateTimes(rng, 20).list);
      final List<Double> additions = newArrayList();
      for (int i = 0; i < ts.size(); i++) {

        if (i % 3 == 0) {
          for (int j = 0; j < k; j++) {
            additions.add(ts.get(i) + rng.nextDouble() * 10);
          }
        }
      }
      ts.addAll(additions);
      Collections.sort(ts);
      times.add(asTimesDouble(1000, ts));
    }

    final Times regular = asTimes(10, 0L, 1L, 2L, 5L, 6L, 7L, 8L, 9L);

    for (int i = 1; i < 4; i++) {
      final List<Double> newList = newArrayList();
      for (final double l : regular.list) {
        newList.add(l * Math.pow(10, i));
      }
      times
        .add(
          asTimesDouble((int) (regular.length * Math.pow(10, i)), newList));
    }

    times.add(asTimes(1000, 250L, 250L, 250L, 500L, 500L, 500L, 750L,
      750L, 750L));

    times.add(asTimes(1000, 250L, 500L, 500L, 500L, 500L, 500L, 500L,
      500L, 750L));

    times.add(asTimes(1000, 100L, 100L, 200L, 200L, 300L, 300L, 400L, 400L,
      500L,
      500L, 600L, 600L, 700L, 700L, 800L, 800L, 900L, 900L));
    times.add(asTimes(1000, 100L, 200L, 200L, 200L, 200L, 200L, 200L, 200L,
      200L,
      200L, 200L, 300L, 400L, 500L, 600L, 700L, 800L, 900L));
    times.add(asTimes(1000, 100L, 200L, 300L, 400L, 500L, 600L, 700L, 800L,
      800L,
      800L, 800L, 800L, 800L, 800L, 800L, 800L, 800L, 900L));

    // times.subList(1, times.size()).clear();

    for (int j = 0; j < ordersPerHour.length; j++) {
      System.out.println("=========" + ordersPerHour[j] + "=========");
      for (int i = 0; i < times.size(); i++) {

        System.out.println("----- " + i + " -----");
        // System.out.println(times.get(i).length + " " + times.get(i).list);
        // final double dod2 = measureDynamismDistr(times.get(i).list,
        // times.get(i).length);

        final double dod8 = measureDynamism(times.get(i).list,
          times.get(i).length);

        // final double dod5 = measureDynamism2ndDerivative(times.get(i).list,
        // times.get(i).length);
        // final double dod6 = measureDynDeviationCount(times.get(i).list,
        // times.get(i).length);
        // final double dod7 = chi(times.get(i).list,
        // times.get(i).length);
        // System.out.println(dod);
        // System.out.printf("%1.3f%%\n", dod2 * 100d);
        System.out.printf("%1.3f%%\n", dod8 * 100d);
        // System.out.printf("%1.3f%%\n", dod5 * 100d);
        // System.out.printf("%1.3f%%\n", dod6 * 100d);
        // System.out.printf("%1.3f%%\n", dod7);

        final double name = Math.round(dod8 * 100000d) / 1000d;

        try {
          final File dest = new File(
            "files/generator/times/orders"
              + Strings.padStart(Integer.toString(i), 3, '0')
              + "-" + name + ".times");
          Files.createParentDirs(dest);
          Files.write(
            times.get(i).length + "\n"
              + Joiner.on("\n").join(times.get(i).list) + "\n",
            dest,
            Charsets.UTF_8);
        } catch (final IOException e) {
          throw new IllegalArgumentException();
        }
      }
    }
  }

  @Test
  public void testDynamismScaleInvariant0() {
    final double expectedDynamism = 0d;
    final List<Integer> numEvents = asList(7, 30, 50, 70, 100, 157, 234,
      748, 998, 10000, 100000);
    final int scenarioLength = 1000;
    for (final int num : numEvents) {
      final List<Double> scenario = newArrayList();
      for (int i = 0; i < num; i++) {
        scenario.add(300d);
      }
      final double dyn = measureDynamism(scenario, scenarioLength);
      assertEquals(expectedDynamism, dyn, 0.0001);
    }
  }

  @Test
  public void testDynamismScaleInvariant50() {
    final double expectedDynamism = 0.5d;
    final List<Integer> numEvents = asList(30, 50, 70, 100, 158, 234, 426,
      748, 998, 10000, 100000);
    final int scenarioLength = 1000;
    for (final int num : numEvents) {
      final List<Double> scenario = newArrayList();
      final int unique = num / 2;
      final double dist = scenarioLength / (double) unique;
      for (int i = 0; i < num / 2; i++) {
        scenario.add(dist / 2d + i * dist);
        scenario.add(dist / 2d + i * dist);
      }
      final double dyn = measureDynamism(scenario, scenarioLength);
      assertEquals(expectedDynamism, dyn, 0.02);
    }
  }

  /**
   * The metric should be insensitive to the number of events.
   */
  @Test
  public void testDynamismScaleInvariant100() {
    final double expectedDynamism = 1d;
    final List<Integer> numEvents = asList(7, 30, 50, 70, 100, 157, 234,
      748, 998, 10000, 100000);
    final int scenarioLength = 1000;
    for (final int num : numEvents) {
      final double dist = scenarioLength / (double) num;
      final List<Double> scenario = newArrayList();
      for (int i = 0; i < num; i++) {
        scenario.add(dist / 2d + i * dist);
      }
      final double dyn = measureDynamism(scenario, scenarioLength);
      assertEquals(expectedDynamism, dyn, 0.0001);
    }
  }

  List<Double> generateUniformScenario(RandomGenerator rng, int numEvents,
      double scenarioLength) {
    final List<Double> events = newArrayList();
    for (int i = 0; i < numEvents; i++) {
      events.add(rng.nextDouble() * scenarioLength);
    }
    Collections.sort(events);
    return events;
  }

  /**
   * The metric should be time scale invariant, meaning that when the length of
   * the day is scaled together with all event times the metric should give the
   * same value. For example:
   * <ul>
   * <li>10 - 2 3 6 7 9</li>
   * <li>100 - 20 30 60 70 90</li>
   * </ul>
   * <br>
   * should give the same values.
   */
  @Test
  public void testDynamismTimeScaleInvariant() {
    final RandomGenerator rng = new MersenneTwister(123);
    final double startLengthOfDay = 1000;
    final int repetitions = 3;
    final List<Integer> numEvents = asList(200, 300, 400, 500, 600, 700, 800);
    final List<Double> lengthsOfDay = asList(startLengthOfDay, 1300d, 2000d,
      4500d, 7600d, 15000d, 60000d, 10000d, 100000d);
    for (final int events : numEvents) {
      // repetitions
      for (int j = 0; j < repetitions; j++) {
        final List<Double> scenario = newArrayList();
        for (int i = 0; i < events; i++) {
          scenario.add(rng.nextDouble() * startLengthOfDay);
        }
        Collections.sort(scenario);
        double dod = -1;
        for (final double dayLength : lengthsOfDay) {
          final List<Double> cur = newArrayList();
          for (final double i : scenario) {
            cur.add(i * (dayLength / startLengthOfDay));
          }
          final double curDod = measureDynamism(cur, dayLength);
          if (dod >= 0d) {
            assertEquals(dod, curDod, 0.001);
          }
          dod = curDod;
        }
      }
    }
  }

  /**
   * The metric should be insensitive to shifting all events left or right.
   */
  @Test
  public void testDynamismShiftInvariant() {
    final RandomGenerator rng = new MersenneTwister(123);
    final double lengthOfDay = 1000;
    final int repetitions = 10;
    final List<Integer> numEvents = asList(20, 50, 120, 200, 300, 500, 670,
      800);

    for (final int num : numEvents) {
      for (int j = 0; j < repetitions; j++) {
        final List<Double> scenario = newArrayList();
        for (int i = 0; i < num; i++) {
          scenario.add(rng.nextDouble() * lengthOfDay);
        }
        Collections.sort(scenario);

        final double curDod = measureDynamism(scenario, lengthOfDay);

        // shift left
        final List<Double> leftShiftedScenario = newArrayList();
        final double leftMost = scenario.get(0);
        for (int i = 0; i < num; i++) {
          leftShiftedScenario.add(scenario.get(i) - leftMost);
        }
        final double leftDod = measureDynamism(leftShiftedScenario,
          lengthOfDay);

        // shift right
        final List<Double> rightShiftedScenario = newArrayList();
        final double rightMost = lengthOfDay
          - scenario.get(scenario.size() - 1) - 0.00000001;
        for (int i = 0; i < num; i++) {
          rightShiftedScenario.add(scenario.get(i) + rightMost);
        }
        final double rightDod = measureDynamism(rightShiftedScenario,
          lengthOfDay);

        assertEquals(curDod, leftDod, 0.0001);
        assertEquals(curDod, rightDod, 0.0001);
      }
    }
  }

  /**
   * Tests whether histogram is computed correctly.
   */
  @Test
  public void testHistogram() {
    assertEquals(ImmutableSortedMultiset.of(),
      Metrics.computeHistogram(Arrays.<Double>asList(), .2));

    final List<Double> list = Arrays.asList(1d, 2d, 2d, 1.99999, 3d, 4d);
    assertEquals(ImmutableSortedMultiset.of(0d, 0d, 2d, 2d, 2d, 4d),
      Metrics.computeHistogram(list, 2));

    final List<Double> list2 = Arrays.asList(1d, 2d, 2d, 1.99999, 3d, 4d);
    assertEquals(ImmutableSortedMultiset.of(1d, 1.5d, 2d, 2d, 3d, 4d),
      Metrics.computeHistogram(list2, .5));
  }

  /**
   * NaN is not accepted.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testHistogramInvalidInput1() {
    Metrics.computeHistogram(asList(0d, Double.NaN), 3);
  }

  /**
   * Infinity is not accepted.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testHistogramInvalidInput2() {
    Metrics.computeHistogram(asList(0d, Double.POSITIVE_INFINITY), 3);
  }

  static <T> boolean areAllValuesTheSame(List<T> list) {
    if (list.isEmpty()) {
      return true;
    }
    return Collections.frequency(list, list.get(0)) == list.size();
  }
}
