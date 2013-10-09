package rinde.sim.pdptw.common;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;

import rinde.sim.core.graph.Point;
import rinde.sim.util.TimeWindow;

/**
 * This is an immutable value object representing a parcel in a
 * pickup-and-delivery problem. It defines pickup location, destination
 * location, capacity and related time windows and service durations.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class ParcelDTO implements Serializable {
  private static final long serialVersionUID = -6128057042614968652L;

  /**
   * The location at which this parcel can be picked up.
   */
  public final Point pickupLocation;

  /**
   * The location at which this parcel has to be delivered.
   */
  public final Point destinationLocation;

  /**
   * The {@link TimeWindow} in which this parcel has to be picked up.
   */
  public final TimeWindow pickupTimeWindow;

  /**
   * The {@link TimeWindow} in which this parcel has to be delivered.
   */
  public final TimeWindow deliveryTimeWindow;

  /**
   * The capacity that is needed in a vehicle to carry this parcel.
   */
  public final int neededCapacity;

  /**
   * The time at which this parcel is announced, i.e. the time at which the
   * customer calls with this request.
   */
  public final long orderArrivalTime;

  /**
   * The time the pickup operation takes.
   */
  public final long pickupDuration;

  /**
   * The time the delivery operation takes.
   */
  public final long deliveryDuration;

  /**
   * Create a new parcel value object.
   * @param pPickupLocation {@link #pickupLocation}.
   * @param pDestinationLocation {@link #destinationLocation}.
   * @param pPickupTimeWindow {@link #pickupTimeWindow}.
   * @param pDeliveryTimeWindow {@link #deliveryTimeWindow}.
   * @param pNeededCapacity {@link #neededCapacity}.
   * @param pOrderArrivalTime {@link #orderArrivalTime}.
   * @param pPickupDuration {@link #pickupDuration}.
   * @param pDeliveryDuration {@link #deliveryDuration}.
   */
  public ParcelDTO(Point pPickupLocation, Point pDestinationLocation,
      TimeWindow pPickupTimeWindow, TimeWindow pDeliveryTimeWindow,
      int pNeededCapacity, long pOrderArrivalTime, long pPickupDuration,
      long pDeliveryDuration) {
    checkArgument(pOrderArrivalTime <= pPickupTimeWindow.begin,
        "Order arrival time may not be after the pickup TW has already opened.");
    checkArgument(pNeededCapacity >= 0, "Capacity can not be negative.");
    checkArgument(pPickupDuration >= 0,
        "Pickup duration needs to be strictly positive.");
    checkArgument(pDeliveryDuration >= 0,
        "Delivery duration needs to be strictly positive.");

    pickupLocation = pPickupLocation;
    destinationLocation = pDestinationLocation;
    pickupTimeWindow = pPickupTimeWindow;
    deliveryTimeWindow = pDeliveryTimeWindow;
    neededCapacity = pNeededCapacity;
    orderArrivalTime = pOrderArrivalTime;
    pickupDuration = pPickupDuration;
    deliveryDuration = pDeliveryDuration;
  }

  @Override
  public String toString() {
    return new StringBuilder("ParcelDTO-").append(
        Integer.toHexString(hashCode())).toString();
  }
}
