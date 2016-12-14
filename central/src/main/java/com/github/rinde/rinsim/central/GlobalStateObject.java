/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.central;

import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.Nullable;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.TravelTimes;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Point;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * An immutable state object representing the state of an entire
 * {@link com.github.rinde.rinsim.core.Simulator}.
 * @author Rinde van Lon
 */
@AutoValue
public abstract class GlobalStateObject {
  // TODO add generic way for storing travel distances based on shortest path
  // in road model

  GlobalStateObject() {}

  /**
   * @return All known parcels which require both a pickup and a delivery. They
   *         are not in the inventory of a vehicle.
   */
  public abstract ImmutableSet<Parcel> getAvailableParcels();

  /**
   * @return All vehicles.
   */
  public abstract ImmutableList<VehicleStateObject> getVehicles();

  /**
   * @return The current time.
   */
  public abstract long getTime();

  /**
   * @return The unit of time.
   */
  public abstract Unit<Duration> getTimeUnit();

  /**
   * @return The unit of (vehicle) speed.
   */
  public abstract Unit<Velocity> getSpeedUnit();

  /**
   * @return The unit of distances.
   */
  public abstract Unit<Length> getDistUnit();

  /**
   * @return An interface to calculate shortest paths
   */
  public abstract TravelTimes getTravelTimes();

  static GlobalStateObject create(ImmutableSet<Parcel> availableParcels,
      ImmutableList<VehicleStateObject> vehicles, long time,
      Unit<Duration> timeUnit, Unit<Velocity> speedUnit,
      Unit<Length> distUnit, TravelTimes tt) {
    return new AutoValue_GlobalStateObject(
      availableParcels, vehicles, time, timeUnit, speedUnit, distUnit, tt);
  }

  /**
   * Constructs a new {@link GlobalStateObject} with only the selected vehicle.
   * The current instance remains unchanged.
   * @param index The index of the vehicle to select.
   * @return A new object containing only the selected vehicle, all other values
   *         are copied from this instance.
   */
  public GlobalStateObject withSingleVehicle(int index) {
    checkArgument(index >= 0 && index < getVehicles().size(),
      "Invalid vehicle index (%s) must be >= 0 and < %s.", index,
      getVehicles().size());
    return create(getAvailableParcels(),
      ImmutableList.of(getVehicles().get(index)),
      getTime(), getTimeUnit(), getSpeedUnit(), getDistUnit(),
      getTravelTimes());
  }

  /**
   * Constructs a new {@link GlobalStateObject} using the routes specified.
   * @param routes The routes to use, this will replace any existing routes in
   *          the vehicles. Exactly one route must be specified for each
   *          vehicle.
   * @return A newly constructed {@link GlobalStateObject} that only differs
   *         from the current object in the vehicles' routes.
   */
  public GlobalStateObject withRoutes(
      ImmutableList<ImmutableList<Parcel>> routes) {
    checkArgument(routes.size() == getVehicles().size());
    final ImmutableList.Builder<VehicleStateObject> b = ImmutableList.builder();
    for (int i = 0; i < getVehicles().size(); i++) {
      b.add(getVehicles().get(i).withRoute(routes.get(i)));
    }
    return create(getAvailableParcels(), b.build(), getTime(), getTimeUnit(),
      getSpeedUnit(), getDistUnit(), getTravelTimes());
  }

  /**
   * Immutable state object of a vehicle.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class VehicleStateObject {

    VehicleStateObject() {}

    /**
     * @return The {@link VehicleDTO}.
     */
    public abstract VehicleDTO getDto();

    /**
     * @return Location of the vehicle.
     */
    public abstract Point getLocation();

    /**
     * @return The connection the vehicle is on, if applicable.
     */
    public abstract Optional<? extends Connection<?>> getConnection();

    /**
     * @return The contents of the vehicle. This excludes parcels which are
     *         currently being picked up and includes parcels which are
     *         currently being delivered.
     */
    public abstract ImmutableSet<Parcel> getContents();

    /**
     * @return The remaining time the vehicle needs for completion of its
     *         current servicing operation.
     */
    public abstract long getRemainingServiceTime();

    /**
     * Destination is present in two situations:
     * <ol>
     * <li>In case all of the following holds:
     * <ul>
     * <li>Vehicles are not allowed to divert from their previously started
     * routes.</li>
     * <li>The vehicle is moving to a parcel (either pickup or delivery
     * location).</li>
     * <li>The vehicle has not yet started servicing.</li>
     * </ul>
     * In this case it indicates the current destination of the vehicle. When a
     * vehicle has a destination it <b>must</b> first move to and service this
     * destination.</li>
     * <li>In case the vehicle is servicing a parcel. In this case the
     * {@link Parcel} as specified by this field is the one being serviced. In
     * this case servicing <b>must</b> first complete before the vehicle can do
     * something else. When this {@link Parcel} also occurs in
     * {@link #getContents()} this parcel is currently being delivered,
     * otherwise it is being picked up.</li>
     * </ol>
     * @return The destination.
     */
    public abstract Optional<Parcel> getDestination();

    /**
     * @return If present the route the vehicle is currently following is
     *         returned. If no route is being followed {@link Optional#absent()}
     *         is returned.
     */
    public abstract Optional<ImmutableList<Parcel>> getRoute();

    @SuppressWarnings("unchecked")
    static VehicleStateObject create(VehicleDTO dto, Point location,
        Optional<? extends Connection<?>> conn,
        ImmutableSet<Parcel> contents, long remainingServiceTime,
        @Nullable Parcel destination,
        @Nullable ImmutableList<? extends Parcel> route) {
      return new AutoValue_GlobalStateObject_VehicleStateObject(
        dto,
        location,
        conn,
        contents,
        remainingServiceTime,
        Optional.fromNullable(destination),
        Optional.fromNullable((ImmutableList<Parcel>) route));
    }

    VehicleStateObject withRoute(ImmutableList<Parcel> route) {
      return create(getDto(), getLocation(), getConnection(), getContents(),
        getRemainingServiceTime(), getDestination().orNull(), route);
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return this == other;
    }
  }
}
