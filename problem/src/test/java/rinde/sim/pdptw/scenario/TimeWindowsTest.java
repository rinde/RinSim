package rinde.sim.pdptw.scenario;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static rinde.sim.pdptw.scenario.TimeWindows.builder;

import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.common.ParcelDTO;
import rinde.sim.pdptw.scenario.ScenarioGenerator.TravelTimes;
import rinde.sim.pdptw.scenario.TimeWindows.TimeWindowGenerator;
import rinde.sim.util.SupplierRngs;
import rinde.sim.util.TestUtil;
import rinde.sim.util.TimeWindow;

import com.google.common.collect.ImmutableList;

@RunWith(Parameterized.class)
public class TimeWindowsTest {
  private final TimeWindowGenerator timeWindowGenerator;

  public TimeWindowsTest(TimeWindowGenerator twg) {
    timeWindowGenerator = twg;
    TestUtil.testPrivateConstructor(TimeWindows.class);
  }

  @Parameters
  public static List<Object[]> parameters() {
    return ImmutableList.of(
        new Object[] { builder().build() },
        new Object[] { builder()
            .pickupUrgency(SupplierRngs.uniformLong(0, 10))
            .build() },
        new Object[] { builder()
            .urgency(SupplierRngs.uniformLong(0, 10))
            .timeWindowLength(
                SupplierRngs.normal().bounds(0, 10).longSupplier())
            .build() }

        );
  }

  @Test
  public void determinismTest() {
    final RandomGenerator rng = new MersenneTwister(123L);
    for (final TravelTimes tt : FakeTravelTimes.values()) {
      for (int i = 0; i < 10; i++) {
        final long seed = rng.nextLong();
        final ParcelDTO.Builder builder = ParcelDTO.builder(new Point(0, 0),
            new Point(10, 10));
        timeWindowGenerator.generate(seed, builder, tt, 100);
        final TimeWindow p1 = builder.getPickupTimeWindow();
        final TimeWindow d1 = builder.getDeliveryTimeWindow();

        timeWindowGenerator.generate(seed, builder, tt, 100);
        final TimeWindow p2 = builder.getPickupTimeWindow();
        final TimeWindow d2 = builder.getDeliveryTimeWindow();
        assertNotSame(p1, p2);
        assertNotSame(d1, d2);
        assertEquals(p1, p2);
        assertEquals(d1, d2);
      }
    }

  }

  // FIXME fix this test
  @Ignore
  @Test
  public void overlapTest() {
    final RandomGenerator rng = new MersenneTwister(123L);
    final long endTime = 100;
    for (final TravelTimes tt : FakeTravelTimes.values()) {

      // TODO use different:
      // pickup durations
      // delivery durations
      // pickup locations
      // delivery locations

      for (int i = 0; i < 100; i++) {
        final Point p1 = new Point(0, 0);
        final Point p2 = new Point(10, 10);
        final ParcelDTO.Builder builder = ParcelDTO.builder(p1, p2);
        builder.arrivalTime(rng.nextInt(50));
        timeWindowGenerator.generate(rng.nextLong(), builder, tt, endTime);

        final long pickDelTT = tt.getShortestTravelTime(p1, p2);
        final long delDepTT = tt.getTravelTimeToNearestDepot(p2);

        assertTrue(builder.getPickupTimeWindow().end <= builder
            .getDeliveryTimeWindow().end + pickDelTT
            + builder.getPickupDuration());

        assertTrue(builder.getPickupTimeWindow().begin
            + pickDelTT + builder.getPickupDuration() <= builder
              .getDeliveryTimeWindow().begin);
        assertTrue(builder.getDeliveryTimeWindow().end <= endTime
            - delDepTT + builder.getDeliveryDuration());
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
