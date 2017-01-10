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
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.hash;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

/**
 * Table-based implementation of a graph. Since this graph is backed by a table
 * look ups for both incoming and outgoing connections from nodes is fast.
 * @author Rinde van Lon
 * @author Bartosz Michalik - change to the parametric version
 * @param <E> The type of {@link ConnectionData} that is used.
 */
public class TableGraph<E extends ConnectionData> extends AbstractGraph<E> {

  private final Table<Point, Point, Connection<E>> data;

  /**
   * Create a new empty graph.
   */
  public TableGraph() {
    data = Tables.newCustomTable(
      new LinkedHashMap<Point, Map<Point, Connection<E>>>(),
      new LinkedHashMapFactory<Connection<E>>());
  }

  @Override
  public Set<Point> getNodes() {
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
  public Collection<Point> getOutgoingConnections(Point node) {
    return data.row(node).keySet();
  }

  @Override
  public Collection<Point> getIncomingConnections(Point node) {
    return data.column(node).keySet();
  }

  @Override
  public void removeNode(Point node) {
    data.row(node).clear();
    data.column(node).clear();
  }

  @Override
  public void removeConnection(Point from, Point to) {
    if (hasConnection(from, to)) {
      data.remove(from, to);
    } else {
      throw new IllegalArgumentException(
        "Can not remove non-existing connection: " + from + " -> " + to);
    }
  }

  @Override
  public Set<Connection<E>> getConnections() {
    return ImmutableSet.copyOf(data.values());
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

  @Override
  protected void doAddConnection(Point from, Point to, Optional<E> connData) {
    data.put(from, to, Connection.create(from, to, connData));
  }

  @Override
  protected Optional<E> doChangeConnectionData(Point from, Point to,
      Optional<E> connData) {
    return verifyNotNull(
      data.put(from, to, Connection.create(from, to, connData))).data();
  }

  @Override
  public int hashCode() {
    return hash(data);
  }

  /**
   * Create a supplier for empty instances of {@link TableGraph}.
   * @param <E> The type of connection data.
   * @return A new supplier.
   */
  public static <E extends ConnectionData> Supplier<TableGraph<E>> supplier() {
    return new TableGraphSupplier<>();
  }

  private static class TableGraphSupplier<E extends ConnectionData>
      implements Supplier<TableGraph<E>> {

    TableGraphSupplier() {}

    @Override
    public TableGraph<E> get() {
      return new TableGraph<>();
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return other != null && other.getClass() == getClass();
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }
  }
}
