/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.core.model.pdp;

import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;

/**
 * The parcel class represents goods that can be transported. Construct
 * instances via {@link #builder(Point, Point)}.
 * @author Rinde van Lon
 */
public class Parcel extends PDPObjectImpl implements IParcel {

  private final ParcelDTO dto;
  private final String string;

  /**
   * Create a new parcel.
   * @param parcelDto The {@link ParcelDTO} detailing all immutable information
   *          of a parcel.
   */
  public Parcel(ParcelDTO parcelDto) {
    this(parcelDto, null);
  }

  public Parcel(ParcelDTO parcelDto, @Nullable String toString) {
    dto = parcelDto;
    setStartPosition(dto.getPickupLocation());
    if (toString == null) {
      string = "[Parcel-" + Integer.toHexString(hashCode()) + "]";
    } else {
      string = toString;
    }
  }

  @Override
  public final PDPType getType() {
    return PDPType.PARCEL;
  }

  @Override
  public long getOrderAnnounceTime() {
    return dto.getOrderAnnounceTime();
  }

  @Override
  public final double getNeededCapacity() {
    return dto.getNeededCapacity();
  }

  @Override
  public final long getPickupDuration() {
    return dto.getPickupDuration();
  }

  @Override
  public final long getDeliveryDuration() {
    return dto.getDeliveryDuration();
  }

  @Override
  public final Point getPickupLocation() {
    return dto.getPickupLocation();
  }

  @Override
  public final Point getDeliveryLocation() {
    return dto.getDeliveryLocation();
  }

  @Override
  public final TimeWindow getDeliveryTimeWindow() {
    return dto.getDeliveryTimeWindow();
  }

  @Override
  public final TimeWindow getPickupTimeWindow() {
    return dto.getPickupTimeWindow();
  }

  /**
   * @return The immutable {@link ParcelDTO}.
   */
  public final ParcelDTO getDto() {
    return dto;
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

  @Override
  public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

  @Override
  public String toString() {
    return string;
  }

  /**
   * Starts building a {@link ParcelDTO} with the mandatory origin and
   * destination locations.
   * @param from The pickup location.
   * @param to The delivery location.
   * @return A {@link Parcel.Builder} instance.
   */
  public static Parcel.Builder builder(Point from, Point to) {
    return new Parcel.Builder(from, to);
  }

  public static Parcel.Builder builder(ParcelDTO dto) {
    return new Parcel.Builder(dto);
  }

  public static Parcel.Builder builder(Parcel parcel) {
    return builder(parcel.getDto());
  }

  /**
   * A builder for {@link ParcelDTO}. For fields which are not set the following
   * default values are used:
   * <ul>
   * <li><code>pickupTimeWindow = {@link TimeWindow#always()}</code></li>
   * <li><code>deliveryTimeWindow = {@link TimeWindow#always()}</code></li>
   * <li><code>neededCapacity = 0</code></li>
   * <li><code>orderArrivalTime = 0L</code></li>
   * <li><code>pickupDuration = 0L</code></li>
   * <li><code>deliveryDuration = 0L</code></li>
   * </ul>
   *
   * @author Rinde van Lon
   */
  public static final class Builder {
    final Point pickupLocation;
    final Point deliveryLocation;
    TimeWindow pickupTimeWindow;
    TimeWindow deliveryTimeWindow;
    double neededCapacity;
    long orderAnnounceTime;
    long pickupDuration;
    long deliveryDuration;
    @Nullable
    String toString;

    Builder(Point from, Point to) {
      pickupLocation = from;
      deliveryLocation = to;
      pickupTimeWindow = TimeWindow.always();
      deliveryTimeWindow = TimeWindow.always();
      neededCapacity = 0;
      orderAnnounceTime = 0L;
      pickupDuration = 0L;
      deliveryDuration = 0L;
      toString = null;
    }

    Builder(ParcelDTO dto) {
      pickupLocation = dto.getPickupLocation();
      deliveryLocation = dto.getDeliveryLocation();
      pickupTimeWindow = dto.getPickupTimeWindow();
      deliveryTimeWindow = dto.getDeliveryTimeWindow();
      neededCapacity = dto.getNeededCapacity();
      orderAnnounceTime = dto.getOrderAnnounceTime();
      pickupDuration = dto.getPickupDuration();
      deliveryDuration = dto.getDeliveryDuration();
      toString = null;
    }

    /**
     * @return A new parcel data transfer object ({@link ParcelDTO}).
     */
    public ParcelDTO buildDTO() {
      checkArgument(orderAnnounceTime <= pickupTimeWindow.begin(),
        "Order arrival time may not be after the pickup TW has already "
          + "opened.");
      return new AutoValue_ParcelDTO(pickupLocation, deliveryLocation,
        pickupTimeWindow, deliveryTimeWindow, neededCapacity,
        orderAnnounceTime, pickupDuration, deliveryDuration);
    }

    /**
     * @return A new parcel object.
     */
    public Parcel build() {
      return new Parcel(buildDTO(), toString);
    }

    /**
     * Sets both the pickup and delivery time windows to the specified value.
     * The default value is {@link TimeWindow#always()}.
     * @param tw The time window to set.
     * @return This, as per the builder pattern.
     */
    public Builder timeWindows(TimeWindow tw) {
      return pickupTimeWindow(tw).deliveryTimeWindow(tw);
    }

    /**
     * Sets the pickup time window. The default value is
     * {@link TimeWindow#always()}.
     * @param tw The new pickup time window.
     * @return This, as per the builder pattern.
     */
    public Builder pickupTimeWindow(TimeWindow tw) {
      pickupTimeWindow = tw;
      return this;
    }

    /**
     * Sets the delivery time window. The default value is
     * {@link TimeWindow#always()}.
     * @param tw The new delivery time window.
     * @return This, as per the builder pattern.
     */
    public Builder deliveryTimeWindow(TimeWindow tw) {
      deliveryTimeWindow = tw;
      return this;
    }

    /**
     * Sets the capacity that is needed for this parcel. Default value:
     * <code>0</code>.
     * @param capacity The capacity to set.
     * @return This, as per the builder pattern.
     */
    public Builder neededCapacity(double capacity) {
      checkArgument(capacity >= 0, "Capacity can not be negative.");
      neededCapacity = capacity;
      return this;
    }

    /**
     * Sets the order announce time. Default value: <code>0</code>.
     * @param time The announce time.
     * @return This, as per the builder pattern.
     */
    public Builder orderAnnounceTime(long time) {
      orderAnnounceTime = time;
      return this;
    }

    /**
     * Sets the duration of both the pickup and delivery process, must be
     * <code>&gt;= 0</code>. Default value: <code>0</code>.
     * @param duration The duration of the service process.
     * @return This, as per the builder pattern.
     */
    public Builder serviceDuration(long duration) {
      return pickupDuration(duration).deliveryDuration(duration);
    }

    /**
     * Sets the duration of the pickup, must be <code>&gt;=0</code>. Default
     * value: <code>0</code>.
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
     * Sets the duration of the delivery, must be <code>&gt;=0</code>. Default
     * value: <code>0</code>.
     * @param duration The duration of the delivery.
     * @return This, as per the builder pattern.
     */
    public Builder deliveryDuration(long duration) {
      checkArgument(duration >= 0,
        "Delivery duration needs to be strictly positive.");
      deliveryDuration = duration;
      return this;
    }

    /**
     * Overrides {@link Parcel#toString()} with the specified string.
     * @param string The string to use.
     * @return This, as per the builder pattern.
     */
    public Builder toString(String string) {
      toString = string;
      return this;
    }

    /**
     * @return The announce time of this parcel order.
     */
    public long getOrderAnnounceTime() {
      return orderAnnounceTime;
    }

    /**
     * @return The pickup location of the parcel.
     */
    public Point getPickupLocation() {
      return pickupLocation;
    }

    /**
     * @return The pickup time window of the parcel.
     */
    public TimeWindow getPickupTimeWindow() {
      return pickupTimeWindow;
    }

    /**
     * @return The duration of the pickup.
     */
    public long getPickupDuration() {
      return pickupDuration;
    }

    /**
     * @return The delivery location.
     */
    public Point getDeliveryLocation() {
      return deliveryLocation;
    }

    /**
     * @return The delivery time window.
     */
    public TimeWindow getDeliveryTimeWindow() {
      return deliveryTimeWindow;
    }

    /**
     * @return The duration of the delivery.
     */
    public long getDeliveryDuration() {
      return deliveryDuration;
    }

    /**
     * @return The needed capacity.
     */
    public double getNeededCapacity() {
      return neededCapacity;
    }
  }
}
