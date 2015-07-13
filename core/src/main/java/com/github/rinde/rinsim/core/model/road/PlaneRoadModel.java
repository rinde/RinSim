/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
import static java.lang.Math.min;
import static java.util.Arrays.asList;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

/**
 * A {@link RoadModel} that uses a plane as road structure. This assumes that
 * from every point in the plane it is possible to drive to every other point in
 * the plane. The plane has a boundary as defined by a rectangle. Instances can
 * be obtained via {@link RoadModelBuilders#plane()}.
 *
 * @author Rinde van Lon
 */
public class PlaneRoadModel extends AbstractRoadModel<Point> {

  /**
   * The minimum travelable distance.
   */
  protected static final double DELTA = 0.000001;

  /**
   * The minimum x and y of the plane.
   */
  public final Point min;
  /**
   * The maximum x and y of the plane.
   */
  public final Point max;
  /**
   * The width of the plane.
   */
  public final double width;
  /**
   * The height of the plane.
   */
  public final double height;
  /**
   * The maximum speed in meters per second that objects can travel on the
   * plane.
   */
  public final double maxSpeed;

  PlaneRoadModel(RoadModelBuilders.PlaneRMB b) {
    super(b.getDistanceUnit(), b.getSpeedUnit());
    min = b.getMin();
    max = b.getMax();
    width = max.x - min.x;
    height = max.y - min.y;
    maxSpeed = unitConversion.toInSpeed(b.getMaxSpeed());
  }

  @Override
  public Point getRandomPosition(RandomGenerator rnd) {
    return new Point(min.x + rnd.nextDouble() * width, min.y
        + rnd.nextDouble() * height);
  }

  @Override
  public void addObjectAt(RoadUser obj, Point pos) {
    checkArgument(
        isPointInBoundary(pos),
        "objects can only be added within the boundaries of the plane, %s is "
            + "not in the boundary.",
        pos);
    super.addObjectAt(obj, pos);
  }

  @Override
  protected MoveProgress doFollowPath(MovingRoadUser object, Queue<Point> path,
      TimeLapse time) {
    final long startTimeConsumed = time.getTimeConsumed();
    Point loc = objLocs.get(object);

    double traveled = 0;
    final double speed = min(unitConversion.toInSpeed(object.getSpeed()),
        maxSpeed);
    if (speed == 0d) {
      // FIXME add test for this case, also check GraphRoadModel
      final Measure<Double, Length> dist = Measure.valueOf(0d,
          getDistanceUnit());
      final Measure<Long, Duration> dur = Measure.valueOf(0L,
          time.getTimeUnit());
      return MoveProgress.create(dist, dur, new ArrayList<Point>());
    }

    final List<Point> travelledNodes = new ArrayList<>();
    while (time.hasTimeLeft() && !path.isEmpty()) {
      checkArgument(isPointInBoundary(path.peek()),
          "points in the path must be within the predefined boundary of the "
              + "plane");

      // distance in internal time unit that can be traveled with timeleft
      final double travelDistance = speed
          * unitConversion.toInTime(time.getTimeLeft(),
              time.getTimeUnit());
      final double stepLength = unitConversion.toInDist(Point
          .distance(loc, path.peek()));

      if (travelDistance >= stepLength) {
        loc = path.remove();
        travelledNodes.add(loc);

        final long timeSpent = DoubleMath.roundToLong(
            unitConversion.toExTime(stepLength / speed,
                time.getTimeUnit()),
            RoundingMode.HALF_DOWN);
        time.consume(timeSpent);
        traveled += stepLength;
      } else {
        final Point diff = Point.diff(path.peek(), loc);

        if (stepLength - travelDistance < DELTA) {
          loc = path.peek();
          traveled += stepLength;
        } else {
          final double perc = travelDistance / stepLength;
          loc = new Point(loc.x + perc * diff.x, loc.y + perc * diff.y);
          traveled += travelDistance;
        }
        time.consumeAll();

      }
    }
    objLocs.put(object, loc);

    // convert to external units
    final Measure<Double, Length> distTraveled = unitConversion
        .toExDistMeasure(traveled);
    final Measure<Long, Duration> timeConsumed = Measure.valueOf(
        time.getTimeConsumed() - startTimeConsumed, time.getTimeUnit());
    return MoveProgress.create(distTraveled, timeConsumed, travelledNodes);
  }

  @Override
  public List<Point> getShortestPathTo(Point from, Point to) {
    checkArgument(
        isPointInBoundary(from),
        "from must be within the predefined boundary of the plane, from is %s, "
            + "boundary: min %s, max %s.",
        to, min, max);
    checkArgument(
        isPointInBoundary(to),
        "to must be within the predefined boundary of the plane, to is %s, "
            + "boundary: min %s, max %s.",
        to, min, max);
    return asList(from, to);
  }

  @Override
  protected Point locObj2point(Point locObj) {
    return locObj;
  }

  @Override
  protected Point point2LocObj(Point point) {
    return point;
  }

  /**
   * Checks whether the specified point is within the plane as defined by this
   * model.
   * @param p The point to check.
   * @return <code>true</code> if the points is within the boundary,
   *         <code>false</code> otherwise.
   */
  // TODO give more general name?
  protected boolean isPointInBoundary(Point p) {
    return p.x >= min.x && p.x <= max.x && p.y >= min.y && p.y <= max.y;
  }

  @Override
  public ImmutableList<Point> getBounds() {
    return ImmutableList.of(min, max);
  }

  @Override
  public <U> U get(Class<U> type) {
    return type.cast(this);
  }
}
