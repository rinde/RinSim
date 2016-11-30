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

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.geom.Point;

abstract class AbstractTravelTimes implements TravelTimes {

  final Unit<Duration> timeUnit;
  final Unit<Length> distanceUnit;

  AbstractTravelTimes(Unit<Duration> tu,
      Unit<Length> du) {
    timeUnit = tu;
    distanceUnit = du;
  }

  AbstractTravelTimes(AbstractTravelTimes tt) {
    timeUnit = tt.timeUnit;
    distanceUnit = tt.distanceUnit;
  }

  @Override
  public abstract long getTheoreticalShortestTravelTime(Point from, Point to);

  @Override
  public abstract long getCurrentShortestTravelTime(Point from, Point to);

  @Override
  public abstract double computeTheoreticalDistance(Point from, Point to);

  @Override
  public abstract double computeCurrentDistance(Point from, Point to);

}
