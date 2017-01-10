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
package com.github.rinde.rinsim.core.model.road;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;

public class TravelTimesTestUtil {

  public static PlaneTravelTimes createPlaneTravelTimes(RoadModel rm,
      Unit<Duration> timeUnit) {
    return new PlaneTravelTimes(rm.getBounds().get(0), rm.getBounds().get(1),
      timeUnit, rm.getDistanceUnit());
  }

  public static TravelTimes createDefaultPlaneTravelTimes(Point min, Point max,
      Unit<Duration> timeUnit, Unit<Length> distanceUnit) {
    return new PlaneTravelTimes(min, max, timeUnit, distanceUnit);
  }

  public static GraphTravelTimes createGraphTravelTimes(Graph<?> graph,
      Unit<Duration> modelTimeUnit,
      Unit<Length> modelDistanceUnit) {
    return new GraphTravelTimes<>(graph, modelTimeUnit, modelDistanceUnit);
  }

}
