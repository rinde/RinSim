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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.RandomGenerator;

/**
 * Abstract graph implementation providing basic implementations of several
 * graph functions.
 * @param <E> The type of {@link ConnectionData} that is used at the
 *          {@link Connection}s.
 * @author Rinde van Lon
 */
public abstract class AbstractGraph<E extends ConnectionData> implements
    Graph<E> {

  /**
   * Create a new empty graph.
   */
  public AbstractGraph() {
    super();
  }

  @SuppressWarnings("null")
  @Override
  public double connectionLength(Point from, Point to) {
    checkArgument(hasConnection(from, to),
        "Can not get connection length from a non-existing connection.");
    @Nullable
    final E connData = connectionData(from, to);
    return !isEmptyConnectionData(connData) ? connData.getLength() : Point
        .distance(from, to);
  }

  /**
   * Determines whether a connection data is 'empty'. Default only
   * <code>null</code> is considered as an empty connection data. This can be
   * overridden to include a specific instance of connection data to be the
   * 'empty' instance.
   * @param connData The connection data to check.
   * @return <code>true</code> if the specified connection data is considered
   *         empty, <code>false</code> otherwise.
   */
  protected boolean isEmptyConnectionData(@Nullable E connData) {
    return connData == null;
  }

  @Override
  public void addConnection(Point from, Point to) {
    addConnection(from, to, null);
  }

  @Override
  public void addConnection(Connection<E> c) {
    addConnection(c.from, c.to, c.getData());
  }

  @Override
  public void addConnections(Iterable<? extends Connection<E>> connections) {
    for (final Connection<E> connection : connections) {
      addConnection(connection);
    }
  }

  @Override
  public void addConnections(Collection<? extends Connection<E>> connections) {
    addConnections((Iterable<? extends Connection<E>>) connections);
  }

  @Override
  public void merge(Graph<E> other) {
    addConnections(other.getConnections());
  }

  @Override
  public void addConnection(Point from, Point to, @Nullable E connData) {
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
      @Nullable E connData);

  @Override
  @SuppressWarnings({ "unchecked" })
  public boolean equals(@Nullable Object other) {
    return other instanceof Graph ? Graphs.equal(this, (Graph<E>) other)
        : false;
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
  public Connection<E> getConnection(Point from, Point to) {
    checkArgument(hasConnection(from, to), "%s -> %s is not a connection.",
        from, to);
    return new Connection<>(from, to, connectionData(from, to));
  }

}
