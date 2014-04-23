/**
 * 
 */
package rinde.sim.pdptw.scenario;

import static org.junit.Assert.assertTrue;
import static rinde.sim.pdptw.measure.Metrics.travelTime;

import java.math.RoundingMode;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Ignore;
import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.common.ParcelDTO;

import com.google.common.math.DoubleMath;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class ProportionateUniformTWGeneratorTest {

  @Ignore
  @Test
  public void test() {
    final long serviceTime = 5;
    final long endTime = 180;
    final Point depotLocation = new Point(5, 5);
    final double vehicleSpeed = 40;
    final Object twg = null;
    // new ProportionateUniformTWGenerator(
    // depotLocation, endTime, serviceTime, 30, vehicleSpeed);
    final RandomGenerator rng = new MersenneTwister(123);
    for (int i = 0; i < 10000; i++) {
      final Point p1 = new Point(6, 6);
      final Point p2 = new Point(4, 4);

      final ParcelDTO.Builder b = ParcelDTO.builder(p1, p2);
      b.arrivalTime(DoubleMath.roundToLong(rng.nextDouble() * 120,
          RoundingMode.HALF_DOWN));
      b.serviceDuration(serviceTime);

      // twg.generate(rng.nextLong(), b, ImmutableList.of(depotLocation),
      // ImmutableList.of(vehicleSpeed));
      assertTrue(b.getPickupTimeWindow().end <= b.getDeliveryTimeWindow().end
          + travelTime(p1, p2, vehicleSpeed) + serviceTime);
      assertTrue(b.getPickupTimeWindow().begin
          + travelTime(p1, p2, vehicleSpeed) + serviceTime <= b
            .getDeliveryTimeWindow().begin);
      assertTrue(b.getDeliveryTimeWindow().end <= endTime
          - (travelTime(p2, depotLocation, vehicleSpeed) + serviceTime));

    }
  }
}
