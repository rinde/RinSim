/**
 * 
 */
package rinde.sim.pdptw.generator;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static rinde.sim.pdptw.generator.Metrics.measureDynamism;
import static rinde.sim.pdptw.generator.Metrics.measureDynamism2ndDerivative;
import static rinde.sim.pdptw.generator.Metrics.measureDynamismDistr;
import static rinde.sim.pdptw.generator.Metrics.measureDynamismDistr2;
import static rinde.sim.pdptw.generator.Metrics.measureDynamismDistr3;
import static rinde.sim.pdptw.generator.Metrics.measureLoad;
import static rinde.sim.pdptw.generator.Metrics.sum;
import static rinde.sim.pdptw.generator.Metrics.travelTime;

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

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.ParcelDTO;
import rinde.sim.pdptw.generator.Metrics.LoadPart;
import rinde.sim.util.TimeWindow;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.Range;
import com.google.common.io.Files;
import com.google.common.math.DoubleMath;
import com.google.common.primitives.Doubles;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class MetricsTest {

  static final double EPSILON = 0.00001;

  @Test
  public void testLoad1() {
    // distance is 1 km which is traveled in 2 minutes with 30km/h
    final ParcelDTO dto = new ParcelDTO(new Point(0, 0), new Point(0, 1),
        new TimeWindow(0, 10), new TimeWindow(10, 20), 0, 0, 5, 5);

    final List<LoadPart> parts = measureLoad(new AddParcelEvent(dto), 30);
    assertEquals(3, parts.size());

    // pickup load in [0,15), duration is 5 minutes, so load is 5/15 = 1/3
    assertEquals(0, parts.get(0).begin);
    assertEquals(1 / 3d, parts.get(0).get(0), EPSILON);
    assertEquals(15, parts.get(0).length());

    // travel load in [5,20), duration is 2 minutes, so load is 2/15
    assertEquals(5, parts.get(1).begin);
    assertEquals(2 / 15d, parts.get(1).get(5), EPSILON);
    assertEquals(15, parts.get(1).length());

    // delivery load in [10,25), duration is 5 minutes, so load is 5/15 =
    // 1/3
    assertEquals(10, parts.get(2).begin);
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

  @Test
  public void testLoad2() {
    // distance is 10km which is travelled in 20 minutes with 30km/h
    final ParcelDTO dto = new ParcelDTO(new Point(0, 0), new Point(0, 10),
        new TimeWindow(15, 15), new TimeWindow(15, 15), 0, 0, 5, 5);

    final List<LoadPart> parts = measureLoad(new AddParcelEvent(dto), 30);
    assertEquals(3, parts.size());

    // pickup load in [15,20), duration is 5 minutes, so load is 5/5 = 1
    assertEquals(15, parts.get(0).begin);
    assertEquals(1d, parts.get(0).get(15), EPSILON);
    assertEquals(0d, parts.get(0).get(20), EPSILON);
    assertEquals(5, parts.get(0).length());

    // travel load in [20,40), duration is 20 minutes, so load is 20/20 = 1
    assertEquals(20, parts.get(1).begin);
    assertEquals(1, parts.get(1).get(20), EPSILON);
    assertEquals(20, parts.get(1).length());

    // delivery load in [40,45), duration is 5 minutes, so load is 5/5 = 1
    assertEquals(40, parts.get(2).begin);
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

  @Test
  public void testLoad3() {
    // distance is 3 km which is traveled in 6 minutes with 30km/h
    final ParcelDTO dto = new ParcelDTO(new Point(0, 0), new Point(0, 3),
        new TimeWindow(10, 30), new TimeWindow(50, 75), 0, 0, 5, 5);

    final List<LoadPart> parts = measureLoad(new AddParcelEvent(dto), 30);
    assertEquals(3, parts.size());

    // pickup load in [10,35), duration is 5 minutes, so load is 5/25 = 6/30
    assertEquals(10, parts.get(0).begin);
    assertEquals(6 / 30d, parts.get(0).get(10), EPSILON);
    assertEquals(25, parts.get(0).length());

    // travel load in [15,75), duration is 6 minutes, so load is 6/60 = 3/30
    assertEquals(15, parts.get(1).begin);
    assertEquals(3 / 30d, parts.get(1).get(15), EPSILON);
    assertEquals(60, parts.get(1).length());

    // delivery load in [50,80), duration is 5 minutes, so load is 5/30
    assertEquals(50, parts.get(2).begin);
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

  @Test
  public void testTravelTime() {
    // driving 1 km with 30km/h should take exactly 2 minutes
    assertEquals(2, travelTime(new Point(0, 0), new Point(1, 0), 30d));

    // driving 1 km with 60km/h should take exactly 1 minutes
    assertEquals(1, travelTime(new Point(0, 0), new Point(1, 0), 60d));

    // driving 1 km with 90km/h should take .667 minutes, which should be
    // rounded to 1 minute.
    assertEquals(1, travelTime(new Point(0, 0), new Point(1, 0), 90d));

    // TODO check the rounding behavior
  }

  /**
   * Test for the dynamism function.
   */
  @Test
  public void dynamismTest() {
    assertEquals(.5, measureDynamism(asList(1L, 2L, 3L, 4L, 5L), 10,
        1), EPSILON);
    // duplicates should be ignored
    assertEquals(.5, measureDynamism(asList(1L, 2L, 2L, 3L, 4L, 5L), 10,
        1), EPSILON);

    // granularity equals length of day
    assertEquals(1d, measureDynamism(asList(1L, 2L, 2L, 3L, 4L, 5L), 10,
        10), EPSILON);

    // check valid border values of times
    assertEquals(.6, measureDynamism(asList(0L, 2L, 2L, 3L, 4L, 8L, 9L), 10,
        1), EPSILON);

    assertEquals(.1, measureDynamism(asList(0L, 2L, 2L, 3L, 4L, 8L, 9L), 100,
        5), EPSILON);

    assertEquals(2 / 7d,
        measureDynamism(asList(0L, 2L, 2L, 3L, 4L, 8L, 9L), 49,
            7), EPSILON);

    // both intervals
    assertEquals(1d,
        measureDynamism(asList(0L, 2L, 2L, 3L, 4L, 8L, 9L), 10,
            5), EPSILON);

    // no time
    assertEquals(0d,
        measureDynamism(Arrays.<Long> asList(), 10,
            5), EPSILON);

    // check interval borders
    assertEquals(0.5d,
        measureDynamism(asList(1000L, 1999L), 2000,
            1000), EPSILON);
    assertEquals(2 / 3d,
        measureDynamism(asList(1000L, 2000L), 3000,
            1000), EPSILON);

  }

  static List<Long> generateTimes(RandomGenerator rng, double intensity) {
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
    return times;
  }

  @Test
  public void dynamismTest601() {

    System.out.println(measureDynamism2ndDerivative(
        asList(0L, 1L, 1L, 2L, 2L, 3L, 3L, 4L, 4L, 5L, 5L, 6L, 6L, 7L, 7L, 8L,
            9L), 10L));

    System.out.println(measureDynamism2ndDerivative(
        asList(1L, 4L, 8L), 10L));
    System.out.println(measureDynamism2ndDerivative(
        asList(1L, 2L, 3L), 10L));

    System.out.println(measureDynamism2ndDerivative(
        asList(0L, 0L, 0L, 0L, 0L), 5L));
    System.out.println(measureDynamism2ndDerivative(
        asList(1L, 1L, 1L, 1L, 1L), 5L));
    System.out.println(measureDynamism2ndDerivative(
        asList(2L, 2L, 2L, 2L, 2L), 5L));
    System.out.println(measureDynamism2ndDerivative(
        asList(3L, 3L, 3L, 3L, 3L), 5L));
    System.out.println(measureDynamism2ndDerivative(
        asList(4L, 4L, 4L, 4L, 4L), 100L));

  }

  @Test
  public void dynamismTest2() {

    int length = 1000;
    final int granularity = 100;
    final double[] ordersPerHour = { 15d };// , 20d, 50d, 100d, 1000d };

    final StandardDeviation sd = new StandardDeviation();

    final RandomGenerator rng = new MersenneTwister(123L);

    final List<List<Long>> times = newArrayList();
    // for (int i = 0; i < 10; i++) {
    // times.add(generateTimes(rng));
    // }
    times.add(asList(250L, 500L, 750L));
    times.add(asList(100L, 500L, 750L));
    times.add(asList(100L, 200L, 300L, 400L, 500L, 600L, 700L, 800L, 900L));
    times.add(asList(100L, 200L, 300L, 399L, 500L, 600L, 700L, 800L, 900L));
    times
        .add(asList(50L, 150L, 250L, 350L, 450L, 550L, 650L, 750L, 850L, 950L));
    times
        .add(asList(50L, 150L, 250L, 350L, 450L, 551L, 650L, 750L, 850L, 950L));
    times.add(asList(250L, 500L, 750L));
    times.add(asList(0L, 50L, 55L, 57L, 59L, 60L, 100L, 150L, 750L));
    times.add(asList(5L, 5L, 5L, 5L));
    times.add(asList(4L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L,
        5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L));
    times.add(asList(5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L,
        5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L));
    times.add(asList(0L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L,
        5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L,
        999L));
    times.add(asList(500L, 500L, 500L, 500L));
    times.add(asList(5L, 5L, 5L, 5L, 400L, 410L, 430L, 440L, 800L, 810L, 820L,
        830L));
    times.add(asList(0L, 0L, 0L));
    times.add(asList(1L, 1L, 1L));
    times.add(asList(999L, 999L, 999L));
    times.add(asList(0L, 0L, 500L, 500L, 999L, 999L));
    times.add(asList(250L, 250L, 500L, 500L, 750L, 750L));
    times.add(asList(250L, 250L, 250L, 500L, 500L, 500L, 750L, 750L, 750L));

    for (int i = 0; i < 10; i++) {
      times.add(generateTimes(rng, 10d));
    }

    for (int i = 0; i < 10; i++) {
      times.add(generateTimes(rng, 30d));
    }
    for (int i = 0; i < 5; i++) {

      final List<Long> ts = generateTimes(rng, 50d);

      final List<Long> newTs = newArrayList();
      for (final long l : ts) {
        newTs.add(l);
        newTs.add(Math.min(999, Math.max(0, l
            + DoubleMath.roundToLong((rng.nextDouble() * 6d) - 3d,
                RoundingMode.HALF_EVEN))));
      }
      times.add(newTs);
    }

    for (int i = 0; i < 5; i++) {

      final List<Long> ts = generateTimes(rng, 100d);

      final List<Long> newTs = newArrayList();
      System.out.println("num events: " + ts.size());
      for (final long l : ts) {

        newTs.add(l);
        newTs.add(Math.min(999, Math.max(0, l
            + DoubleMath.roundToLong((rng.nextDouble() * 2d) - 1d,
                RoundingMode.HALF_EVEN))));
        newTs.add(Math.min(999, Math.max(0, l
            + DoubleMath.roundToLong((rng.nextDouble() * 2d) - 1d,
                RoundingMode.HALF_EVEN))));
      }
      times.add(newTs);
    }

    final List<Long> t = asList(100L, 300L, 500L, 700L, 900L);

    for (int i = 0; i < 15; i++) {
      final List<Long> c = newArrayList();
      for (int j = 0; j < i + 1; j++) {
        c.addAll(t);
      }
      Collections.sort(c);
      times.add(c);
    }

    for (int i = 0; i < 10; i++) {
      times.add(generateTimes(rng, (i + 1) * 100d));
    }

    final ImmutableList<Long> all = ContiguousSet.create(
        Range.closedOpen(0L, 1000L),
        DiscreteDomain.longs()).asList();

    times.add(all);

    final List<Long> more = newArrayList(all);
    for (final long l : all) {
      if (l % 2 == 0) {
        more.add(l);
      }
    }
    Collections.sort(more);
    times.add(more);

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
    times.add(more2);
    times.add(newMore);

    for (int k = 0; k < 5; k++) {
      final List<Long> ts = generateTimes(rng, 20);
      final List<Long> additions = newArrayList();
      for (int i = 0; i < ts.size(); i++) {

        if (i % 3 == 0) {
          for (int j = 0; j < k; j++) {
            additions.add(
                DoubleMath.roundToLong(ts.get(i) + (rng.nextDouble() * 10),
                    RoundingMode.HALF_DOWN));
          }
        }
      }
      ts.addAll(additions);
      Collections.sort(ts);
      times.add(ts);
    }

    for (int j = 0; j < ordersPerHour.length; j++) {
      final List<Double> dodValues = newArrayList();
      final List<Double> dodValues2 = newArrayList();
      final List<Double> dodValues3 = newArrayList();
      final List<Integer> numOrders = newArrayList();
      System.out.println("=========" + ordersPerHour[j] + "=========");
      for (int i = 0; i < times.size(); i++) {

        System.out.println("----- " + i + " -----");
        // System.out.println(times.get(i));

        // final ArrivalTimesGenerator atg = new PoissonProcessArrivalTimes(
        // length,
        // ordersPerHour[j] / 60d, 1);
        // final List<Long> times = atg.generate(rng);
        if (i == 78) {
          length *= 10;
        }
        final double dod = measureDynamism(times.get(i), length, granularity);

        final double dod2 = measureDynamismDistr(times.get(i), length);
        final double dod3 = measureDynamismDistr2(times.get(i), length);
        final double dod4 = measureDynamismDistr3(times.get(i), length);
        final double dod5 = measureDynamism2ndDerivative(times.get(i), length);
        // System.out.println(dod);
        System.out.printf("%1.3f%%\n", dod2 * 100d);
        System.out.printf("%1.3f%%\n", dod5 * 100d);
        // System.out.printf("%1.3f%%\n", dod4 * 100d);
        // System.out.println(dod3);
        numOrders.add(times.get(i).size());
        dodValues.add(dod);
        dodValues2.add(dod2);
        dodValues3.add(dod3);

        // Analysis.writeLoads(times, new File("files/generator/times/orders"
        // ));

        try {
          final File dest = new File(
              "files/generator/times/orders" + i
                  + ".times");
          Files.createParentDirs(dest);
          Files.write(Joiner.on("\n").join(times.get(i)), dest, Charsets.UTF_8);
        } catch (final IOException e) {
          throw new IllegalArgumentException();
        }

      }

      System.out.printf(
          "dod: \t%1.3f \t+- %1.3f\n",
          DoubleMath.mean(dodValues) * 100d,
          sd.evaluate(Doubles.toArray(dodValues)) * 100d);
      System.out.printf(
          "dod2: \t%1.3f \t+- %1.3f\n",
          DoubleMath.mean(dodValues2),
          sd.evaluate(Doubles.toArray(dodValues2)));
      System.out.printf(
          "dod2: \t%1.3f \t+- %1.3f\n",
          DoubleMath.mean(dodValues3),
          sd.evaluate(Doubles.toArray(dodValues3)));
      System.out.printf("size: \t%1.3f \t+- %1.3f\n",
          DoubleMath.mean(numOrders),
          sd.evaluate(Doubles.toArray(numOrders)));
    }

  }

  /**
   * Length of day must be positive.
   */
  @Test(expected = IllegalArgumentException.class)
  public void dynamismIllegalArgument1() {
    measureDynamism(asList(1L), 0, 7);
  }

  /**
   * Granularity must be <= length of day.
   */
  @Test(expected = IllegalArgumentException.class)
  public void dynamismIllegalArgument2() {
    measureDynamism(asList(1L), 1, 7);
  }

  /**
   * Granularity needs to fit an exact number of times in length of day.
   */
  @Test(expected = IllegalArgumentException.class)
  public void dynamismIllegalArgument3() {
    measureDynamism(asList(1L, 2L), 10, 7);
  }

  /**
   * Times can not be >= lengthOfDay.
   */
  @Test(expected = IllegalArgumentException.class)
  public void dynamismIllegalArgument4a() {
    measureDynamism(asList(1L, 2L, 10L), 10, 1);
  }

  /**
   * Times can not be < 0.
   */
  @Test(expected = IllegalArgumentException.class)
  public void dynamismIllegalArgument4b() {
    measureDynamism(asList(1L, 2L, -1L), 10, 1);
  }

  /**
   * Tests whether histogram is computed correctly.
   */
  @Test
  public void testHistogram() {
    assertEquals(ImmutableSortedMultiset.of(),
        Metrics.computeHistogram(Arrays.<Double> asList(), .2));

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
