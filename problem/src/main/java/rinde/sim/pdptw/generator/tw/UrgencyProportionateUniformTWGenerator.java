/**
 * 
 */
package rinde.sim.pdptw.generator.tw;

import static com.google.common.base.Preconditions.checkArgument;
import static rinde.sim.pdptw.generator.Metrics.travelTime;

import java.math.RoundingMode;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.util.TimeWindow;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class UrgencyProportionateUniformTWGenerator implements
    TimeWindowGenerator {

  private final Point depotLocation;
  private final long endTime;
  private final long serviceTime;
  private final long minResponseTime;
  private final long maxResponseTime;
  private final double vehicleSpeed; // km/h

  /**
   * @param depotLocation The location of the depot.
   * @param endTime
   * @param serviceTime
   * @param minResponseTime
   * @param vehicleSpeed
   */
  public UrgencyProportionateUniformTWGenerator(Point depotLocation,
      long endTime,
      long serviceTime, long minResponseTime, long maxResponseTime,
      double vehicleSpeed) {
    checkArgument(maxResponseTime >= minResponseTime + serviceTime);
    this.depotLocation = depotLocation;
    this.endTime = endTime;
    this.serviceTime = serviceTime;
    this.minResponseTime = minResponseTime;
    this.maxResponseTime = maxResponseTime;
    this.vehicleSpeed = vehicleSpeed;
  }

  @Override
  public ImmutableList<TimeWindow> generate(long orderAnnounceTime,
      Point pickup, Point delivery, RandomGenerator rng) {
    checkArgument(
        orderAnnounceTime <= endTime
            - (minResponseTime + travelTime(pickup, delivery, vehicleSpeed)
                + (2 * serviceTime) + travelTime(delivery, depotLocation,
                  vehicleSpeed)), "The orderAnnounceTime is infeasible.");

    // largely inspired on Gendreau et al. method, but changed to ensure
    // feasibility and strictness of all time windows.
    /*
     * GLOBAL
     */
    final long intertt = travelTime(pickup, delivery, vehicleSpeed);
    final long earliestPickupStartTime = orderAnnounceTime + minResponseTime;
    final long latestDeliveryEndTime = endTime
        - (travelTime(delivery, depotLocation, vehicleSpeed) + serviceTime);
    final long latestPickupEndTime = orderAnnounceTime + maxResponseTime;
    // latestDeliveryEndTime - (intertt + serviceTime);

    /*
     * PICKUP
     */
    final long middlePickup = earliestPickupStartTime
        + DoubleMath
            .roundToLong((latestPickupEndTime - earliestPickupStartTime) / 2,
                RoundingMode.HALF_EVEN);

    final double betaPickup = uniform(.6, 1.0, rng);

    long pickLeft;
    if (rng.nextDouble() <= betaPickup) {
      pickLeft = earliestPickupStartTime
          + DoubleMath.roundToLong((middlePickup - earliestPickupStartTime)
              * rng.nextDouble(), RoundingMode.HALF_EVEN);
    } else {
      pickLeft = middlePickup
          + DoubleMath.roundToLong((latestPickupEndTime - middlePickup)
              * rng.nextDouble(), RoundingMode.HALF_EVEN);
    }
    // final long pickupRemainingTime = latestPickupEndTime - pickLeft;
    // final double deltaPickup = uniform(.1, .8, rng);

    pickLeft = orderAnnounceTime;
    long pickRight = latestPickupEndTime;
    // + DoubleMath
    // .roundToLong(deltaPickup * pickupRemainingTime,
    // RoundingMode.HALF_EVEN);

    /*
     * DELIVERY
     */
    final long earliestDeliveryStartTime = pickLeft + serviceTime + intertt;
    final long middleDelivery = earliestDeliveryStartTime
        + DoubleMath
            .roundToLong(
                (latestDeliveryEndTime - earliestDeliveryStartTime) / 2,
                RoundingMode.HALF_EVEN);

    final double betaDeliver = uniform(.6, 1.0, rng);
    long deliverLeft;
    if (rng.nextDouble() <= betaDeliver) {
      deliverLeft = earliestDeliveryStartTime
          + DoubleMath.roundToLong((middleDelivery - earliestDeliveryStartTime)
              * rng.nextDouble(), RoundingMode.HALF_EVEN);
    } else {
      deliverLeft = middleDelivery
          + DoubleMath.roundToLong((latestDeliveryEndTime - middleDelivery)
              * rng.nextDouble(), RoundingMode.HALF_EVEN);
    }
    final long deliverRemainingTime = latestDeliveryEndTime - deliverLeft;
    final double deltaDeliver = uniform(.3, 1.0, rng);
    final long deliverRight = deliverLeft
        + DoubleMath
            .roundToLong(deltaDeliver * deliverRemainingTime,
                RoundingMode.HALF_EVEN);

    // adapt pickRight to make it strict
    pickRight = Math.min(pickRight, deliverRight - intertt - serviceTime);

    // if (deliverLeft < pickLeft + serviceTime + travelTime(pickup,
    // delivery)) {
    // System.out.println(earliestDeliveryStartTime);
    // System.out.println(latestDeliveryEndTime);
    // System.out.println(middleDelivery);
    // System.out.println(deliverRemainingTime);
    // System.out.println(deliverLeft);
    // }

    // System.out.println(pickRight - pickLeft);
    return ImmutableList
        .of(new TimeWindow(pickLeft, pickRight), new TimeWindow(deliverLeft,
            deliverRight));
  }

  /**
   * Draws a random number from the uniform distribution U(lb,ub)
   * @param lb
   * @param ub
   * @param rng
   * @return
   */
  static double uniform(double lb, double ub, RandomGenerator rng) {
    return lb + rng.nextDouble() * Math.abs(ub - lb);
  }

}
