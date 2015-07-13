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

import java.util.Collection;
import java.util.Queue;

import javax.measure.quantity.Duration;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.ListenableGraph.GraphEvent;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.CategoryMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

/**
 * Graph road model that avoids collisions between {@link RoadUser}s. When a
 * dead lock situation arises a {@link DeadlockException} is thrown, note that a
 * grid lock situation (spanning multiple connections) is not detected.
 * Instances can be obtained via a builder, see
 * {@link RoadModelBuilders#dynamicGraph(ListenableGraph)} and then call
 * {@link RoadModelBuilders.DynamicGraphRMB#withCollisionAvoidance()}.
 * <p>
 * The graph can be modified at runtime, for information about modifying the
 * graph see {@link DynamicGraphRoadModel}.
 * @author Rinde van Lon
 */
public class CollisionGraphRoadModel extends DynamicGraphRoadModel {
  private final double minConnLength;
  private final double vehicleLength;
  private final double minDistance;
  private final SetMultimap<MovingRoadUser, Point> occupiedNodes;

  CollisionGraphRoadModel(ListenableGraph<?> g, double pMinConnLength,
      RoadModelBuilders.CollisionGraphRMB builder) {
    super(g, builder);
    vehicleLength = unitConversion.toInDist(builder.getVehicleLength());
    minDistance = unitConversion.toInDist(builder.getMinDistance());
    minConnLength = unitConversion.toInDist(pMinConnLength);
    occupiedNodes = Multimaps.synchronizedSetMultimap(CategoryMap
        .<MovingRoadUser, Point>create());
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
    if (newObj instanceof MovingRoadUser) {
      checkArgument(
          !occupiedNodes.containsValue(pos),
          "A MovingRoadUser can not be added on an already occupied position "
              + "%s.",
          pos);
      occupiedNodes.put((MovingRoadUser) newObj, pos);
    }
    super.addObjectAt(newObj, pos);
  }

  @Override
  public void addObjectAtSamePosition(RoadUser newObj, RoadUser existingObj) {
    checkExists(existingObj);
    addObjectAt(newObj, getPosition(existingObj));
  }

  @Override
  public void removeObject(RoadUser object) {
    checkExists(object);
    occupiedNodes.removeAll(object);
    super.removeObject(object);
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
   * Checks whether the specified node is occupied by the specified
   * {@link MovingRoadUser}.
   * @param node The node to check for occupancy.
   * @param user The user to check if it is occupying that location.
   * @return <code>true</code> if the specified node is occupied by the
   *         specified user, <code>false</code> otherwise.
   * @throws IllegalArgumentException If road user is not known by this model.
   */
  public boolean isOccupiedBy(Point node, MovingRoadUser user) {
    checkExists(user);
    return occupiedNodes.containsEntry(user, node);
  }

  void checkExists(RoadUser user) {
    checkArgument(containsObject(user), "RoadUser: %s does not exist.", user);
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

  static void checkConnectionLength(double minConnLength, Connection<?> conn) {
    checkArgument(
        Point.distance(conn.from(), conn.to()) >= minConnLength,
        "Invalid graph: the minimum connection length is %s, connection %s->%s "
            + "is too short.",
        minConnLength, conn.from(), conn.to());
    checkArgument(
        conn.getLength() >= minConnLength,
        "Invalid graph: the minimum connection length is %s, connection %s->%s "
            + "defines length data that is too short: %s.",
        minConnLength, conn.from(), conn.to(), conn.getLength());
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
