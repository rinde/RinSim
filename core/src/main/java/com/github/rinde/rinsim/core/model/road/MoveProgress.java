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

import java.util.List;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;

import com.github.rinde.rinsim.geom.Point;

/**
 * Represents the distance traveled and time spend in
 * {@link RoadModel#followPath(MovingRoadUser, java.util.Queue, com.github.rinde.rinsim.core.TimeLapse)}
 * .
 * @author Bartosz Michalik 
 * @author Rinde van Lon 
 * @since 2.0
 */
public final class MoveProgress {
  /**
   * Distance traveled in the
   * {@link RoadModel#followPath(MovingRoadUser, java.util.Queue, com.github.rinde.rinsim.core.TimeLapse)}
   * .
   */
  public final Measure<Double, Length> distance;
  /**
   * Time spend on traveling the distance.
   */
  public final Measure<Long, Duration> time;

  /**
   * The nodes which were traveled.
   */
  public final List<Point> travelledNodes;

  MoveProgress(Measure<Double, Length> dist, Measure<Long, Duration> pTime,
      List<Point> pTravelledNodes) {
    checkArgument(dist.getValue() >= 0d, "distance must be greater than or equal to 0");
    checkArgument(pTime.getValue() >= 0L, "time must be greather than or equal to 0");
    distance = dist;
    time = pTime;
    travelledNodes = pTravelledNodes;
  }

  @Override
  public String toString() {
    return new StringBuilder().append("{PathProgress distance:")
        .append(distance).append(" time:").append(time)
        .append(" travelledNodes:").append(travelledNodes).append("}")
        .toString();
  }
}
