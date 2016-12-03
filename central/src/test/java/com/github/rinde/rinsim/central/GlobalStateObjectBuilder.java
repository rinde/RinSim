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

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.central.GlobalStateObject.VehicleStateObject;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.TravelTimes;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 *
 * @author Rinde van Lon
 */
public class GlobalStateObjectBuilder {

  Set<Parcel> availableParcels;
  List<VehicleStateObject> vehicles;
  long time;
  Unit<Duration> timeUnit;
  Unit<Velocity> speedUnit;
  Unit<Length> distUnit;

  TravelTimes mockedTT = mock(TravelTimes.class);

  GlobalStateObjectBuilder() {
    availableParcels = new LinkedHashSet<>();
    vehicles = new ArrayList<>();
    time = 0L;
    timeUnit = SI.MILLI(SI.SECOND);
    speedUnit = NonSI.KILOMETERS_PER_HOUR;
    distUnit = SI.KILOMETER;
  }

  public GlobalStateObjectBuilder addAvailableParcel(Parcel p) {
    availableParcels.add(p);
    return this;
  }

  public GlobalStateObjectBuilder addAvailableParcels(
      Iterable<? extends Parcel> ps) {
    Iterables.addAll(availableParcels, ps);
    return this;
  }

  public GlobalStateObjectBuilder addAvailableParcels(Parcel... ps) {
    availableParcels.addAll(asList(ps));
    return this;
  }

  public GlobalStateObjectBuilder addVehicle(VehicleStateObject vso) {
    vehicles.add(vso);
    return this;
  }

  public GlobalStateObjectBuilder setTime(long t) {
    time = t;
    return this;
  }

  public GlobalStateObjectBuilder setTimeUnit(Unit<Duration> tu) {
    timeUnit = tu;
    return this;
  }

  public GlobalStateObjectBuilder setSpeedUnit(Unit<Velocity> su) {
    speedUnit = su;
    return this;
  }

  public GlobalStateObjectBuilder setDistanceUnit(Unit<Length> du) {
    distUnit = du;
    return this;
  }

  public GlobalStateObject build() {
    return SolverValidator.validateInputs(buildUnsafe());
  }

  public GlobalStateObject buildUnsafe() {
    return GlobalStateObject.create(
      ImmutableSet.copyOf(availableParcels),
      ImmutableList.copyOf(vehicles),
      time,
      timeUnit,
      speedUnit,
      distUnit,
      mockedTT);
  }

  public static GlobalStateObjectBuilder globalBuilder() {
    return new GlobalStateObjectBuilder();
  }

  public static VSOBuilder vehicleBuilder() {
    return new VSOBuilder();
  }

  public static class VSOBuilder {
    static final Point DEFAULT_LOCATION = new Point(0, 0);
    VehicleDTO dto;
    Point location;
    Set<Parcel> contents;
    long remainingServiceTime;

    @Nullable
    Parcel destination;
    @Nullable
    ImmutableList<Parcel> route;

    VSOBuilder() {
      dto = VehicleDTO.builder().build();
      location = DEFAULT_LOCATION;
      contents = new LinkedHashSet<>();
      remainingServiceTime = 0L;
      destination = null;
      route = null;
    }

    public VSOBuilder setVehicleDTO(VehicleDTO dt) {
      dto = dt;
      return this;
    }

    public VSOBuilder setLocation(Point loc) {
      location = loc;
      return this;
    }

    public VSOBuilder addToContents(Parcel p) {
      contents.add(p);
      return this;
    }

    public VSOBuilder setRemainingServiceTime(long time) {
      remainingServiceTime = time;
      return this;
    }

    public VSOBuilder setDestination(@Nullable Parcel p) {
      destination = p;
      return this;
    }

    public VSOBuilder setRoute(@Nullable ImmutableList<Parcel> r) {
      route = r;
      return this;
    }

    public VehicleStateObject build() {
      final Optional<Connection<?>> absent = Optional.absent();
      return VehicleStateObject.create(
        dto,
        location,
        absent,
        ImmutableSet.copyOf(contents),
        remainingServiceTime,
        destination,
        route);
    }
  }
}
