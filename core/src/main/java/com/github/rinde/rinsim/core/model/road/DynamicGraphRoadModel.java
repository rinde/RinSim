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

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.ImmutableSet;

/**
 * {@link GraphRoadModel} that allows adding and removing connections and nodes
 * in the road graph. The {@link Graph} needs to be supplied as a
 * {@link ListenableGraph}. When the graph reports a change of its structure the
 * model will check whether the modification is allowed. There are two
 * situations in which a graph modification is not allowed:
 * <ul>
 * <li>Removal of a connection or changing a connection when a {@link RoadUser}
 * is on that connection.</li>
 * <li>Removal of the last connection to a node with a {@link RoadUser} on it.
 * </li>
 * </ul>
 * An {@link IllegalStateException} will be thrown upon detection of an invalid
 * modification. It is up to the user to prevent this from happening. The method
 * {@link #hasRoadUserOn(Point, Point)} can be of help for this. Instances can
 * be obtained via {@link RoadModelBuilders#dynamicGraph(ListenableGraph)}.
 * @author Rinde van Lon
 */
public interface DynamicGraphRoadModel extends GraphRoadModel {

  /**
   * Checks whether there is a {@link RoadUser} on the connection between
   * <code>from</code> and <code>to</code> (inclusive).
   * @param from The start point of a connection.
   * @param to The end point of a connection.
   * @return <code>true</code> if a {@link RoadUser} occupies either
   *         <code>from</code>, <code>to</code> or the connection between
   *         <code>from</code> and <code>to</code>, <code>false</code>
   *         otherwise.
   * @throws IllegalArgumentException if no connection exists between
   *           <code>from</code> and <code>to</code>.
   */
  boolean hasRoadUserOn(Point from, Point to);

  /**
   * Returns all {@link RoadUser}s that are on the connection between
   * <code>from</code> and <code>to</code> (inclusive).
   * @param from The start point of a connection.
   * @param to The end point of a connection.
   * @return The {@link RoadUser}s that are on the connection, or an empty set
   *         in case {@link #hasRoadUserOn(Point, Point)} returns
   *         <code>false</code>.
   * @throws IllegalArgumentException if no connection exists between
   *           <code>from</code> and <code>to</code>.
   */
  ImmutableSet<RoadUser> getRoadUsersOn(Point from, Point to);

  /**
   * Returns all {@link RoadUser}s that are on the specified node.
   * @param node A node in the graph.
   * @return The set of {@link RoadUser}s that are <i>exactly</i> at the
   *         position of the node, or an empty set if there are no
   *         {@link RoadUser}s on the node.
   * @throws IllegalArgumentException if the specified point is not a node in
   *           the graph.
   */
  ImmutableSet<RoadUser> getRoadUsersOnNode(Point node);

}
