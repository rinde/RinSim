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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * Multimap-based implementation of a graph.
 * @author Rinde van Lon
 * @author Bartosz Michalik - added edge data + and dead end nodes
 * @param <E> The type of {@link ConnectionData} that is used in the edges.
 */
public class MultimapGraph<E extends ConnectionData> extends AbstractGraph<E> {

  private final Multimap<Point, Point> data;
  private final Map<Connection<E>, E> edgeData;
  private final Set<Point> deadEndNodes;

  /**
   * Create a new empty graph.
   */
  public MultimapGraph() {
    data = LinkedHashMultimap.create();
    this.edgeData = new HashMap<>();
    deadEndNodes = new LinkedHashSet<>();
  }

  /**
   * Instantiates a new graph using the specified multimap.
   * @param map The multimap that is copied into this new graph.
   */
  public MultimapGraph(Multimap<Point, Point> map) {
    this.data = LinkedHashMultimap.create(map);
    this.edgeData = new HashMap<>();
    this.deadEndNodes = new HashSet<>();
    deadEndNodes.addAll(data.values());
    deadEndNodes.removeAll(data.keySet());
  }

  @Override
  public boolean containsNode(Point node) {
    return data.containsKey(node) || deadEndNodes.contains(node);
  }

  @Override
  public Collection<Point> getOutgoingConnections(Point node) {
    return data.get(node);
  }

  @Override
  public boolean hasConnection(Point from, Point to) {
    return data.containsEntry(from, to);
  }

  @Override
  public int getNumberOfConnections() {
    return data.size();
  }

  @Override
  public int getNumberOfNodes() {
    return data.keySet().size() + deadEndNodes.size();
  }

  @Override
  public E setConnectionData(Point from, Point to, @Nullable E connData) {

    if (!hasConnection(from, to)) {
      throw new IllegalArgumentException("the connection " + from + " -> " + to
          + "does not exist");
    }
    return this.edgeData.put(new Connection<E>(from, to, null), connData);
  }

  @Nullable
  @Override
  public E connectionData(Point from, Point to) {
    return edgeData.get(new Connection<E>(from, to, null));
  }

  @Override
  public Set<Point> getNodes() {
    final Set<Point> nodes = new LinkedHashSet<>(data.keySet());
    nodes.addAll(deadEndNodes);
    return nodes;
  }

  @Override
  public List<Connection<E>> getConnections() {
    final List<Connection<E>> res = new ArrayList<>(
        edgeData.size());
    for (final Entry<Point, Point> p : data.entries()) {
      final Connection<E> connection = new Connection<>(p.getKey(),
          p.getValue(), null);
      final E eD = edgeData.get(connection);
      connection.setData(eD);
      res.add(connection);
    }
    return res;
  }

  /**
   * Returns an unmodifiable view on the {@link Multimap} which back this graph.
   * @return The view on the multimap.
   */
  public Multimap<Point, Point> getMultimap() {
    return Multimaps.unmodifiableMultimap(data);
  }

  @Override
  public boolean isEmpty() {
    return data.isEmpty();
  }

  /**
   * Warning: very inefficient! If this function is needed regularly it is
   * advised to use {@link TableGraph} instead. {@inheritDoc}
   */
  @Override
  public Collection<Point> getIncomingConnections(Point node) {
    final Set<Point> set = new LinkedHashSet<>();
    for (final Entry<Point, Point> entry : data.entries()) {
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
    data.remove(from, to);
    removeData(from, to);
    if (!data.containsKey(to)) {
      deadEndNodes.add(to);
    }
  }

  private void removeData(Point from, Point to) {
    edgeData.remove(new Connection<>(from, to, null));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(data, deadEndNodes, edgeData);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    return super.equals(o);
  }

  @Override
  protected void doAddConnection(Point from, Point to, @Nullable E connData) {
    data.put(from, to);
    deadEndNodes.remove(from);
    if (!data.containsKey(to)) {
      deadEndNodes.add(to);
    }
    if (connData != null) {
      this.edgeData.put(new Connection<E>(from, to, null), connData);
    }
  }
}
