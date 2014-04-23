package rinde.sim.pdptw.scenario;

import static rinde.sim.util.SupplierRngs.constant;

import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.pdptw.common.ParcelDTO;
import rinde.sim.util.SupplierRng;
import rinde.sim.util.TimeWindow;

public final class TimeWindows {
  private TimeWindows() {}

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Generator of {@link TimeWindow}s for pickup and delivery problems.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public interface TimeWindowGenerator {
    /**
     * Should return two {@link TimeWindow}s, a pickup time window and a
     * delivery time window. These time windows should be theoretically
     * feasible, meaning that they should be serviceable such that there is
     * enough time for a vehicle to return to the depot.
     * @param seed Random seed.
     * @param orderAnnounceTime The time at which the order is announced.
     * @param pickup Position of pickup.
     * @param delivery Position of delivery.
     * @return A list containing exactly two {@link TimeWindow}s. The first
     *         indicates the <i>pickup time window</i> the second indicates the
     *         <i>delivery time window</i>.
     */
    void generate(long seed, ParcelDTO.Builder parcelBuilder,
        TravelModel travelModel, long endTime);
  }

  static class DefaultTimeWindowGenerator implements TimeWindowGenerator {

    private final RandomGenerator rng;
    private final SupplierRng<Long> pickupUrgency;
    private final SupplierRng<Long> deliveryUrgency;
    private final SupplierRng<Long> pickupTWLength;
    private final SupplierRng<Long> deliveryTWLength;

    public DefaultTimeWindowGenerator(Builder b) {
      rng = new MersenneTwister();
      pickupUrgency = b.pickupUrgency;
      deliveryUrgency = b.deliveryUrgency;
      pickupTWLength = b.pickupTWLength;
      deliveryTWLength = b.deliveryTWLength;
    }

    @Override
    public void generate(long seed, ParcelDTO.Builder parcelBuilder,
        TravelModel travelModel, long endTime) {
      rng.setSeed(seed);
      final long orderAnnounceTime = parcelBuilder.getOrderArrivalTime();
      final Point pickup = parcelBuilder.getPickupLocation();
      final Point delivery = parcelBuilder.getDestinationLocation();

      final long pickupToDeliveryTT = travelModel.getShortestTravelTime(pickup,
          delivery);
      final long deliveryToDepotTT = travelModel
          .getTravelTimeToNearestDepot(delivery);

      // PICKUP
      final long earliestPickupOpening = orderAnnounceTime;
      final long latestPickupClosing = endTime - deliveryToDepotTT
          - pickupToDeliveryTT - parcelBuilder.getPickupDuration()
          - parcelBuilder.getDeliveryDuration();
      final TimeWindow pickupTW = urgencyTimeWindow(earliestPickupOpening,
          latestPickupClosing, pickupUrgency, pickupTWLength);

      // DELIVERY
      final long earliestDeliveryOpening = pickupTW.begin + pickupToDeliveryTT
          + parcelBuilder.getPickupDuration();
      final long latestDeliveryClosing = endTime - deliveryToDepotTT
          + parcelBuilder.getDeliveryDuration();
      final TimeWindow deliveryTW = urgencyTimeWindow(earliestDeliveryOpening,
          latestDeliveryClosing, deliveryUrgency, deliveryTWLength);

      parcelBuilder.pickupTimeWindow(pickupTW);
      parcelBuilder.deliveryTimeWindow(deliveryTW);
    }

    TimeWindow urgencyTimeWindow(long earliestOpening,
        long latestClosing, SupplierRng<Long> urgency, SupplierRng<Long> length) {

      final long closing = earliestOpening + urgency.get(rng.nextLong());
      final long roundedClosing =
          Math.max(earliestOpening, Math.min(closing, latestClosing));

      final long opening = roundedClosing - length.get(rng.nextLong());
      final long roundedOpening =
          Math.max(earliestOpening, Math.min(opening, roundedClosing));
      return new TimeWindow(roundedOpening, roundedClosing);
    }
  }

  public static class Builder {

    private static final SupplierRng<Long> DEFAULT_URGENCY = constant(30 * 60 * 1000L);
    private static final SupplierRng<Long> DEFAULT_LENGTH = constant(10 * 60 * 1000L);

    SupplierRng<Long> pickupUrgency;
    SupplierRng<Long> deliveryUrgency;
    SupplierRng<Long> pickupTWLength;
    SupplierRng<Long> deliveryTWLength;

    Builder() {
      pickupUrgency = DEFAULT_URGENCY;
      deliveryUrgency = DEFAULT_URGENCY;
      pickupTWLength = DEFAULT_LENGTH;
      deliveryTWLength = DEFAULT_LENGTH;
    }

    public Builder pickupUrgency(SupplierRng<Long> urgency) {
      pickupUrgency = urgency;
      return this;
    }

    public Builder deliveryUrgency(SupplierRng<Long> urgency) {
      deliveryUrgency = urgency;
      return this;
    }

    public Builder urgency(SupplierRng<Long> urgency) {
      return pickupUrgency(urgency).deliveryUrgency(urgency);
    }

    public Builder pickupTimeWindowLength(SupplierRng<Long> length) {
      pickupTWLength = length;
      return this;
    }

    public Builder deliveryTimeWindowLength(SupplierRng<Long> length) {
      deliveryTWLength = length;
      return this;
    }

    public Builder timeWindowLength(SupplierRng<Long> length) {
      return pickupTimeWindowLength(length).deliveryTimeWindowLength(length);
    }

    public TimeWindowGenerator build() {
      return new DefaultTimeWindowGenerator(this);
    }
  }

  interface TravelModel {

    // should use RoadModel internally

    // using fastest truck
    long getShortestTravelTime(Point from, Point to);

    // using nearest depot
    long getTravelTimeToNearestDepot(Point from);
  }

  static class DefaultTravelModel implements TravelModel {

    private final RoadModel roadModel;

    DefaultTravelModel(RoadModel rm) {
      roadModel = rm;
    }

    @Override
    public long getShortestTravelTime(Point from, Point to) {
      final List<Point> path = roadModel.getShortestPathTo(from, to);

      return 0L;
    }

    @Override
    public long getTravelTimeToNearestDepot(Point from) {
      // TODO Auto-generated method stub
      return 0;
    }

  }

}
