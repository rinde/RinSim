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
import static com.google.common.collect.Maps.newLinkedHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Supplier;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.Tables;

/**
 * Table-based implementation of a graph. Since this graph is backed by a table
 * look ups for both incoming and outgoing connections from nodes is fast.
 * @author Rinde van Lon 
 * @author Bartosz Michalik  - change to the
 *         parametric version
 * @param <E> The type of {@link ConnectionData} that is used in the edges.
 */
public class TableGraph<E extends ConnectionData> extends AbstractGraph<E> {

  private final Table<Point, Point, E> data;
  private final E empty;

  /**
   * Create a new empty graph.
   * @param emptyValue A special connection data instance that is used as the
   *          'empty' instance.
   */
  public TableGraph(E emptyValue) {
    data = Tables.newCustomTable(new LinkedHashMap<Point, Map<Point, E>>(),
        new Factory<E>());
    empty = emptyValue;
  }

  @Override
  public Set<Point> getNodes() {
    final Set<Point> nodes = new LinkedHashSet<Point>(data.rowKeySet());
    nodes.addAll(data.columnKeySet());
    return Collections.unmodifiableSet(nodes);
  }

  @Override
  public boolean hasConnection(Point from, Point to) {
    return data.contains(from, to);
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
  public List<Connection<E>> getConnections() {
    final List<Connection<E>> connections = new ArrayList<Connection<E>>();
    for (final Cell<Point, Point, E> cell : data.cellSet()) {
      if (empty.equals(cell.getValue())) {
        connections.add(new Connection<E>(cell.getRowKey(),
            cell.getColumnKey(), null));
      } else {
        connections.add(new Connection<E>(cell.getRowKey(),
            cell.getColumnKey(), cell.getValue()));
      }
    }
    return connections;
  }

  @Override
  public boolean isEmpty() {
    return data.isEmpty();
  }

  @Override
  protected boolean isEmptyConnectionData(E connData) {
    return super.isEmptyConnectionData(connData) || empty.equals(connData);
  }

  @Override
  public Connection<E> getConnection(Point from, Point to) {
    checkArgument(hasConnection(from, to), "%s -> %s is not a connection",
        from, to);
    return new Connection<E>(from, to, connectionData(from, to));
  }

  @Nullable
  @Override
  public E connectionData(Point from, Point to) {
    final E e = data.get(from, to);
    if (empty.equals(e)) {
      return null;
    }
    return e;
  }

  @Override
  protected void doAddConnection(Point from, Point to, @Nullable E edgeData) {
    if (edgeData == null) {
      data.put(from, to, empty);
    } else {
      data.put(from, to, edgeData);
    }
  }

  @Nullable
  @Override
  public E setConnectionData(Point from, Point to, @Nullable E edgeData) {
    if (hasConnection(from, to)) {
      E e;
      if (edgeData == null) {
        e = data.put(from, to, empty);
      } else {
        e = data.put(from, to, edgeData);
      }

      if (empty.equals(e)) {
        return null;
      }
      return e;
    }
    throw new IllegalArgumentException(
        "Can not get connection length from a non-existing connection.");
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(data, empty);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    return super.equals(o);
  }

  private static final class Factory<E> implements Supplier<Map<Point, E>> {

    Factory() {}

    @Override
    public Map<Point, E> get() {
      return newLinkedHashMap();
    }
  }

}
