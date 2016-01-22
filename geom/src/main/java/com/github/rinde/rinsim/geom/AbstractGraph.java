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
package com.github.rinde.rinsim.geom;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.RandomGenerator;

import com.google.common.base.Optional;

/**
 * Abstract graph implementation providing basic implementations of several
 * graph functions.
 * @author Rinde van Lon
 * @param <E> The type of {@link ConnectionData} that is used at the
 *          {@link Connection}s.
 */
public abstract class AbstractGraph<E extends ConnectionData> implements
    Graph<E> {

  /**
   * Create a new empty graph.
   */
  public AbstractGraph() {
    super();
  }

  @Override
  public double connectionLength(Point from, Point to) {
    checkArgument(hasConnection(from, to),
        "Can not get connection length from a non-existing connection.");
    final Optional<E> connData = connectionData(from, to);
    return connData.isPresent() && connData.get().getLength().isPresent()
        ? connData.get().getLength().get()
        : Point.distance(from, to);
  }

  @Override
  public void addConnection(Point from, Point to) {
    addConnection(from, to, Optional.<E>absent());
  }

  @Override
  public void addConnection(Connection<E> c) {
    addConnection(c.from(), c.to(), c.data());
  }

  @Override
  public void addConnections(Iterable<? extends Connection<E>> connections) {
    for (final Connection<E> connection : connections) {
      addConnection(connection);
    }
  }

  @Override
  public void merge(Graph<E> other) {
    addConnections(other.getConnections());
  }

  @Override
  public void addConnection(Point from, Point to, E connData) {
    addConnection(from, to, Optional.of(connData));
  }

  /**
   * Adds a connection.
   * @param from Start of the connection.
   * @param to End of the connection.
   * @param connData The connection data wrapped in an optional.
   */
  protected void addConnection(Point from, Point to, Optional<E> connData) {
    checkArgument(!from.equals(to),
        "A connection cannot be circular: %s -> %s ", from, to);
    checkArgument(!hasConnection(from, to),
        "Connection already exists: %s -> %s ", from, to);
    doAddConnection(from, to, connData);
  }

  /**
   * Must be overridden by implementors. It should add a connection between from
   * and to. It can be assumed that the connection does not yet exist and that
   * it is not circular.
   * @param from Starting point of the connection.
   * @param to End point of the connection.
   * @param connData The data to be associated to the connection.
   */
  protected abstract void doAddConnection(Point from, Point to,
      Optional<E> connData);

  @Override
  public boolean equals(@Nullable Object other) {
    return Graphs.equal(this, other);
  }

  @Override
  public abstract int hashCode();

  @Override
  public Point getRandomNode(RandomGenerator generator) {
    checkState(!isEmpty(), "Can not find a random node in an empty graph.");
    final Set<Point> nodes = getNodes();
    final int idx = generator.nextInt(nodes.size());
    int index = 0;
    for (final Point point : nodes) {
      if (idx == index++) {
        return point;
      }
    }
    throw new IllegalStateException();
  }

  @Override
  public Optional<E> setConnectionData(Point from, Point to, E connData) {
    return changeConnectionData(from, to, Optional.of(connData));
  }

  @Override
  public Optional<E> removeConnectionData(Point from, Point to) {
    return changeConnectionData(from, to, Optional.<E>absent());
  }

  /**
   * Change connection data. Precondition: connection from -&gt; to exists.
   * @param from Start point of connection.
   * @param to End point of connection.
   * @param connData The connection data used for the connection.
   * @return old connection data or {@link Optional#absent()} if there was no
   *         connection data.
   * @throws IllegalArgumentException if the connection between the nodes does
   *           not exist.
   */
  protected Optional<E> changeConnectionData(Point from, Point to,
      Optional<E> connData) {
    checkArgument(hasConnection(from, to),
        "The connection %s->%s does not exist.", from, to);
    return doChangeConnectionData(from, to, connData);
  }

  /**
   * Change connection data. It can be assumed that the connection exists.
   * @param from Start point of connection.
   * @param to End point of connection.
   * @param connData The connection data used for the connection.
   * @return old connection data or {@link Optional#absent()} if there was no
   *         connection data.
   */
  protected abstract Optional<E> doChangeConnectionData(Point from, Point to,
      Optional<E> connData);

}
