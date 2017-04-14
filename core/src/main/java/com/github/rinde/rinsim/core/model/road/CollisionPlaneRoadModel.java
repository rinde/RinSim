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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;

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

  public double getObjectRadius() {
    return objRadius;
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
    blockingRegistry.removeObject(object);

    final MoveProgress mp = super.doFollowPath(object, path, time);

    blockingRegistry.addAt(object, getPosition(object));
    return mp;
  }

  @Override
  protected double computeTravelableDistance(Point from, Point to, double speed,
      long tLeft, Unit<Duration> tUnit) {
    final ImmutableSet<MovingRoadUser> set =
      blockingRegistry.findObjectsWithinRadius(to, 2 * objRadius);

    // find intersection of line from <-> to with any MovingRoadUser in the set.
    final Set<Point> intersectionPoints = new LinkedHashSet<>();
    for (final MovingRoadUser ru : set) {
      intersectionPoints
        .addAll(findIntersectionPoints(getPosition(ru), objRadius, from, to));
    }

    if (intersectionPoints.isEmpty()) {
      return super.computeTravelableDistance(from, to, speed, tLeft, tUnit);
    }

    // find closest intersection
    final Iterator<Point> it = intersectionPoints.iterator();
    double closestDist = Point.distance(from, it.next());
    while (it.hasNext()) {
      final double dist = Point.distance(from, it.next());
      if (dist < closestDist) {
        closestDist = dist;
      }
    }

    return Math.max(0, closestDist - objRadius);
  }

  // http://mathworld.wolfram.com/Circle-LineIntersection.html
  static ImmutableSet<Point> findIntersectionPoints(Point circleCenter,
      double circleRadius, Point lineFrom, Point lineTo) {

    // translate points such that circle center is at origin (0,0)
    final Point p1 =
      new Point(lineFrom.x - circleCenter.x, lineFrom.y - circleCenter.y);
    final Point p2 =
      new Point(lineTo.x - circleCenter.x, lineTo.y - circleCenter.y);

    final double dx = p2.x - p1.x;
    final double dy = p2.y - p1.y;
    final double drSquared = dx * dx + dy * dy;
    final double d = p1.x * p2.y - p2.x * p1.y;

    final double delta = circleRadius * circleRadius * drSquared - d * d;

    if (delta < 0) {
      return ImmutableSet.of();
    }

    final double xPart1 = d * dy;
    final double xPart2 = sign(dy) * dx * Math.sqrt(delta);
    final double yPart1 = -d * dx;
    final double yPart2 = Math.abs(dy) * Math.sqrt(delta);

    final double x1 = (xPart1 - xPart2) / drSquared + circleCenter.x;
    final double y1 = (yPart1 - yPart2) / drSquared + circleCenter.y;
    if (delta > 0) {
      // two intersection points
      final double x2 = (xPart1 + xPart2) / drSquared + circleCenter.x;
      final double y2 = (yPart1 + yPart2) / drSquared + circleCenter.y;
      return ImmutableSet.of(new Point(x1, y1), new Point(x2, y2));
    }
    // else: tangent, one intersection point
    return ImmutableSet.of(new Point(x1, y1));
  }

  static double sign(double x) {
    return x < 0 ? -1 : 1;
  }

}
