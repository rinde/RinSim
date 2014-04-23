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
public final class ParcelDTO implements Serializable {
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
   * @deprecated Use {@link #builder(Point, Point)} instead.
   */
  @Deprecated
  public ParcelDTO(Point pPickupLocation, Point pDestinationLocation,
      TimeWindow pPickupTimeWindow, TimeWindow pDeliveryTimeWindow,
      int pNeededCapacity, long pOrderArrivalTime, long pPickupDuration,
      long pDeliveryDuration) {
    this(builder(pPickupLocation, pDestinationLocation)
        .pickupTimeWindow(pPickupTimeWindow)
        .deliveryTimeWindow(pDeliveryTimeWindow)
        .neededCapacity(pNeededCapacity)
        .arrivalTime(pOrderArrivalTime)
        .pickupDuration(pPickupDuration)
        .deliveryDuration(pDeliveryDuration));
  }

  ParcelDTO(Builder b) {
    checkArgument(b.orderArrivalTime <= b.pickupTimeWindow.begin,
        "Order arrival time may not be after the pickup TW has already opened.");
    pickupLocation = b.pickupLocation;
    destinationLocation = b.destinationLocation;
    pickupTimeWindow = b.pickupTimeWindow;
    deliveryTimeWindow = b.deliveryTimeWindow;
    neededCapacity = b.neededCapacity;
    orderArrivalTime = b.orderArrivalTime;
    pickupDuration = b.pickupDuration;
    deliveryDuration = b.deliveryDuration;
  }

  @Override
  public String toString() {
    return new StringBuilder("ParcelDTO-").append(
        Integer.toHexString(hashCode())).toString();
  }

  /**
   * Starts building a {@link ParcelDTO} with the mandatory origin and
   * destination locations.
   * @param from The pickup location.
   * @param to The delivery location.
   * @return A {@link Builder} instance.
   */
  public static Builder builder(Point from, Point to) {
    return new Builder(from, to);
  }

  /**
   * A builder for {@link ParcelDTO}. For fields which are not set the following
   * default values are used:
   * <ul>
   * <li><code>pickupTimeWindow = {@link TimeWindow#ALWAYS}</code></li>
   * <li><code>deliveryTimeWindow = {@link TimeWindow#ALWAYS}</code></li>
   * <li><code>neededCapacity = 0</code></li>
   * <li><code>orderArrivalTime = 0L</code></li>
   * <li><code>pickupDuration = 0L</code></li>
   * <li><code>deliveryDuration = 0L</code></li>
   * </ul>
   * 
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static final class Builder {
    final Point pickupLocation;
    final Point destinationLocation;
    TimeWindow pickupTimeWindow;
    TimeWindow deliveryTimeWindow;
    int neededCapacity;
    long orderArrivalTime;
    long pickupDuration;
    long deliveryDuration;

    Builder(Point from, Point to) {
      pickupLocation = from;
      destinationLocation = to;
      pickupTimeWindow = TimeWindow.ALWAYS;
      deliveryTimeWindow = TimeWindow.ALWAYS;
      neededCapacity = 0;
      orderArrivalTime = 0L;
      pickupDuration = 0L;
      deliveryDuration = 0L;
    }

    /**
     * @return A new {@link ParcelDTO}.
     */
    public ParcelDTO build() {
      return new ParcelDTO(this);
    }

    /**
     * Sets both the pickup and delivery time windows to the specified value.
     * The default value is {@link TimeWindow#ALWAYS}.
     * @param tw The time window to set.
     * @return This, as per the builder pattern.
     */
    public Builder timeWindows(TimeWindow tw) {
      return pickupTimeWindow(tw).deliveryTimeWindow(tw);
    }

    /**
     * Sets the pickup time window. The default value is
     * {@link TimeWindow#ALWAYS}.
     * @param tw The new pickup time window.
     * @return This, as per the builder pattern.
     */
    public Builder pickupTimeWindow(TimeWindow tw) {
      pickupTimeWindow = tw;
      return this;
    }

    /**
     * Sets the delivery time window. The default value is
     * {@link TimeWindow#ALWAYS}.
     * @param tw The new delivery time window.
     * @return This, as per the builder pattern.
     */
    public Builder deliveryTimeWindow(TimeWindow tw) {
      deliveryTimeWindow = tw;
      return this;
    }

    /**
     * Sets the capacity that is needed for this parcel.
     * @param capacity The capacity to set.
     * @return This, as per the builder pattern.
     */
    public Builder neededCapacity(int capacity) {
      checkArgument(capacity >= 0, "Capacity can not be negative.");
      neededCapacity = capacity;
      return this;
    }

    /**
     * Sets the arrival time of the order.
     * @param time The time of arrival.
     * @return This, as per the builder pattern.
     */
    public Builder arrivalTime(long time) {
      orderArrivalTime = time;
      return this;
    }

    /**
     * Sets the duration of both the pickup and delivery process, must be
     * <code>>= 0</code>.
     * @param duration The duration of the service process.
     * @return This, as per the builder pattern.
     */
    public Builder serviceDuration(long duration) {
      return pickupDuration(duration).deliveryDuration(duration);
    }

    /**
     * Sets the duration of the pickup, must be <code>>=0</code>.
     * @param duration The duration of the pickup.
     * @return This, as per the builder pattern.
     */
    public Builder pickupDuration(long duration) {
      checkArgument(duration >= 0,
          "Pickup duration needs to be strictly positive.");
      pickupDuration = duration;
      return this;
    }

    /**
     * Sets the duration of the delivery, must be <code>>=0</code>.
     * @param duration The duration of the delivery.
     * @return This, as per the builder pattern.
     */
    public Builder deliveryDuration(long duration) {
      checkArgument(duration >= 0,
          "Delivery duration needs to be strictly positive.");
      deliveryDuration = duration;
      return this;
    }

    public Point getPickupLocation() {
      return pickupLocation;
    }

    public Point getDestinationLocation() {
      return destinationLocation;
    }

    public TimeWindow getPickupTimeWindow() {
      return pickupTimeWindow;
    }

    public TimeWindow getDeliveryTimeWindow() {
      return deliveryTimeWindow;
    }

    public int getNeededCapacity() {
      return neededCapacity;
    }

    public long getOrderArrivalTime() {
      return orderArrivalTime;
    }

    public long getPickupDuration() {
      return pickupDuration;
    }

    public long getDeliveryDuration() {
      return deliveryDuration;
    }
  }
}
