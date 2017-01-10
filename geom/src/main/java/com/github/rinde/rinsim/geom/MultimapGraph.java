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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

/**
 * Multimap-based implementation of a graph.
 * @author Rinde van Lon
 * @author Bartosz Michalik - added connection data + and dead end nodes
 * @param <E> The type of {@link ConnectionData} that is used.
 */
public class MultimapGraph<E extends ConnectionData> extends AbstractGraph<E> {
  private final Multimap<Point, Point> multimap;
  private final Table<Point, Point, Connection<E>> lazyConnectionTable;
  private final Set<Point> deadEndNodes;

  /**
   * Create a new empty graph.
   */
  public MultimapGraph() {
    this(LinkedHashMultimap.<Point, Point>create());
  }

  /**
   * Instantiates a new graph using the specified multimap.
   * @param map The multimap that is copied into this new graph.
   */
  public MultimapGraph(Multimap<Point, Point> map) {
    multimap = LinkedHashMultimap.create(map);
    lazyConnectionTable = Tables.newCustomTable(
      new LinkedHashMap<Point, Map<Point, Connection<E>>>(),
      new LinkedHashMapFactory<Connection<E>>());
    deadEndNodes = new HashSet<>();
    deadEndNodes.addAll(multimap.values());
    deadEndNodes.removeAll(multimap.keySet());
  }

  @Override
  public boolean containsNode(Point node) {
    return multimap.containsKey(node) || deadEndNodes.contains(node);
  }

  @Override
  public Collection<Point> getOutgoingConnections(Point node) {
    return multimap.get(node);
  }

  @Override
  public boolean hasConnection(Point from, Point to) {
    return multimap.containsEntry(from, to);
  }

  @Override
  public <T extends ConnectionData> boolean hasConnection(
      Connection<T> connection) {
    if (connection.data().isPresent()) {
      return getConnection(connection.from(), connection.to()).equals(
        connection);
    }
    return hasConnection(connection.from(), connection.to());
  }

  @Override
  public int getNumberOfConnections() {
    return multimap.size();
  }

  @Override
  public int getNumberOfNodes() {
    return multimap.keySet().size() + deadEndNodes.size();
  }

  @Override
  protected Optional<E> doChangeConnectionData(Point from, Point to,
      Optional<E> connData) {
    Optional<E> dat;
    if (lazyConnectionTable.contains(from, to)) {
      dat = lazyConnectionTable.get(from, to).data();
    } else {
      dat = Optional.absent();
    }
    lazyConnectionTable.put(from, to, Connection.create(from, to, connData));
    return dat;
  }

  @Override
  public Optional<E> connectionData(Point from, Point to) {
    if (lazyConnectionTable.contains(from, to)) {
      return lazyConnectionTable.get(from, to).data();
    }
    return Optional.absent();
  }

  @Override
  public Set<Point> getNodes() {
    final Set<Point> nodes = new LinkedHashSet<>(multimap.keySet());
    nodes.addAll(deadEndNodes);
    return nodes;
  }

  @Override
  public Connection<E> getConnection(Point from, Point to) {
    checkArgument(hasConnection(from, to), "%s -> %s is not a connection.",
      from, to);
    if (!lazyConnectionTable.contains(from, to)) {
      lazyConnectionTable.put(from, to, Connection.<E>create(from, to));
    }
    return lazyConnectionTable.get(from, to);
  }

  @Override
  public Set<Connection<E>> getConnections() {
    for (final Entry<Point, Point> p : multimap.entries()) {
      getConnection(p.getKey(), p.getValue());
    }
    return ImmutableSet.copyOf(lazyConnectionTable.values());
  }

  /**
   * Returns an unmodifiable view on the {@link Multimap} which back this graph.
   * @return The view on the multimap.
   */
  public Multimap<Point, Point> getMultimap() {
    return Multimaps.unmodifiableMultimap(multimap);
  }

  @Override
  public boolean isEmpty() {
    return multimap.isEmpty();
  }

  /**
   * Warning: very inefficient! If this function is needed regularly it is
   * advised to use {@link TableGraph} instead. {@inheritDoc}
   */
  @Override
  public Collection<Point> getIncomingConnections(Point node) {
    final Set<Point> set = new LinkedHashSet<>();
    for (final Entry<Point, Point> entry : multimap.entries()) {
      if (entry.getValue().equals(node)) {
        set.add(entry.getKey());
      }
    }
    return set;
  }

  /**
   * Warning: very inefficient! If this function is needed regularly it is
   * advised to use {@link TableGraph} instead.
   * @param node The node to remove.
   */
  @Override
  public void removeNode(Point node) {
    // copy data first to avoid concurrent modification exceptions
    final List<Point> out = new ArrayList<>();
    out.addAll(getOutgoingConnections(node));
    for (final Point p : out) {
      removeConnection(node, p);
    }
    final List<Point> in = new ArrayList<>();
    in.addAll(getIncomingConnections(node));
    for (final Point p : in) {
      removeConnection(p, node);
    }
    deadEndNodes.remove(node);
  }

  @Override
  public void removeConnection(Point from, Point to) {
    checkArgument(hasConnection(from, to),
      "Can not remove non-existing connection: %s -> %s", from, to);
    multimap.remove(from, to);
    removeData(from, to);
    if (!multimap.containsKey(to)) {
      deadEndNodes.add(to);
    }
  }

  private void removeData(Point from, Point to) {
    lazyConnectionTable.remove(from, to);
  }

  @Override
  public int hashCode() {
    return hash(multimap, deadEndNodes, lazyConnectionTable);
  }

  @Override
  protected void doAddConnection(Point from, Point to, Optional<E> connData) {
    multimap.put(from, to);
    deadEndNodes.remove(from);
    if (!multimap.containsKey(to)) {
      deadEndNodes.add(to);
    }
    if (connData.isPresent()) {
      this.lazyConnectionTable.put(from, to,
        Connection.create(from, to, connData));
    }
  }

  /**
   * Create a supplier for empty instances of {@link MultimapGraph}.
   * @param <E> The type of connection data.
   * @return A new supplier.
   */
  public static <E extends ConnectionData> Supplier<MultimapGraph<E>> supplier() {
    return new MultimapGraphSupplier<>();
  }

  /**
   * Instantiates a new graph supplier that will create {@link MultimapGraph}
   * instances using the specified data.
   * @param data The multimap that is copied into this new graph.
   * @param <E> The type of connection data.
   * @return A new supplier.
   */
  public static <E extends ConnectionData> Supplier<MultimapGraph<E>> supplier(
      ImmutableMultimap<Point, Point> data) {
    return new MultimapGraphSupplier<>(data);
  }

  private static class MultimapGraphSupplier<E extends ConnectionData>
      implements Supplier<MultimapGraph<E>> {

    private final ImmutableMultimap<Point, Point> data;

    MultimapGraphSupplier() {
      this(ImmutableMultimap.<Point, Point>of());
    }

    MultimapGraphSupplier(ImmutableMultimap<Point, Point> d) {
      data = d;
    }

    @Override
    public MultimapGraph<E> get() {
      return new MultimapGraph<>(data);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (other == null || other.getClass() != getClass()) {
        return false;
      }
      return Objects.equals(data, ((MultimapGraphSupplier<?>) other).data);
    }

    @Override
    public int hashCode() {
      return hash(data);
    }
  }
}
