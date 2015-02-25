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
package com.github.rinde.rinsim.geom;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import com.google.common.base.Optional;

/**
 * Common interface for graphs (V,E). Vertices are called <code>nodes</code>
 * which are represented as {@link Point}s and connections are represented by
 * {@link Connection}s. Graphs are directed.
 * 
 * @author Rinde van Lon
 * @author Bartosz Michalik - added connection data handling
 * @param <E> The type of {@link ConnectionData} that is used in the
 *          connections.
 * @since 1.0
 */
public interface Graph<E extends ConnectionData> {

  /**
   * Checks whether the specified node is a vertex in this graph.
   * @param node The node to check.
   * @return <code>true</code> if the node is a vertex in this graph,
   *         <code>false</code> otherwise.
   */
  boolean containsNode(Point node);

  /**
   * @param node The node to check for outgoing connections.
   * @return The outgoing connections starting from the specified node.
   */
  Collection<Point> getOutgoingConnections(Point node);

  /**
   * @param node The node to check for incoming connections.
   * @return The incoming connections starting from the specified node.
   */
  Collection<Point> getIncomingConnections(Point node);

  /**
   * Checks if a directed connection between <code>from</code> and
   * <code>to</code> exists.
   * @param from The starting node.
   * @param to The end node.
   * @return <code>true</code> if the connection exist, <code>false</code>
   *         otherwise.
   */
  boolean hasConnection(Point from, Point to);

  /**
   * Checks if the same (as defined by {@link Connection#equals(Object)})
   * connection exists in this graph.
   * @param <T> The type of connection.
   * @param connection The connection to check.
   * @return <code>true</code> if the connection exist, <code>false</code>
   *         otherwise.
   */
  <T extends ConnectionData> boolean hasConnection(Connection<T> connection);

  /**
   * Returns a {@link Connection} between <code>from</code> and <code>to</code>.
   * @param from The starting node.
   * @param to The end node.
   * @return the {@link Connection}.
   * @throws IllegalArgumentException if connection does not exist.
   */
  Connection<E> getConnection(Point from, Point to);

  /**
   * Get the data associated with connection.
   * @param from Start of connection
   * @param to End of connection
   * @return connection data or {@link Optional#absent()} if there is no data or
   *         connection does not exists.
   */
  Optional<E> connectionData(Point from, Point to);

  /**
   * Computes the length of the connection between <code>from</code> and
   * <code>to</code>.
   * @param from Start of connection.
   * @param to End of connection.
   * @return The length of the connection.
   * @throws IllegalArgumentException if connection does not exist.
   */
  double connectionLength(Point from, Point to);

  /**
   * @return The total number of connections in this graph.
   */
  int getNumberOfConnections();

  /**
   * @return All connections in this graph.
   */
  List<Connection<E>> getConnections();

  /**
   * @return The total number of nodes in this graph.
   */
  int getNumberOfNodes();

  /**
   * @return All nodes in this graph.
   */
  Set<Point> getNodes();

  /**
   * Add connection to the graph.
   * @param from starting node
   * @param to end node
   * @param connectionData data associated with the connection
   * @throws IllegalArgumentException if the connection already exists.
   */
  void addConnection(Point from, Point to, E connectionData);

  /**
   * Add a connection to the graph.
   * @param from Starting node
   * @param to End node
   * @throws IllegalArgumentException if the connection already exists.
   */
  void addConnection(Point from, Point to);

  /**
   * Add connection to the graph.
   * @param connection the connection to add.
   * @throws IllegalArgumentException if the connection already exists.
   */
  void addConnection(Connection<E> connection);

  /**
   * Set connection data. Precondition: connection from -&gt; to exists.
   * @param from Start point of connection.
   * @param to End point of connection.
   * @param connectionData The connection data used for the connection.
   * @return old connection data or {@link Optional#absent()} if there was no
   *         connection data.
   * @throws IllegalArgumentException when the connection between nodes do not
   *           exist.
   */
  Optional<E> setConnectionData(Point from, Point to, E connectionData);

  /**
   * Remove connection data. Precondition: connection from -&gt; to exists.
   * @param from Start point of connection.
   * @param to End point of connection.
   * @return old connection data or {@link Optional#absent()} if there was no
   *         connection data.
   * @throws IllegalArgumentException when the connection between nodes do not
   *           exist.
   */
  Optional<E> removeConnectionData(Point from, Point to);

  /**
   * Adds connections to the graph.
   * @param connections The connections to add.
   * @throws IllegalArgumentException if any of the connections already exists.
   */
  void addConnections(Iterable<? extends Connection<E>> connections);

  /**
   * Merges <code>other</code> into this graph.
   * @param other The graph to merge into this graph.
   */
  void merge(Graph<E> other);

  /**
   * @return <code>true</code> if this graph contains no nodes and connections,
   *         <code>false</code> otherwise.
   */
  boolean isEmpty();

  /**
   * Removes the specified node from the graph, all connected connections
   * (incoming and outgoing) are removed as well.
   * @param node The node to remove.
   */
  void removeNode(Point node);

  /**
   * Removes connection between <code>from</code> and <code>to</code>.
   * @param from Start node of connection.
   * @param to End node of connection.
   * @throws IllegalArgumentException if connection does not exist.
   */
  void removeConnection(Point from, Point to);

  /**
   * Get a random node in graph.
   * @param generator used to generate the random point.
   * @return random {@link Point}
   */
  Point getRandomNode(RandomGenerator generator);

}
