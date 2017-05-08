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

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.road.RoadModelBuilders.CollisionPlaneRMB;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.ImmutableSet;

/**
 * Adds collision detection to the {@link PlaneRoadModel}. All
 * {@link MovingRoadUser}s are considered to be circular objects with the same
 * radius (e.g. an unmanned aerial vehicle, UAV). {@link RoadUser}s that do not
 * implement the {@link MovingRoadUser} interface are considered to be
 * non-blocking objects (e.g. an object on the ground such as a parcel).
 * <p>
 * This model handles collisions between {@link MovingRoadUser}s by stopping the
 * movements of colliding objects.
 *
 * @author Rinde van Lon
 */
public class CollisionPlaneRoadModel extends PlaneRoadModel {

  private static final double DMAX_RAD_RATIO = .5;
  private final double deltaMax;
  private final double objRadius;

  private final SpatialRegistry<MovingRoadUser> blockingRegistry;

  /**
   * Constructs a new instance.
   * @param b The builder with configuration options.
   * @param c A reference to the clock.
   */
  protected CollisionPlaneRoadModel(CollisionPlaneRMB b, Clock c) {
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

  /**
   * @return The radius of all {@link MovingRoadUser} in the model.
   */
  public double getObjectRadius() {
    return objRadius;
  }

  /**
   * Checks whether the specified {@link Point} is occupied or not. A
   * {@link Point} is considered occupied if a {@link MovingRoadUser} cannot be
   * added at that location without colliding with an existing
   * {@link MovingRoadUser}.
   * @param pos The position to check.
   * @return <code>true</code> indicates that the position is occupied,
   *         <code>false</code> indicates that the position is not occupied.
   */
  public boolean isOccupied(Point pos) {
    return !blockingRegistry.findObjectsWithinRadius(pos, 2 * objRadius)
      .isEmpty();
  }

  /**
   * Finds a random unoccupied position.
   * <p>
   * <b>Warning</b>, if there are no unoccupied positions, this method will not
   * return.
   * @param rng The random number generator to use for drawing random numbers.
   * @return A random unoccupied position.
   */
  public Point getRandomUnoccupiedPosition(RandomGenerator rng) {
    Point pos;
    do {
      pos = getRandomPosition(rng);
    } while (isOccupied(pos));
    return pos;
  }

  @Override
  public void addObjectAt(RoadUser obj, Point pos) {
    if (obj instanceof MovingRoadUser) {
      checkArgument(!isOccupied(pos),
        "Cannot add an object on an occupied position: %s.", pos);
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

    // find intersection of line [from <-> to] with any MovingRoadUser in the
    // set.
    final Set<Point> intersectPoints = new LinkedHashSet<>();
    // indicates whether we are currently colliding
    boolean hit = false;
    for (final MovingRoadUser ru : set) {
      final Point pos = getPosition(ru);
      if (Point.distance(pos, from) <= objRadius * 2) {
        hit = true;
      }

      final Set<Point> pts =
        findIntersectionPoints(pos, 2 * objRadius, from, destDuringTick);

      // destination is invalid
      if (Point.distance(pos, destDuringTick) <= objRadius * 2) {
        intersectPoints.addAll(pts);
      } else {
        // destination is valid, this means we can ignore any intersection
        // points that are within DELTA of our current position (as this is how
        // we can stop colliding with another object)
        for (final Point p : pts) {
          // only include points on the travelline and whom are not too close to
          // origin
          if (isBetween(from, destDuringTick, p)
            && Point.distance(from, p) > DELTA) {
            intersectPoints.add(p);
          }
        }
      }
    }

    if (intersectPoints.isEmpty()) {
      return travelableDistance;
    } else if (hit) {
      return 0d;
    }

    // find closest intersection
    final double minDist = findMinDist(from, intersectPoints);
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

  static boolean isBetween(Point from, Point to, Point pointOnLine) {
    return Math.abs(
      Point.distance(from, pointOnLine) + Point.distance(pointOnLine, to)
        - Point.distance(from, to)) < DELTA;
  }
}
