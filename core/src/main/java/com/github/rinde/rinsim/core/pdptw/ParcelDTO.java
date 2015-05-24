/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.core.pdptw;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;

/**
 * This is an immutable value object representing a parcel in a
 * pickup-and-delivery problem. It defines pickup location, destination
 * location, capacity and related time windows and service durations.
 * @author Rinde van Lon
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
  public final Point deliveryLocation;

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
  public final long orderAnnounceTime;

  /**
   * The time the pickup operation takes.
   */
  public final long pickupDuration;

  /**
   * The time the delivery operation takes.
   */
  public final long deliveryDuration;

  ParcelDTO(Builder b) {
    checkArgument(b.orderAnnounceTime <= b.pickupTimeWindow.begin,
      "Order arrival time may not be after the pickup TW has already opened.");
    pickupLocation = b.pickupLocation;
    deliveryLocation = b.deliveryLocation;
    pickupTimeWindow = b.pickupTimeWindow;
    deliveryTimeWindow = b.deliveryTimeWindow;
    neededCapacity = b.neededCapacity;
    orderAnnounceTime = b.orderAnnounceTime;
    pickupDuration = b.pickupDuration;
    deliveryDuration = b.deliveryDuration;
  }

  @Override
  public int hashCode() {
    return Objects.hash(pickupLocation, deliveryLocation, pickupTimeWindow,
      deliveryTimeWindow, neededCapacity, orderAnnounceTime, pickupDuration,
      deliveryDuration);
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object == null || object.getClass() != getClass()) {
      return false;
    }
    final ParcelDTO o = (ParcelDTO) object;
    return Objects.equals(pickupLocation, o.pickupLocation)
      && Objects.equals(deliveryLocation, o.deliveryLocation)
      && Objects.equals(pickupTimeWindow, o.pickupTimeWindow)
      && Objects.equals(deliveryTimeWindow, o.deliveryTimeWindow)
      && Objects.equals(neededCapacity, o.neededCapacity)
      && Objects.equals(orderAnnounceTime, o.orderAnnounceTime)
      && Objects.equals(pickupDuration, o.pickupDuration)
      && Objects.equals(deliveryDuration, o.deliveryDuration);
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
   * @author Rinde van Lon
   */
  public static final class Builder {
    final Point pickupLocation;
    final Point deliveryLocation;
    TimeWindow pickupTimeWindow;
    TimeWindow deliveryTimeWindow;
    int neededCapacity;
    long orderAnnounceTime;
    long pickupDuration;
    long deliveryDuration;

    Builder(Point from, Point to) {
      pickupLocation = from;
      deliveryLocation = to;
      pickupTimeWindow = TimeWindow.ALWAYS;
      deliveryTimeWindow = TimeWindow.ALWAYS;
      neededCapacity = 0;
      orderAnnounceTime = 0L;
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
     * Sets the order announce time.
     * @param time The announce time.
     * @return This, as per the builder pattern.
     */
    public Builder orderAnnounceTime(long time) {
      orderAnnounceTime = time;
      return this;
    }

    /**
     * Sets the duration of both the pickup and delivery process, must be
     * <code>&gt;= 0</code>.
     * @param duration The duration of the service process.
     * @return This, as per the builder pattern.
     */
    public Builder serviceDuration(long duration) {
      return pickupDuration(duration).deliveryDuration(duration);
    }

    /**
     * Sets the duration of the pickup, must be <code>&gt;=0</code>.
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
     * Sets the duration of the delivery, must be <code>&gt;=0</code>.
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
    public int getNeededCapacity() {
      return neededCapacity;
    }
  }
}
