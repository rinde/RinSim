/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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
package com.github.rinde.rinsim.core.model.road;

import static com.google.common.base.Preconditions.checkArgument;

import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.ListenableGraph;

/**
 * @author Rinde van Lon
 *
 */
public class CollisionGraphRoadModel extends DynamicGraphRoadModel {
  private final double vehicleLength;
  private final double minDistance;

  CollisionGraphRoadModel(Builder builder) {
    super(builder.graph, builder.distanceUnit, builder.speedUnit);
    vehicleLength = builder.vehicleLength;
    minDistance = builder.minDistance;
  }

  public static Builder builder(ListenableGraph<? extends ConnectionData> graph) {
    return new Builder(graph);
  }

  public static class Builder {
    public static final Unit<Length> DEFAULT_DISTANCE_UNIT = SI.METER;
    public static final Unit<Velocity> DEFAULT_SPEED_UNIT = NonSI.KILOMETERS_PER_HOUR;
    public static final double DEFAULT_VEHICLE_LENGTH = 1;
    public static final double DEFAULT_MIN_DISTANCE = 0.25;

    final ListenableGraph<? extends ConnectionData> graph;
    Unit<Length> distanceUnit;
    Unit<Velocity> speedUnit;
    double vehicleLength;
    double minDistance;

    Builder(ListenableGraph<? extends ConnectionData> g) {
      graph = g;
      distanceUnit = DEFAULT_DISTANCE_UNIT;
      speedUnit = DEFAULT_SPEED_UNIT;
      vehicleLength = DEFAULT_VEHICLE_LENGTH;
      minDistance = DEFAULT_MIN_DISTANCE;
    }

    public Builder setDistanceUnit(Unit<Length> unit) {
      distanceUnit = unit;
      return this;
    }

    public Builder setSpeedUnit(Unit<Velocity> unit) {
      speedUnit = unit;
      return this;
    }

    public Builder setVehicleLength(double length) {
      checkArgument(length > 0d);
      vehicleLength = length;
      return this;
    }

    public Builder setMinDistance(double dist) {
      checkArgument(dist > 0d);
      minDistance = dist;
      return this;
    }

    public CollisionGraphRoadModel build() {
      return new CollisionGraphRoadModel(this);
    }

  }
}
