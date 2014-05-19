package rinde.sim.pdptw.scenario;

import static com.google.common.base.Preconditions.checkArgument;
import static rinde.sim.util.SupplierRngs.constant;

import java.math.RoundingMode;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.common.ParcelDTO;
import rinde.sim.pdptw.scenario.ScenarioGenerator.TravelTimes;
import rinde.sim.util.SupplierRng;
import rinde.sim.util.TimeWindow;

import com.google.common.base.Optional;
import com.google.common.math.DoubleMath;

/**
 * Utility class for creating {@link TimeWindowGenerator}s.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class TimeWindows {
  private TimeWindows() {}

  /**
   * @return A new {@link Builder} instance for creating
   *         {@link TimeWindowGenerator}s.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Generator of {@link TimeWindow}s for pickup and delivery problems.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public interface TimeWindowGenerator {
    /**
     * Should create two {@link TimeWindow}s, a pickup time window and a
     * delivery time window. These time windows should be theoretically
     * feasible, meaning that they should be serviceable such that there is
     * enough time for a vehicle to return to the depot.
     * @param seed Random seed.
     * @param parcelBuilder The {@link rinde.sim.pdptw.common.ParcelDTO.Builder}
     *          that is being used for creating a {@link ParcelDTO}. The time
     *          windows should be added to this builder via the
     *          {@link rinde.sim.pdptw.common.ParcelDTO.Builder#pickupTimeWindow(TimeWindow)}
     *          and
     *          {@link rinde.sim.pdptw.common.ParcelDTO.Builder#deliveryTimeWindow(TimeWindow)}
     *          methods.
     * @param travelTimes An object that provides information about the travel
     *          times in the scenario.
     * @param endTime The end time of the scenario.
     */
    void generate(long seed, ParcelDTO.Builder parcelBuilder,
        TravelTimes travelTimes, long endTime);
  }

  /**
   * A builder for creating {@link TimeWindow} instances using urgency. Urgency
   * is defined as follows:
   * <ul>
   * <li><code>pickup_urgency = pickupTW.R - orderAnnounceTime</code></li>
   * <li>
   * <code>delivery_urgency = deliveryTW.R - earliest possible leave time from pickup site</code>
   * </li>
   * </ul>
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static class Builder {
    private static final SupplierRng<Long> DEFAULT_URGENCY = constant(30 * 60 * 1000L);
    private static final SupplierRng<Long> DEFAULT_PICKUP_LENGTH = constant(10 * 60 * 1000L);
    private static final SupplierRng<Long> DEFAULT_DELIVERY_OPENING = constant(0L);
    private static final SupplierRng<Double> DEFAULT_DELIVERY_LENGTH_FACTOR = constant(2d);

    SupplierRng<Long> pickupUrgency;
    SupplierRng<Long> pickupTWLength;
    SupplierRng<Long> deliveryOpening;
    SupplierRng<Double> deliveryLengthFactor;
    Optional<SupplierRng<Long>> minDeliveryLength;

    Builder() {
      pickupUrgency = DEFAULT_URGENCY;
      pickupTWLength = DEFAULT_PICKUP_LENGTH;
      deliveryOpening = DEFAULT_DELIVERY_OPENING;
      deliveryLengthFactor = DEFAULT_DELIVERY_LENGTH_FACTOR;
      minDeliveryLength = Optional.absent();
    }

    public Builder pickupUrgency(SupplierRng<Long> urgency) {
      pickupUrgency = urgency;
      return this;
    }

    public Builder pickupTimeWindowLength(SupplierRng<Long> length) {
      pickupTWLength = length;
      return this;
    }

    /**
     * Sets the opening of the delivery time window. The value is interpreted as
     * the time relative to the earliest feasible delivery opening time:
     * <code>pickup opening + pickup duration + travel time from pickup to delivery</code>
     * .
     * @param opening May only return values which are <code>>= 0</code>.
     * @return This, as per the builder pattern.
     */
    public Builder deliveryOpening(SupplierRng<Long> opening) {
      deliveryOpening = opening;
      return this;
    }

    /**
     * 
     * @param factor May only return values which are <code>> 0</code>.
     * @return
     */
    // length of delivery TW as a ratio to length of pickup TW
    public Builder deliveryLengthFactor(SupplierRng<Double> factor) {
      deliveryLengthFactor = factor;
      return this;
    }

    public Builder minDeliveryLength(SupplierRng<Long> del) {
      minDeliveryLength = Optional.of(del);
      return this;
    }

    public TimeWindowGenerator build() {
      return new DefaultTimeWindowGenerator(this);
    }
  }

  static class DefaultTimeWindowGenerator implements TimeWindowGenerator {
    private final RandomGenerator rng;
    private final SupplierRng<Long> pickupUrgency;
    private final SupplierRng<Long> pickupTWLength;
    private final SupplierRng<Long> deliveryOpening;
    private final SupplierRng<Double> deliveryLengthFactor;
    private final Optional<SupplierRng<Long>> minDeliveryLength;

    DefaultTimeWindowGenerator(Builder b) {
      rng = new MersenneTwister();
      pickupUrgency = b.pickupUrgency;
      pickupTWLength = b.pickupTWLength;
      deliveryOpening = b.deliveryOpening;
      deliveryLengthFactor = b.deliveryLengthFactor;
      minDeliveryLength = b.minDeliveryLength;
    }

    @Override
    public void generate(long seed, ParcelDTO.Builder parcelBuilder,
        TravelTimes travelModel, long endTime) {
      rng.setSeed(seed);
      final long orderAnnounceTime = parcelBuilder.getOrderAnnounceTime();
      final Point pickup = parcelBuilder.getPickupLocation();
      final Point delivery = parcelBuilder.getDeliveryLocation();

      final long pickupToDeliveryTT = travelModel.getShortestTravelTime(pickup,
          delivery);
      final long deliveryToDepotTT = travelModel
          .getTravelTimeToNearestDepot(delivery);

      // PICKUP
      final long earliestPickupOpening = orderAnnounceTime;
      final long earliestPickupClosing = earliestPickupOpening;

      final long latestPickupClosing = endTime - deliveryToDepotTT
          - pickupToDeliveryTT - parcelBuilder.getPickupDuration()
          - parcelBuilder.getDeliveryDuration();
      final TimeWindow pickupTW = urgencyTimeWindow(earliestPickupOpening,
          earliestPickupClosing, latestPickupClosing, pickupUrgency,
          pickupTWLength);

      // DELIVERY
      final long earliestDeliveryOpening = pickupTW.begin + pickupToDeliveryTT
          + parcelBuilder.getPickupDuration();
      final long latestDeliveryOpening = endTime - deliveryToDepotTT;

      final long delOpen = deliveryOpening.get(rng.nextLong());
      checkArgument(delOpen >= 0);
      long delOpening = Math.min(earliestDeliveryOpening + delOpen,
          latestDeliveryOpening);
      delOpening = Math.max(delOpening, earliestDeliveryOpening);

      final long earliestDeliveryClosing = pickupTW.end + pickupToDeliveryTT
          + parcelBuilder.getPickupDuration();
      final long latestDeliveryClosing = endTime - deliveryToDepotTT
          - parcelBuilder.getDeliveryDuration();

      final double delFactor = deliveryLengthFactor.get(rng.nextLong());
      checkArgument(delFactor > 0d);
      long deliveryClosing = DoubleMath.roundToLong(pickupTW.length()
          * delFactor, RoundingMode.CEILING);

      if (minDeliveryLength.isPresent()) {
        deliveryClosing = Math.max(
            delOpening + minDeliveryLength.get().get(rng.nextLong()),
            deliveryClosing);
      }

      final long boundedDelClose = boundValue(deliveryClosing,
          earliestDeliveryClosing, latestDeliveryClosing);

      final TimeWindow deliveryTW = new TimeWindow(delOpening,
          boundedDelClose);

      parcelBuilder.pickupTimeWindow(pickupTW);
      parcelBuilder.deliveryTimeWindow(deliveryTW);
    }

    static long boundValue(long value, long lowerBound, long upperBound) {
      return Math.max(lowerBound, Math.min(value, upperBound));
    }

    TimeWindow urgencyTimeWindow(long earliestOpening, long earliestClosing,
        long latestClosing, SupplierRng<Long> urgency, SupplierRng<Long> length) {
      final long closing = boundValue(
          earliestClosing + urgency.get(rng.nextLong()), earliestClosing,
          latestClosing);
      final long opening = boundValue(closing - length.get(rng.nextLong()),
          earliestOpening, closing);
      return new TimeWindow(opening, closing);
    }
  }
}
