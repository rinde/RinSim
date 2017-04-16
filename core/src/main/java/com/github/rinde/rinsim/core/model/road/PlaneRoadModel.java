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
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.min;
import static java.util.Arrays.asList;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.AbstractGraph;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.GeomHeuristic;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.math.DoubleMath;

/**
 * A {@link RoadModel} that uses a plane as road structure. This assumes that
 * from every point in the plane it is possible to drive to every other point in
 * the plane. The plane has a boundary as defined by a rectangle. Instances can
 * be obtained via {@link RoadModelBuilders#plane()}.
 *
 * @author Rinde van Lon
 */
public class PlaneRoadModel extends AbstractRoadModel {

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

  private final RoadModelSnapshot snapshot;
  private final PlaneGraph<ConnectionData> planeGraph;
  private final SpatialRegistry<RoadUser> registry;

  PlaneRoadModel(RoadModelBuilders.AbstractPlaneRMB<?, ?> b) {
    super(b.getDistanceUnit(), b.getSpeedUnit());
    min = b.getMin();
    max = b.getMax();
    width = max.x - min.x;
    height = max.y - min.y;
    maxSpeed = unitConversion.toInSpeed(b.getMaxSpeed());
    snapshot = PlaneRoadModelSnapshot.create(this);
    planeGraph = new PlaneGraph<>();
    registry = MapSpatialRegistry.create();
  }

  @Override
  protected SpatialRegistry<RoadUser> registry() {
    return registry;
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
    Point loc = registry().getPosition(object);

    System.out.println("followPath " + object);

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
      final double travelableDistance = computeTravelableDistance(loc,
        path.peek(), speed, time.getTimeLeft(), time.getTimeUnit());
      checkState(
        travelableDistance >= 0d,
        "Found a bug in computeTravelableDistance, return value must be >= 0,"
          + " but is %s.",
        travelableDistance);

      final double stepLength = unitConversion.toInDist(
        Point.distance(loc, path.peek()));

      if (travelableDistance >= stepLength) {
        System.out.println("****");
        System.out.println(travelableDistance);
        System.out.println(stepLength);
        loc = path.remove();
        travelledNodes.add(loc);

        final long timeSpent = DoubleMath.roundToLong(
          unitConversion.toExTime(stepLength / speed, time.getTimeUnit()),
          RoundingMode.HALF_DOWN);
        time.consume(timeSpent);
        traveled += stepLength;
      } else {

        if (stepLength - travelableDistance < DELTA) {
          loc = path.peek();
          traveled += stepLength;
        } else {
          final Point diff = Point.diff(path.peek(), loc);
          final double perc = travelableDistance / stepLength;
          loc = new Point(loc.x + perc * diff.x, loc.y + perc * diff.y);
          traveled += travelableDistance;
        }
        time.consumeAll();

      }
    }
    registry().addAt(object, loc);

    // convert to external units
    final Measure<Double, Length> distTraveled = unitConversion
      .toExDistMeasure(traveled);
    final Measure<Long, Duration> timeConsumed = Measure.valueOf(
      time.getTimeConsumed() - startTimeConsumed, time.getTimeUnit());
    return MoveProgress.create(distTraveled, timeConsumed, travelledNodes);
  }

  /**
   * Computes the distance that can be traveled between <code>from</code> and
   * <code>to</code> at the specified <code>speed</code> and using the available
   * <code>time</code>. This method can optionally be overridden to change the
   * move behavior of the model. The return value of the method is interpreted
   * in the following way:
   * <ul>
   * <li><code>if travelableDistance &lt; distance(from,to)</code> then there is
   * either:
   * <ul>
   * <li>not enough time left to travel the whole distance</li>
   * <li>another reason (e.g. an obstacle on the way) that prevents traveling
   * the whole distance</li>
   * </ul>
   * <li><code>if travelableDistance &ge; distance(from,to)</code> then it is
   * possible to travel the whole distance at once.</li>
   * </ul>
   * @param from The start position for this travel.
   * @param to The destination position for this travel.
   * @param speed The travel speed.
   * @param timeLeft The time available for traveling.
   * @param timeUnit Unit in which <code>timeLeft</code> is expressed.
   * @return The distance that can be traveled, must be &ge; 0.
   */
  protected double computeTravelableDistance(Point from, Point to, double speed,
      long timeLeft, Unit<Duration> timeUnit) {
    return speed * unitConversion.toInTime(timeLeft, timeUnit);
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
      "to must be within the predefined boundary of the plane, to is %s,"
        + " boundary: min %s, max %s.",
      to, min, max);
    return asList(from, to);
  }

  @Override
  public RoadPath getPathTo(Point from, Point to, Unit<Duration> timeUnit,
      Measure<Double, Velocity> speed, GeomHeuristic heuristic) {
    return RoadPath.create(
      asList(from, to),
      heuristic.calculateCost(planeGraph, from, to),
      heuristic.calculateTravelTime(planeGraph, from, to, getDistanceUnit(),
        speed, timeUnit));
  }

  @Override
  public Measure<Double, Length> getDistanceOfPath(Iterable<Point> path)
      throws IllegalArgumentException {
    final List<Point> pathAsList = Lists.newArrayList(path);
    checkArgument(pathAsList.size() > 1,
      "Cannot evaluate the distance of a path with less than two points.");
    return Measure.valueOf(Graphs.pathLength(pathAsList),
      getDistanceUnit());
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
  @Nonnull
  public <U> U get(Class<U> type) {
    return type.cast(this);
  }

  @Override
  public RoadModelSnapshot getSnapshot() {
    return snapshot;
  }

  private static class PlaneGraph<E extends ConnectionData>
      extends AbstractGraph<E> {

    PlaneGraph() {}

    @Override
    public boolean containsNode(Point node) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Point> getOutgoingConnections(Point node) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Point> getIncomingConnections(Point node) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasConnection(Point from, Point to) {
      return true;
    }

    @Override
    public <T extends ConnectionData> boolean hasConnection(
        Connection<T> connection) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Connection<E> getConnection(Point from, Point to) {
      return Connection.create(from, to);
    }

    @Override
    public Optional<E> connectionData(Point from, Point to) {
      return Optional.absent();
    }

    @Override
    public int getNumberOfConnections() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<Connection<E>> getConnections() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getNumberOfNodes() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<Point> getNodes() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void removeNode(Point node) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void removeConnection(Point from, Point to) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected void doAddConnection(Point from, Point to, Optional<E> connData) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected Optional<E> doChangeConnectionData(Point from, Point to,
        Optional<E> connData) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return false;
    }

  }
}
