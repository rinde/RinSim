package rinde.sim.pdptw.scenario;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static rinde.sim.pdptw.scenario.TimeWindows.builder;
import static rinde.sim.util.SupplierRngs.constant;
import static rinde.sim.util.SupplierRngs.normal;
import static rinde.sim.util.SupplierRngs.uniformLong;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.common.ParcelDTO;
import rinde.sim.pdptw.scenario.ScenarioGenerator.TravelTimes;
import rinde.sim.pdptw.scenario.TimeWindows.TimeWindowGenerator;
import rinde.sim.util.TestUtil;
import rinde.sim.util.TimeWindow;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

@RunWith(Parameterized.class)
public class TimeWindowsTest {
  private final TimeWindowGenerator timeWindowGenerator;

  public TimeWindowsTest(TimeWindowGenerator twg) {
    timeWindowGenerator = twg;
    TestUtil.testPrivateConstructor(TimeWindows.class);
  }

  @Parameters
  public static Iterable<Object[]> parameters() {

    return c(ImmutableList.<Object> of(
        builder()
            .build(),
        builder()
            .pickupUrgency(uniformLong(0, 10))
            .build(),
        builder()
            .urgency(uniformLong(0, 10))
            .timeWindowLength(
                normal().bounds(0, 10).mean(5).std(3).longSupplier())
            .build(),
        builder()
            .urgency(constant(0L))
            .timeWindowLength(constant(0L))
            .build()));
  }

  static Iterable<Object[]> c(Iterable<Object> in) {
    final List<Object[]> list = newArrayList();
    for (final Object o : in) {
      list.add(new Object[] { o });
    }
    return list;
  }

  static Iterable<ParcelDTO.Builder> parcelBuilders() {
    final Iterator<Point> locations = Iterators.cycle(asList(
        new Point(3, 3),
        new Point(8, 1),
        new Point(2, 0),
        new Point(0, 0),
        new Point(1, 1),
        new Point(7, 6)
        ));

    final Iterator<Long> serviceDurations = Iterators.cycle(asList(
        0L, 0L, 1L, 2L, 5L, 10L, 10L));

    final Iterator<Long> arrivalTimes = Iterators.cycle(asList(0L, 50L, 85L));

    final List<ParcelDTO.Builder> builders = newArrayList();
    for (int i = 0; i < 50; i++) {
      builders.add(
          ParcelDTO.builder(locations.next(), locations.next())
              .arrivalTime(arrivalTimes.next())
              .pickupDuration(serviceDurations.next())
              .deliveryDuration(serviceDurations.next()));
    }
    return builders;
  }

  @Test
  public void determinismTest() {
    final RandomGenerator rng = new MersenneTwister(123L);
    for (final TravelTimes tt : FakeTravelTimes.values()) {
      for (final ParcelDTO.Builder parcelBuilder : parcelBuilders()) {
        for (int i = 0; i < 10; i++) {
          final long seed = rng.nextLong();
          timeWindowGenerator.generate(seed, parcelBuilder, tt, 100);
          final TimeWindow p1 = parcelBuilder.getPickupTimeWindow();
          final TimeWindow d1 = parcelBuilder.getDeliveryTimeWindow();

          timeWindowGenerator.generate(seed, parcelBuilder, tt, 100);
          final TimeWindow p2 = parcelBuilder.getPickupTimeWindow();
          final TimeWindow d2 = parcelBuilder.getDeliveryTimeWindow();
          assertNotSame(p1, p2);
          assertNotSame(d1, d2);
          assertEquals(p1, p2);
          assertEquals(d1, d2);
        }
      }
    }
  }

  /**
   * Tests the generated time windows on two properties:
   * <ul>
   * <li>The distance between pickupTW.begin and deliveryTW.begin</li>
   * <li>The distance between pickupTW.end and deliveryTW.end</li>
   * </ul>
   */
  @Test
  public void overlapTest() {
    final RandomGenerator rng = new MersenneTwister(123L);
    final long endTime = 100;
    for (final TravelTimes tt : FakeTravelTimes.values()) {
      for (final ParcelDTO.Builder parcelBuilder : parcelBuilders()) {
        for (int i = 0; i < 10; i++) {
          timeWindowGenerator
              .generate(rng.nextLong(), parcelBuilder, tt, endTime);

          final long pickDelTT = tt.getShortestTravelTime(
              parcelBuilder.getPickupLocation(),
              parcelBuilder.getDeliveryLocation());

          final TimeWindow pickTW = parcelBuilder.getPickupTimeWindow();
          final TimeWindow delTW = parcelBuilder.getDeliveryTimeWindow();
          final long pickDur = parcelBuilder.getPickupDuration();

          assertTrue(pickTW.begin >= 0);

          assertTrue(
              i + " " + tt + " " + pickTW + " " + delTW,
              pickTW.end <= delTW.end + pickDelTT + pickDur);
          assertTrue(i + " " + tt + " " + pickTW + " " + delTW + " "
              + pickDelTT + " " + pickDur,
              delTW.begin >= pickTW.begin + pickDelTT + pickDur);
        }
      }
    }
  }

  static enum FakeTravelTimes implements TravelTimes {
    DISTANCE {
      private final Point DEPOT_LOC = new Point(0, 0);

      @Override
      public long getShortestTravelTime(Point from, Point to) {
        return (long) Point.distance(from, to);
      }

      @Override
      public long getTravelTimeToNearestDepot(Point from) {
        return (long) Point.distance(from, DEPOT_LOC);
      }
    },
    ZEROS {
      @Override
      public long getShortestTravelTime(Point from, Point to) {
        return 0;
      }

      @Override
      public long getTravelTimeToNearestDepot(Point from) {
        return 0;
      }
    }

  }
}
