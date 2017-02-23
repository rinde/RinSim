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
package com.github.rinde.rinsim.geom;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.hash;

import javax.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;

/**
 * An immutable graph, based on the table-based implementation of a graph, as
 * found in {@link TableGraph}. Note that instances can only be truly immutable
 * if {@link ConnectionData} is immutable (as it should be).
 * @author Vincent Van Gestel
 * @author Rinde van Lon
 * @param <E> The type of {@link ConnectionData} that is used.
 * @see ImmutableGraph#copyOf(Graph)
 */
public class ImmutableGraph<E extends ConnectionData> extends AbstractGraph<E> {
  private final ImmutableTable<Point, Point, Connection<E>> data;

  @SuppressWarnings("unchecked")
  ImmutableGraph(Iterable<? extends Connection<? extends E>> connections) {
    final ImmutableTable.Builder<Point, Point, Connection<E>> tableBuilder =
      ImmutableTable.builder();
    for (final Connection<? extends E> conn : connections) {
      tableBuilder.put(conn.from(), conn.to(), (Connection<E>) conn);
    }
    data = tableBuilder.build();
  }

  @Override
  public ImmutableSet<Point> getNodes() {
    return ImmutableSet.<Point>builder()
      .addAll(data.rowKeySet())
      .addAll(data.columnKeySet())
      .build();
  }

  @Override
  public boolean hasConnection(Point from, Point to) {
    return data.contains(from, to);
  }

  @Override
  public <T extends ConnectionData> boolean hasConnection(
      Connection<T> connection) {
    return hasConnection(connection.from(), connection.to())
      && data.get(connection.from(), connection.to()).equals(connection);
  }

  @Override
  public int getNumberOfNodes() {
    return getNodes().size();
  }

  @Override
  public int getNumberOfConnections() {
    return data.size();
  }

  @Override
  public boolean containsNode(Point node) {
    return data.containsRow(node) || data.containsColumn(node);
  }

  @Override
  public ImmutableSet<Point> getOutgoingConnections(Point node) {
    return data.row(node).keySet();
  }

  @Override
  public ImmutableSet<Point> getIncomingConnections(Point node) {
    return data.column(node).keySet();
  }

  /**
   * @throws UnsupportedOperationException always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public void removeNode(Point node) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public void removeConnection(Point from, Point to) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ImmutableSet<Connection<E>> getConnections() {
    return ImmutableSet.copyOf(data.values());
  }

  /**
   * @throws UnsupportedOperationException always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  protected void addConnection(Point from, Point to, Optional<E> connData) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public void merge(Graph<E> other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isEmpty() {
    return data.isEmpty();
  }

  @Override
  public Connection<E> getConnection(Point from, Point to) {
    checkArgument(hasConnection(from, to), "%s -> %s is not a connection",
      from, to);
    return data.get(from, to);
  }

  @Override
  public Optional<E> connectionData(Point from, Point to) {
    if (data.contains(from, to)) {
      return data.get(from, to).data();
    }
    return Optional.absent();
  }

  /**
   * @throws UnsupportedOperationException always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public Optional<E> setConnectionData(Point from, Point to, E connData) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public Optional<E> removeConnectionData(Point from, Point to) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int hashCode() {
    return hash(data);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return Graphs.equal(this, other);
  }

  /**
   * @throws UnsupportedOperationException always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  protected void doAddConnection(Point from, Point to, Optional<E> connData) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  protected Optional<E> doChangeConnectionData(Point from, Point to,
      Optional<E> connData) {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates an immutable copy of the specified {@link Graph}. This method
   * recognizes when the supplied graph is an instance of {@link ImmutableGraph}
   * , and will avoid making a copy in this case.
   * @param graph A graph.
   * @param <E> The type of connection data.
   * @return An immutable copy of the graph.
   */
  @SuppressWarnings("unchecked")
  public static <E extends ConnectionData> ImmutableGraph<E> copyOf(
      Graph<? extends E> graph) {
    if (graph instanceof ImmutableGraph) {
      return (ImmutableGraph<E>) graph;
    }
    return new ImmutableGraph<>(graph.getConnections());
  }

  /**
   * Creates an immutable graph based on the specified connections. Duplicate
   * connections are not allowed and will cause this method to fail.
   * @param connections The connections to use for creating a graph.
   * @return A new instance of an immutable graph.
   */
  public static <E extends ConnectionData> ImmutableGraph<E> copyOf(
      Iterable<? extends Connection<? extends E>> connections) {
    return new ImmutableGraph<>(connections);
  }

}
