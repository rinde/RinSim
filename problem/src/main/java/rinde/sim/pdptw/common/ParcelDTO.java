package rinde.sim.pdptw.common;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;

import rinde.sim.core.graph.Point;
import rinde.sim.util.TimeWindow;

public class ParcelDTO implements Serializable {
  private static final long serialVersionUID = -6128057042614968652L;
  public final Point pickupLocation;
  public final Point destinationLocation;
  public final TimeWindow pickupTimeWindow;
  public final TimeWindow deliveryTimeWindow;
  public final int neededCapacity;
  public final long orderArrivalTime;
  public final long pickupDuration;
  public final long deliveryDuration;

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
    // return ReflectionToStringBuilder.toString(this);
  }
}
