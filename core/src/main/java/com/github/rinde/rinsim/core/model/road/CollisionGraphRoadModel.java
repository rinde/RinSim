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
import static com.google.common.base.Verify.verify;
import static java.util.Objects.hash;

import java.util.Collection;
import java.util.Objects;
import java.util.Queue;

import javax.annotation.Nullable;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.ListenableGraph.GraphEvent;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.CategoryMap;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.primitives.Doubles;

/**
 * Graph road model that avoids collisions between {@link RoadUser}s. When a
 * dead lock situation arises a {@link DeadlockException} is thrown, note that a
 * grid lock situation (spanning multiple connections) is not detected.
 * Instances can be obtained via a dedicated builder, see
 * {@link #builderCollision(ListenableGraph)}.
 * <p>
 * The graph can be modified at runtime, for information about modifying the
 * graph see {@link DynamicGraphRoadModel}.
 * @author Rinde van Lon
 */
public class CollisionGraphRoadModel extends DynamicGraphRoadModel {
  private final double minConnLength;
  private final double vehicleLength;
  private final double minDistance;
  private final SetMultimap<RoadUser, Point> occupiedNodes;

  CollisionGraphRoadModel(ListenableGraph<?> g, double pMinConnLength,
    Builder builder) {
    super(g, builder);
    vehicleLength = unitConversion.toInDist(builder.vehicleLength);
    minDistance = unitConversion.toInDist(builder.minDistance);
    minConnLength = unitConversion.toInDist(pMinConnLength);
    occupiedNodes = Multimaps.synchronizedSetMultimap(CategoryMap
      .<RoadUser, Point> create());
    getGraph().getEventAPI().addListener(
      new ModificationChecker(minConnLength),
      ListenableGraph.EventTypes.ADD_CONNECTION,
      ListenableGraph.EventTypes.CHANGE_CONNECTION_DATA);
  }

  @Override
  protected MoveProgress doFollowPath(MovingRoadUser object, Queue<Point> path,
    TimeLapse time) {
    if (occupiedNodes.containsKey(object)) {
      occupiedNodes.removeAll(object);
    }
    final MoveProgress mp;
    try {
      mp = super.doFollowPath(object, path, time);
    } catch (final IllegalArgumentException e) {
      throw e;
    } finally {
      // detects if the new location of the object occupies a node
      final Loc loc = objLocs.get(object);
      if (loc.isOnConnection()) {
        if (loc.relativePos < vehicleLength + minDistance) {
          verify(occupiedNodes.put(object, loc.conn.get().from()));
        }
        if (loc.relativePos > loc.connLength - vehicleLength - minDistance) {
          occupiedNodes.put(object, loc.conn.get().to());
        }
      } else {
        occupiedNodes.put(object, loc);
      }
    }
    return mp;
  }

  @Override
  protected double computeTravelableDistance(Loc from, Point to, double speed,
    long timeLeft, Unit<Duration> timeUnit) {
    double closestDist = Double.POSITIVE_INFINITY;
    if (!from.equals(to)) {
      final Connection<?> conn = getConnection(from, to);
      // check if the node is occupied
      if (occupiedNodes.containsValue(conn.to())) {
        closestDist = (from.isOnConnection()
          ? from.connLength - from.relativePos
          : conn.getLength())
          - vehicleLength - minDistance;
      }
      // check if there is an obstacle on the connection
      if (connMap.containsKey(conn)) {
        // if yes, how far is it from 'from'
        final Collection<RoadUser> potentialObstacles = connMap.get(conn);
        for (final RoadUser ru : potentialObstacles) {
          final Loc loc = objLocs.get(ru);
          if (loc.isOnConnection() && loc.relativePos > from.relativePos) {
            final double dist = loc.relativePos - from.relativePos
              - vehicleLength - minDistance;
            if (dist < closestDist) {
              closestDist = dist;
            }
          }
        }
      }

    }
    verify(closestDist >= 0d, "", from, to);
    return Math.min(closestDist,
      super.computeTravelableDistance(from, to, speed, timeLeft, timeUnit));
  }

  @Override
  protected void checkMoveValidity(Loc objLoc, Point nextHop) {
    super.checkMoveValidity(objLoc, nextHop);
    // check if there is a vehicle driving in the opposite direction
    if (!objLoc.equals(nextHop)) {
      final Connection<?> conn = getConnection(objLoc, nextHop);
      if (graph.hasConnection(conn.to(), conn.from())
        && connMap.containsKey(graph.getConnection(conn.to(), conn.from()))) {
        throw new DeadlockException(conn);
      }
    }
  }

  @Override
  public void addObjectAt(RoadUser newObj, Point pos) {
    checkArgument(!occupiedNodes.containsValue(pos),
      "An object can not be added on an already occupied position %s.", pos);
    occupiedNodes.put(newObj, pos);
    super.addObjectAt(newObj, pos);
  }

  @Override
  @Deprecated
  public void addObjectAtSamePosition(RoadUser newObj, RoadUser existingObj) {
    throw new UnsupportedOperationException(
      "Vehicles can not be added at the same position.");
  }

  /**
   * Checks whether the specified node is occupied.
   * @param node The node to check for occupancy.
   * @return <code>true</code> if the specified node is occupied,
   *         <code>false</code> otherwise.
   */
  public boolean isOccupied(Point node) {
    return occupiedNodes.containsValue(node);
  }

  /**
   * @return A read-only <b>indeterministic</b> ordered copy of all currently
   *         occupied nodes in the graph.
   */
  public ImmutableSet<Point> getOccupiedNodes() {
    ImmutableSet<Point> set;
    synchronized (occupiedNodes) {
      set = ImmutableSet.copyOf(occupiedNodes.values());
    }
    return set;
  }

  /**
   * @return The length of all vehicles. The length is expressed in the unit as
   *         specified by {@link #getDistanceUnit()}.
   */
  public double getVehicleLength() {
    return vehicleLength;
  }

  /**
   * @return The minimum distance vehicles need to be apart from each other. The
   *         length is expressed in the unit as specified by
   *         {@link #getDistanceUnit()}.
   */
  public double getMinDistance() {
    return minDistance;
  }

  /**
   * @return The minimum length all connections need to have in the graph. The
   *         length is expressed in the unit as specified by
   *         {@link #getDistanceUnit()}.
   */
  public double getMinConnLength() {
    return minConnLength;
  }

  /**
   * Create a {@link Builder} for constructing {@link CollisionGraphRoadModel}
   * instances. Note that all connections in the specified graph must have
   * length <code>2 * vehicleLength</code>, where vehicle length can be
   * specified in {@link Builder#setVehicleLength(double)}.
   * @param graph A {@link ListenableGraph}.
   * @return A new {@link Builder} instance.
   */
  public static Builder builderCollision(ListenableGraph<?> graph) {
    return new Builder(graph);
  }

  static void checkConnectionLength(double minConnLength, Connection<?> conn) {
    checkArgument(
      Point.distance(conn.from(), conn.to()) >= minConnLength,
      "Invalid graph: the minimum connection length is %s, connection %s->%s is too short.",
      minConnLength, conn.from(), conn.to());
    checkArgument(
      conn.getLength() >= minConnLength,
      "Invalid graph: the minimum connection length is %s, connection %s->%s defines length data that is too short: %s.",
      minConnLength, conn.from(), conn.to(), conn.getLength());
  }

  /**
   * A builder for constructing {@link CollisionGraphRoadModel} instances. Use
   * {@link CollisionGraphRoadModel#builderCollision(ListenableGraph)} for
   * obtaining builder instances.
   * @author Rinde van Lon
   */
  public static final class Builder extends
    DynamicGraphRoadModel.AbstractBuilder<CollisionGraphRoadModel, Builder> {

    /**
     * The default vehicle length: <code>2</code>.
     */
    public static final double DEFAULT_VEHICLE_LENGTH = 2;

    /**
     * The default minimum distance: <code>.25</code>.
     */
    public static final double DEFAULT_MIN_DISTANCE = .25;

    double vehicleLength;
    double minDistance;

    Builder(ListenableGraph<?> g) {
      super(Suppliers.ofInstance(g));
      vehicleLength = DEFAULT_VEHICLE_LENGTH;
      minDistance = DEFAULT_MIN_DISTANCE;
      setDistanceUnit(SI.METER);
    }

    /**
     * Sets the distance unit to for all dimensions. The default is
     * {@link SI#METER}.
     * @param unit The distance unit to set.
     * @return This, as per the builder pattern.
     */
    // this method is overridden just to override the JavaDoc comment such that
    // it displays the correct default unit
    @Override
    public Builder setDistanceUnit(Unit<Length> unit) {
      return super.setDistanceUnit(unit);
    }

    /**
     * Sets the length of each vehicle added to the
     * {@link CollisionGraphRoadModel} that will be constructed by this builder.
     * The vehicle length must be a strictly positive number. The default value
     * is {@link #DEFAULT_VEHICLE_LENGTH}.
     * @param length A length expressed in the unit set by
     *          {@link #setDistanceUnit(Unit)}.
     * @return This, as per the builder pattern.
     */
    public Builder setVehicleLength(double length) {
      checkArgument(length > 0d,
        "Only positive vehicle lengths are allowed, found %s.", length);
      checkArgument(Doubles.isFinite(length),
        "%s is not a valid vehicle length.", length);
      vehicleLength = length;
      return this;
    }

    /**
     * Sets the minimum required distance between two vehicles. The minimum
     * distance must be a positive number &le; to 2 * vehicle length. The
     * default value is {@link #DEFAULT_MIN_DISTANCE}.
     * @param dist A distance expressed in the unit set by
     *          {@link #setDistanceUnit(Unit)}.
     * @return This, as per the builder pattern.
     */
    public Builder setMinDistance(double dist) {
      checkArgument(dist >= 0d);
      minDistance = dist;
      return this;
    }

    @Override
    public CollisionGraphRoadModel build(DependencyProvider dependencyProvider) {
      final double minConnectionLength = vehicleLength;
      checkArgument(
        minDistance <= minConnectionLength,
        "Min distance must be smaller than 2 * vehicle length (%s), but is %s.",
        vehicleLength, minDistance);
      final ListenableGraph<?> graph = getGraph();

      for (final Connection<?> conn : graph.getConnections()) {
        checkConnectionLength(minConnectionLength, conn);
      }
      return new CollisionGraphRoadModel(graph, minConnectionLength, this);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (other == null || other.getClass() != getClass()) {
        return false;
      }
      final Builder o = (Builder) other;
      return Objects.equals(minDistance, o.minDistance)
        && Objects.equals(vehicleLength, o.vehicleLength)
        && super.equals(other);
    }

    @Override
    public int hashCode() {
      return hash(minDistance, vehicleLength, getGraph(), getDistanceUnit(),
        getSpeedUnit());
    }

    @Override
    protected Builder self() {
      return this;
    }
  }

  static class ModificationChecker implements Listener {
    private final double minConnLength;

    ModificationChecker(double minLength) {
      minConnLength = minLength;
    }

    @Override
    public void handleEvent(Event e) {
      verify(e instanceof GraphEvent);
      final GraphEvent event = (GraphEvent) e;
      checkConnectionLength(minConnLength, event.getConnection());
    }
  }
}
