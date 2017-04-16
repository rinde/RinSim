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

  private static final double DMAX_RAD_RATIO = .5;
  private final double deltaMax;
  private final double objRadius;

  private final SpatialRegistry<MovingRoadUser> blockingRegistry;

  CollisionPlaneRoadModel(CollisionPlaneRMB b, Clock c) {
    super(b);
    objRadius = b.getObjectRadius();
    deltaMax =
      maxSpeed * unitConversion.toInTime(c.getTickLength(), c.getTimeUnit());
    checkArgument(deltaMax <= DMAX_RAD_RATIO * objRadius,
      "Incompatible parameters. The following condition should hold: deltaMax "
        + "(%s) <= %s * objRadius (%s), where deltaMax = maxSpeed (%s %s) * "
        + "tickLength (%s %s).",
      deltaMax, DMAX_RAD_RATIO, objRadius, maxSpeed, getSpeedUnit(),
      c.getTickLength(), c.getTimeUnit());

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

    final double travelableDistance =
      super.computeTravelableDistance(from, to, speed, tLeft, tUnit);

    final Point diff = Point.diff(to, from);
    final double perc =
      travelableDistance / unitConversion.toInDist(Point.distance(from, to));
    final Point destDuringTick =
      new Point(from.x + perc * diff.x, from.y + perc * diff.y);

    final ImmutableSet<MovingRoadUser> set =
      blockingRegistry.findObjectsWithinRadius(destDuringTick, 4 * objRadius);

    System.out.println("closeby: " + set.size());
    // find intersection of line [from <-> to] with any MovingRoadUser in the
    // set.
    final Set<Point> centerIntersectionPoints = new LinkedHashSet<>();

    // final double angle = angle(from, destDuringTick);
    // final Set<Point> leftIntersectionPoints = new LinkedHashSet<>();
    // final Point leftFrom = pointInDir(from, angle - .5 * Math.PI, objRadius);
    // final Point leftDest =
    // pointInDir(destDuringTick, angle - .5 * Math.PI, objRadius);
    //
    // final Set<Point> rightIntersectionPoints = new LinkedHashSet<>();
    // final Point rightFrom = pointInDir(from, angle + .5 * Math.PI,
    // objRadius);
    // final Point rightDest =
    // pointInDir(destDuringTick, angle + .5 * Math.PI, objRadius);
    for (final MovingRoadUser ru : set) {
      final Point pos = getPosition(ru);
      if (Point.distance(pos, from) <= objRadius * 2) {
        return 0d;
      }
      centerIntersectionPoints
        .addAll(
          findIntersectionPoints(pos, 2 * objRadius, from, destDuringTick));
      // leftIntersectionPoints
      // .addAll(findIntersectionPoints(pos, objRadius, leftFrom, leftDest));
      // rightIntersectionPoints
      // .addAll(findIntersectionPoints(pos, objRadius, rightFrom, rightDest));
    }

    if (centerIntersectionPoints.isEmpty()) {
      // && leftIntersectionPoints.isEmpty()
      // && rightIntersectionPoints.isEmpty())

      return travelableDistance;
    }

    // find closest intersection
    final double minDist = findMinDist(from, centerIntersectionPoints);
    return Math.min(travelableDistance, Math.max(0, minDist));
  }

  static double findMinDist(Point pos, Iterable<Point> points) {
    double minDist = Double.POSITIVE_INFINITY;
    for (final Point p : points) {
      final double dist = Point.distance(pos, p);
      if (dist < minDist) {
        minDist = dist;
      }
    }
    return minDist;
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

  static Point pointInDir(Point value, double angle, double distance) {
    final double x = Math.cos(angle) * distance;
    final double y = Math.sin(angle) * distance;
    return new Point(value.x + x, value.y + y);
  }

  static double angle(Point p1, Point p2) {
    final double dx = p2.x - p1.x;
    final double dy = p2.y - p1.y;
    return Math.PI + Math.atan2(-dy, -dx);
  }

}
