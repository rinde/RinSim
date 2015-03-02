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
import static java.lang.Math.min;
import static java.util.Arrays.asList;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

/**
 * A {@link RoadModel} that uses a plane as road structure. This assumes that
 * from every point in the plane it is possible to drive to every other point in
 * the plane. The plane has a boundary as defined by a rectangle. Instances can
 * be obtained via {@link #builder()}.
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

  /**
   * Create a new plane road model using the specified boundaries and max speed.
   * @param pMin The minimum x and y of the plane.
   * @param pMax The maximum x and y of the plane.
   * @param distanceUnit This is the unit in which all input distances and
   *          locations (i.e. {@link Point}s) should be specified.
   * @param pMaxSpeed The maximum speed that objects can travel on the plane.
   */
  @Deprecated
  public PlaneRoadModel(Point pMin, Point pMax, Unit<Length> distanceUnit,
      Measure<Double, Velocity> pMaxSpeed) {
    super(distanceUnit, pMaxSpeed.getUnit());
    checkArgument(pMin.x < pMax.x && pMin.y < pMax.y,
        "min should have coordinates smaller than max");
    checkArgument(pMaxSpeed.getValue() > 0, "max speed must be positive");
    min = pMin;
    max = pMax;
    width = max.x - min.x;
    height = max.y - min.y;
    maxSpeed = unitConversion.toInSpeed(pMaxSpeed);
  }

  /**
   * Create a new plane road model using the specified boundaries and max speed.
   * It uses {@link SI#KILOMETER} for distances and
   * {@link NonSI#KILOMETERS_PER_HOUR} for speeds.
   * @param pMin The minimum x and y of the plane.
   * @param pMax The maximum x and y of the plane.
   * @param speedLimitInKmh The maximum speed that objects can travel on the
   *          plane.
   */
  @Deprecated
  public PlaneRoadModel(Point pMin, Point pMax, double speedLimitInKmh) {
    this(pMin, pMax, SI.KILOMETER, Measure.valueOf(speedLimitInKmh,
        NonSI.KILOMETERS_PER_HOUR));
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
        "objects can only be added within the boundaries of the plane, %s is not in the boundary.",
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

    final List<Point> travelledNodes = new ArrayList<Point>();
    while (time.hasTimeLeft() && !path.isEmpty()) {
      checkArgument(isPointInBoundary(path.peek()),
          "points in the path must be within the predefined boundary of the plane");

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
        "from must be within the predefined boundary of the plane, from is %s, boundary: min %s, max %s.",
        to, min, max);
    checkArgument(
        isPointInBoundary(to),
        "to must be within the predefined boundary of the plane, to is %s, boundary: min %s, max %s.",
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

  /**
   * @return A new {@link Builder} for creating a {@link PlaneRoadModel}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Create a {@link Supplier} for {@link PlaneRoadModel}s.
   * @param min The minimum x and y of the plane.
   * @param max The maximum x and y of the plane.
   * @param distanceUnit This is the unit in which all input distances and
   *          locations (i.e. {@link Point}s) should be specified.
   * @param maxSpeed The maximum speed that objects can travel on the plane.
   * @return A newly created supplier.
   */
  public static Supplier<PlaneRoadModel> supplier(
      final Point min,
      final Point max,
      final Unit<Length> distanceUnit,
      final Measure<Double, Velocity> maxSpeed) {
    return new DefaultSupplier(min, max, distanceUnit, maxSpeed);
  }

  /**
   * A builder for {@link PlaneRoadModel}.
   * @author Rinde van Lon
   */
  public static class Builder {
    Point min;
    Point max;
    Unit<Length> distUnit;
    Unit<Velocity> velocityUnit;
    double maxSpeed;

    Builder() {
      min = new Point(0, 0);
      max = new Point(10, 10);
      distUnit = SI.KILOMETER;
      velocityUnit = NonSI.KILOMETERS_PER_HOUR;
      maxSpeed = 50d;
    }

    /**
     * Sets the min point that defines the left top corner of the plane. The
     * default is <code>(0,0)</code>.
     * @param minPoint The min point to set.
     * @return This, as per the builder pattern.
     */
    public Builder setMinPoint(Point minPoint) {
      min = minPoint;
      return this;
    }

    /**
     * Sets the max point that defines the right bottom corner of the plane. The
     * default is <code>(10,10)</code>.
     * @param maxPoint The max point to set.
     * @return This, as per the builder pattern.
     */
    public Builder setMaxPoint(Point maxPoint) {
      max = maxPoint;
      return this;
    }

    /**
     * Sets the distance unit to for all dimensions. The default is
     * {@link SI#KILOMETER}.
     * @param distanceUnit The distanceUnit to set.
     * @return This, as per the builder pattern.
     */
    public Builder setDistanceUnit(Unit<Length> distanceUnit) {
      distUnit = distanceUnit;
      return this;
    }

    /**
     * Sets the speed unit to use for all speeds. The default is
     * {@link NonSI#KILOMETERS_PER_HOUR}.
     * @param speedUnit The speedUnit to set
     * @return This, as per the builder pattern.
     */
    public Builder setSpeedUnit(Unit<Velocity> speedUnit) {
      velocityUnit = speedUnit;
      return this;
    }

    /**
     * Sets the maximum speed to use for all vehicles in the model. The default
     * is <code>50</code>.
     * @param speed The max speed to set.
     * @return This, as per the builder pattern.
     */
    public Builder setMaxSpeed(double speed) {
      maxSpeed = speed;
      return this;
    }

    /**
     * @return A new {@link PlaneRoadModel} instance.
     */
    public PlaneRoadModel build() {
      return new PlaneRoadModel(min, max, distUnit,
          Measure.valueOf(maxSpeed, velocityUnit));
    }

  }

  private static class DefaultSupplier implements Supplier<PlaneRoadModel> {
    final Point min;
    final Point max;
    final Unit<Length> distanceUnit;
    final Measure<Double, Velocity> maxSpeed;

    DefaultSupplier(Point mi, Point ma, Unit<Length> du,
        Measure<Double, Velocity> ms) {
      min = mi;
      max = ma;
      distanceUnit = du;
      maxSpeed = ms;
    }

    @Override
    public PlaneRoadModel get() {
      return new PlaneRoadModel(min, max, distanceUnit, maxSpeed);
    }
  }
}
