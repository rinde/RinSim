/**
 * 
 */
package rinde.sim.pdptw.generator;

import static org.junit.Assert.assertTrue;
import static rinde.sim.pdptw.generator.Metrics.travelTime;

import java.math.RoundingMode;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.generator.tw.ProportionateUniformTWGenerator;
import rinde.sim.util.TimeWindow;

import com.google.common.math.DoubleMath;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class ProportionateUniformTWGeneratorTest {

  @Test
  public void test() {
    final long serviceTime = 5;
    final long endTime = 180;
    final Point depotLocation = new Point(5, 5);
    final double vehicleSpeed = 40;
    final ProportionateUniformTWGenerator twg = new ProportionateUniformTWGenerator(
        depotLocation, endTime, serviceTime, 30, vehicleSpeed);
    final RandomGenerator rng = new MersenneTwister(123);
    for (int i = 0; i < 10000; i++) {
      final Point p1 = new Point(6, 6);
      final Point p2 = new Point(4, 4);
      final List<TimeWindow> tws = twg.generate(rng.nextLong(), DoubleMath
          .roundToLong(rng.nextDouble() * 120, RoundingMode.HALF_DOWN), p1, p2);
      assertTrue(tws.toString(), tws.get(0).end <= tws.get(1).end
          + travelTime(p1, p2, vehicleSpeed) + serviceTime);
      assertTrue(
          tws.toString() + " tt: " + travelTime(p1, p2, vehicleSpeed),
          tws.get(0).begin
              + travelTime(p1, p2, vehicleSpeed) + serviceTime <= tws.get(1).begin);
      assertTrue(tws.get(1).end <= endTime
          - (travelTime(p2, depotLocation, vehicleSpeed) + serviceTime));

    }
  }
}
