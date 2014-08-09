/**
 * 
 */
package com.github.rinde.rinsim.core.model.pdp;

import com.github.rinde.rinsim.core.graph.Point;
import com.github.rinde.rinsim.util.TimeWindow;

/**
 * The parcel class represents goods that can be transported.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class Parcel extends PDPObjectImpl {

  /**
   * The time it takes to pickup this parcel.
   */
  protected final long pickupDuration;

  /**
   * The time window in which this parcel is preferrably picked up.
   */
  protected final TimeWindow pickupTimeWindow;

  /**
   * The time it takes to deliver this parcel.
   */
  protected final long deliveryDuration;

  /**
   * The time window in which this parcel is preferably delivered.
   */
  protected final TimeWindow deliveryTimeWindow;

  /**
   * The destination of this parcel, this is the position to where this parcel
   * needs to be delivered.
   */
  protected final Point destination;

  /**
   * The magnitude of this parcel, can be weight/volume/count depending on the
   * specific application.
   */
  protected final double magnitude;

  /**
   * Create a new parcel.
   * @param pDestination The position where this parcel needs to be delivered.
   * @param pPickupDuration The time needed for pickup.
   * @param pickupTW The time window for pickup.
   * @param pDeliveryDuration The time needed for delivery.
   * @param deliveryTW The time window for delivery.
   * @param pMagnitude The weight/volume/count of this parcel.
   */
  public Parcel(Point pDestination, long pPickupDuration, TimeWindow pickupTW,
      long pDeliveryDuration, TimeWindow deliveryTW, double pMagnitude) {
    destination = pDestination;
    pickupDuration = pPickupDuration;
    pickupTimeWindow = pickupTW;
    deliveryDuration = pDeliveryDuration;
    deliveryTimeWindow = deliveryTW;
    magnitude = pMagnitude;
  }

  @Override
  public final PDPType getType() {
    return PDPType.PARCEL;
  }

  /**
   * @return {@link #magnitude}
   */
  public final double getMagnitude() {
    return magnitude;
  }

  /**
   * @return {@link #pickupDuration}
   */
  public final long getPickupDuration() {
    return pickupDuration;
  }

  /**
   * @return {@link #deliveryDuration}
   */
  public final long getDeliveryDuration() {
    return deliveryDuration;
  }

  /**
   * @return {@link #destination}
   */
  public final Point getDestination() {
    return destination;
  }

  /**
   * @return {@link #deliveryTimeWindow}.
   */
  public final TimeWindow getDeliveryTimeWindow() {
    return deliveryTimeWindow;
  }

  /**
   * @return {@link #pickupTimeWindow}
   */
  public final TimeWindow getPickupTimeWindow() {
    return pickupTimeWindow;
  }

  /**
   * This method can optionally be overridden to add implementation specific
   * constraints on pickup operations.
   * @param v The vehicle that wants to pickup this parcel.
   * @param time The time at which the pickup takes place.
   * @return <code>true</code> if the operation is allowed, <code>false</code>
   *         otherwise.
   */
  public boolean canBePickedUp(Vehicle v, long time) {
    return true;
  }

  /**
   * This method can optionally be overridden to add implementation specific
   * constraints on delivery operations.
   * @param v The vehicle that wants to deliver this parcel.
   * @param time The time at which the delivery takes place.
   * @return <code>true</code> if the operation is allowed, <code>false</code>
   *         otherwise.
   */
  public boolean canBeDelivered(Vehicle v, long time) {
    return true;
  }
}
