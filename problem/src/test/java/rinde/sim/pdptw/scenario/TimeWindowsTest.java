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
  private static final long END_TIME = 100;
  private final TimeWindowGenerator timeWindowGenerator;
  private final double meanPickupUrgency;
  private final double meanDeliveryUrgency;

  public TimeWindowsTest(TimeWindowGenerator twg, double mpu, double mdu) {
    timeWindowGenerator = twg;
    meanPickupUrgency = mpu;
    meanDeliveryUrgency = mdu;
    TestUtil.testPrivateConstructor(TimeWindows.class);
  }

  @Parameters
  public static Iterable<Object[]> parameters() {

    return ImmutableList.of(
        new Object[] {
            builder()
                .urgency(constant(1L))
                .timeWindowLength(constant(0L))
                .build(),
            1d,
            1d
        },
        new Object[] {
            builder()
                .pickupUrgency(uniformLong(0, 10))
                .build(),
            5d,
            1d,
        },
        new Object[] {
            builder()
                .urgency(uniformLong(0, 10))
                .timeWindowLength(
                    normal().bounds(0, 10).mean(5).std(3).buildLong())
                .build(),
            5d,
            5d,
        },
        new Object[] {
            builder()
                .urgency(constant(0L))
                .timeWindowLength(constant(0L))
                .build(),
            0d,
            0d
        });
  }

  static Iterable<Object[]> c(Iterable<?> in) {
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
              .orderAnnounceTime(arrivalTimes.next())
              .pickupDuration(serviceDurations.next())
              .deliveryDuration(serviceDurations.next()));
    }
    return builders;
  }

  /**
   * Test whether calling generate with the same seed yields equal results.
   */
  @Test
  public void determinismTest() {
    final RandomGenerator rng = new MersenneTwister(123L);
    for (final TravelTimes tt : FakeTravelTimes.values()) {
      for (final ParcelDTO.Builder parcelBuilder : parcelBuilders()) {
        for (int i = 0; i < 10; i++) {
          final long seed = rng.nextLong();
          timeWindowGenerator.generate(seed, parcelBuilder, tt, END_TIME);
          final TimeWindow p1 = parcelBuilder.getPickupTimeWindow();
          final TimeWindow d1 = parcelBuilder.getDeliveryTimeWindow();

          timeWindowGenerator.generate(seed, parcelBuilder, tt, END_TIME);
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

    for (final TravelTimes tt : FakeTravelTimes.values()) {
      for (final ParcelDTO.Builder parcelBuilder : parcelBuilders()) {
        for (int i = 0; i < 10; i++) {
          timeWindowGenerator
              .generate(rng.nextLong(), parcelBuilder, tt, END_TIME);

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

  @Test
  public void urgencyTest() {
    final RandomGenerator rng = new MersenneTwister(123L);
    for (final TravelTimes tt : FakeTravelTimes.values()) {
      for (final ParcelDTO.Builder parcelBuilder : parcelBuilders()) {
        // in this case urgency can no longer be guaranteed
        if (parcelBuilder.getOrderAnnounceTime() >= 85) {
          continue;
        }
        double pmean = 0;
        final double repetitions = 10;
        for (int i = 0; i < repetitions; i++) {
          timeWindowGenerator
              .generate(rng.nextLong(), parcelBuilder, tt, END_TIME);
          final ParcelDTO dto = parcelBuilder.build();
          pmean += Metrics.pickupUrgency(dto);
        }
        pmean /= repetitions;
        assertEquals(meanPickupUrgency, pmean, 0.01);
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
