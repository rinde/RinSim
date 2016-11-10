/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.ImmutableSet;

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
public interface CollisionGraphRoadModel extends DynamicGraphRoadModel {

  /**
   * Checks whether the specified node is occupied.
   * @param node The node to check for occupancy.
   * @return <code>true</code> if the specified node is occupied,
   *         <code>false</code> otherwise.
   */
  boolean isOccupied(Point node);

  /**
   * Checks whether the specified node is occupied by the specified
   * {@link MovingRoadUser}.
   * @param node The node to check for occupancy.
   * @param user The user to check if it is occupying that location.
   * @return <code>true</code> if the specified node is occupied by the
   *         specified user, <code>false</code> otherwise.
   * @throws IllegalArgumentException If road user is not known by this model.
   */
  boolean isOccupiedBy(Point node, MovingRoadUser user);

  /**
   * @return A read-only <b>indeterministic</b> ordered copy of all currently
   *         occupied nodes in the graph.
   */
  ImmutableSet<Point> getOccupiedNodes();

  /**
   * @return The length of all vehicles. The length is expressed in the unit as
   *         specified by {@link #getDistanceUnit()}.
   */
  double getVehicleLength();

  /**
   * @return The minimum distance vehicles need to be apart from each other. The
   *         length is expressed in the unit as specified by
   *         {@link #getDistanceUnit()}.
   */
  double getMinDistance();

  /**
   * @return The minimum length all connections need to have in the graph. The
   *         length is expressed in the unit as specified by
   *         {@link #getDistanceUnit()}.
   */
  double getMinConnLength();

}
