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
package com.github.rinde.rinsim.core.model.road;

import java.util.HashMap;
import java.util.Map;

import javax.measure.Measure;
import javax.measure.converter.UnitConverter;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

/**
 * Utility class for managing {@link Unit} conversions for {@link RoadModel}s.
 * @author Rinde van Lon
 */
public final class RoadUnits {

  /**
   * The unit that is used to represent time internally.
   */
  public static final Unit<Duration> INTERNAL_TIME_UNIT = SI.SECOND;

  /**
   * The unit that is used to represent distance internally.
   */
  public static final Unit<Length> INTERNAL_DIST_UNIT = SI.METER;

  /**
   * The unit that is used to represent speed internally.
   */
  public static final Unit<Velocity> INTERNAL_SPEED_UNIT = SI.METERS_PER_SECOND;

  /**
   * The unit that is used to represent distance externally.
   */
  private final Unit<Length> externalDistanceUnit;

  /**
   * The unit that is used to represent speed externally.
   */
  private final Unit<Velocity> externalSpeedUnit;

  /**
   * Converter that converts distances in {@link #INTERNAL_DIST_UNIT} to
   * distances in {@link #externalDistanceUnit}.
   */
  private final UnitConverter toExternalDistConv;

  /**
   * Converter that converts distances in {@link #externalDistanceUnit} to
   * {@link #INTERNAL_DIST_UNIT}.
   */
  private final UnitConverter toInternalDistConv;

  /**
   * Converter that converts speed in {@link #INTERNAL_SPEED_UNIT} to speed in
   * {@link #externalSpeedUnit}.
   */
  private final UnitConverter toExternalSpeedConv;

  /**
   * Converter that converts speed in {@link #externalSpeedUnit} to speed in
   * {@link #INTERNAL_SPEED_UNIT}.
   */
  private final UnitConverter toInternalSpeedConv;

  private final Map<Unit<Duration>, UnitConverter> toInternalTimeCache;
  private final Map<Unit<Duration>, UnitConverter> toExternalTimeCache;

  /**
   * Create a new instance using the specified external units.
   * @param distanceUnit The external distance unit.
   * @param speedUnit The external speed unit.
   */
  public RoadUnits(Unit<Length> distanceUnit,
      Unit<Velocity> speedUnit) {
    externalDistanceUnit = distanceUnit;
    externalSpeedUnit = speedUnit;
    toExternalDistConv = INTERNAL_DIST_UNIT
      .getConverterTo(externalDistanceUnit);
    toInternalDistConv = externalDistanceUnit
      .getConverterTo(INTERNAL_DIST_UNIT);
    toExternalSpeedConv = INTERNAL_SPEED_UNIT.getConverterTo(externalSpeedUnit);
    toInternalSpeedConv = externalSpeedUnit.getConverterTo(INTERNAL_SPEED_UNIT);

    toInternalTimeCache = new HashMap<>();
    toExternalTimeCache = new HashMap<>();
  }

  /**
   * Converts the specified distance into a {@link Measure} using the external
   * distance unit as specified by {@link #getExDistUnit()}.
   * @param distance The distance to convert.
   * @return A new {@link Measure} instance.
   */
  public Measure<Double, Length> toExDistMeasure(double distance) {
    return Measure.valueOf(toExternalDistConv.convert(distance),
      externalDistanceUnit);
  }

  /**
   * Converts the specified distance to {@link #getExDistUnit()}.
   * @param distance The distance to convert.
   * @return The converted distance.
   */
  public double toExDist(double distance) {
    return toExternalDistConv.convert(distance);
  }

  /**
   * Converts the specified distance to {@link #INTERNAL_DIST_UNIT}.
   * @param distance The distance to convert.
   * @return The converted distance.
   */
  public double toInDist(double distance) {
    return toInternalDistConv.convert(distance);
  }

  /**
   * Converts the specified time to {@link #INTERNAL_TIME_UNIT}.
   * @param time The time to convert.
   * @param unit The unit of the specified time.
   * @return The converted time.
   */
  public double toInTime(long time, final Unit<Duration> unit) {
    if (!toInternalTimeCache.containsKey(unit)) {
      toInternalTimeCache.put(unit, unit.getConverterTo(INTERNAL_TIME_UNIT));
    }
    return toInternalTimeCache.get(unit).convert(time);
  }

  /**
   * Converts the specified time to <code>unit</code>.
   * @param time The time to convert.
   * @param unit The unit to which the time should be converted.
   * @return The converted time.
   */
  public double toExTime(double time, Unit<Duration> unit) {
    if (!toExternalTimeCache.containsKey(unit)) {
      toExternalTimeCache.put(unit, INTERNAL_TIME_UNIT.getConverterTo(unit));
    }
    return toExternalTimeCache.get(unit).convert(time);
  }

  /**
   * Converts the specified speed to {@link #INTERNAL_SPEED_UNIT}.
   * @param speed The speed to convert.
   * @return The converted speed.
   */
  public double toInSpeed(double speed) {
    return toInternalSpeedConv.convert(speed);
  }

  /**
   * Converts the specified speed to {@link #INTERNAL_SPEED_UNIT}.
   * @param speed The speed to convert.
   * @return The converted speed.
   */
  @SuppressWarnings("static-method")
  public double toInSpeed(Measure<Double, Velocity> speed) {
    return speed.doubleValue(INTERNAL_SPEED_UNIT);
  }

  /**
   * Converts the specified speed to {@link #getExSpeedUnit()}.
   * @param speed The speed to convert.
   * @return The converted speed.
   */
  public double toExSpeed(double speed) {
    return toExternalSpeedConv.convert(speed);
  }

  /**
   * @return The external distance unit.
   */
  public Unit<Length> getExDistUnit() {
    return externalDistanceUnit;
  }

  /**
   * @return The external speed unit.
   */
  public Unit<Velocity> getExSpeedUnit() {
    return externalSpeedUnit;
  }
}
