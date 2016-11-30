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
package com.github.rinde.rinsim.core.model.pdp;

import static com.google.common.base.Preconditions.checkArgument;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.geom.Point;

/**
 * The {@link TravelTimes} class to be used with the {@link PlaneRoadModel}.
 * @author Vincent Van Gestel
 */
public class PlaneTravelTimes extends AbstractTravelTimes {
  private final Point min;
  private final Point max;

  /**
   * Constructs a new {@link PlaneTravelTimes} object.}
   * @param minimumPoint The minimum point of the plane.
   * @param maximumPoint The maximum point of the plane.
   * @param tu The time unit.
   * @param du The distance unit.
   */
  public PlaneTravelTimes(Point minimumPoint, Point maximumPoint,
      Unit<Duration> tu,
      Unit<Length> du) {
    super(tu, du);
    min = minimumPoint;
    max = maximumPoint;
  }

  /**
   * Construct a new {@link PlaneTravelTimes} object based on an older one.
   * @param tt The base {@link PlaneTravelTimes} object.
   */
  public PlaneTravelTimes(PlaneTravelTimes tt) {
    super(tt);
    min = tt.min;
    max = tt.max;
  }

  @Override
  public long getTheoreticalShortestTravelTime(Point from, Point to,
      Measure<Double, Velocity> vehicleSpeed) {
    checkArgument(
      isPointInBoundary(from),
      "from must be within the predefined boundary of the plane, from is %s, "
        + "boundary: min %s, max %s.",
      to, min, max);
    checkArgument(
      isPointInBoundary(to),
      "to must be within the predefined boundary of the plane, to is %s,"
        + " boundary: min %s, max %s.",
      to, min, max);
    return (long) RoadModels.computeTravelTime(vehicleSpeed,
      Measure.valueOf(Point.distance(from, to), distanceUnit),
      timeUnit);
  }

  @Override
  public long getCurrentShortestTravelTime(Point from, Point to,
      Measure<Double, Velocity> vehicleSpeed) {
    return getTheoreticalShortestTravelTime(from, to, vehicleSpeed);
  }

  /**
   * Checks whether the specified point is within the plane as defined by this
   * model.
   * @param p The point to check.
   * @return <code>true</code> if the points is within the boundary,
   *         <code>false</code> otherwise.
   */
  protected boolean isPointInBoundary(Point p) {
    return p.x >= min.x && p.x <= max.x && p.y >= min.y && p.y <= max.y;
  }

  @Override
  public double computeTheoreticalDistance(Point from, Point to,
      Measure<Double, Velocity> vehicleSpeed) {
    return Point.distance(from, to);
  }

  @Override
  public double computeCurrentDistance(Point from, Point to,
      Measure<Double, Velocity> vehicleSpeed) {
    return Point.distance(from, to);
  }
}
