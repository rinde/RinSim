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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Queue;

import com.github.rinde.rinsim.core.model.road.RoadModelBuilders.AerialRMB;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;

public class AerialModel extends PlaneRoadModel {

  private final double deltaMax;
  private final double uavRadius;

  AerialModel(AerialRMB b, Clock c) {
    super(b);
    uavRadius = b.getUavRadius();
    deltaMax = c.getTickLength() * maxSpeed;
  }

  // how to distinguish between Uav's and Parcels?
  // two registries?
  // do we need a Uav class/interface?
  // MovingRoadUser vs RoadUser

  @Override
  public void addObjectAt(RoadUser obj, Point pos) {
    checkArgument(
      registry().findObjectsWithinRadius(pos, 2 * uavRadius).isEmpty(),
      "Cannot add an object on an occupied position.");
    super.addObjectAt(obj, pos);
  }

  @Override
  protected MoveProgress doFollowPath(MovingRoadUser object, Queue<Point> path,
      TimeLapse time) {

    return super.doFollowPath(object, path, time);
  }

}
