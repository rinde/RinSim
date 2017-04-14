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

import javax.measure.quantity.Duration;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.model.road.RoadModelBuilders.CollisionPlaneRMB;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.ImmutableSet;

public class CollisionPlaneRoadModel extends PlaneRoadModel {

  private final double deltaMax;
  private final double objRadius;

  private final SpatialRegistry<MovingRoadUser> blockingRegistry;

  CollisionPlaneRoadModel(CollisionPlaneRMB b, Clock c) {
    super(b);
    objRadius = b.getObjectRadius();
    deltaMax = c.getTickLength() * maxSpeed;

    blockingRegistry = MapSpatialRegistry.create();
  }

  @Override
  public void addObjectAt(RoadUser obj, Point pos) {
    checkArgument(
      blockingRegistry.findObjectsWithinRadius(pos, 2 * objRadius).isEmpty(),
      "Cannot add an object on an occupied position: %s.", pos);

    if (obj instanceof MovingRoadUser) {
      blockingRegistry.addAt((MovingRoadUser) obj, pos);
    }
    super.addObjectAt(obj, pos);
  }

  @Override
  protected MoveProgress doFollowPath(MovingRoadUser object, Queue<Point> path,
      TimeLapse time) {

    return super.doFollowPath(object, path, time);
  }

  @Override
  protected double computeTravelableDistance(Point from, Point to, double speed,
      long timeLeft, Unit<Duration> timeUnit) {
    final ImmutableSet<MovingRoadUser> set =
      blockingRegistry.findObjectsWithinRadius(to, 2 * objRadius);
    if (set.isEmpty()) {
      return super.computeTravelableDistance(from, to, speed, timeLeft,
        timeUnit);
    }
    return 0d;

  }

}
