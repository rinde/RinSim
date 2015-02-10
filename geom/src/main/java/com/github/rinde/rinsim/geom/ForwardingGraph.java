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

import javax.annotation.Nullable;

import org.apache.commons.math3.random.RandomGenerator;

/**
 * {@link Graph} implementation that forwards all calls to another graph. This
 * is a helper class for creating graph decorators, subclasses only need to
 * override the methods need to be changed.
 * @author Rinde van Lon
 * @param <E> The type of {@link ConnectionData} that is used in the
 *          {@link Connection}s.
 */
public abstract class ForwardingGraph<E extends ConnectionData> implements
    Graph<E> {

  /**
   * The decorated graph.
   */
  protected final Graph<E> delegate;

  /**
   * Instantiates a new instance.
   * @param delegate The graph instance to decorate.
   */
  protected ForwardingGraph(Graph<E> delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean containsNode(Point node) {
    return delegate.containsNode(node);
  }

  @Override
  public Collection<Point> getOutgoingConnections(Point node) {
    return delegate.getOutgoingConnections(node);
  }

  @Override
  public Collection<Point> getIncomingConnections(Point node) {
    return delegate.getIncomingConnections(node);
  }

  @Override
  public boolean hasConnection(Point from, Point to) {
    return delegate.hasConnection(from, to);
  }

  @Override
  public Connection<E> getConnection(Point from, Point to) {
    return delegate.getConnection(from, to);
  }

  @Nullable
  @Override
  public E connectionData(Point from, Point to) {
    return delegate.connectionData(from, to);
  }

  @Override
  public double connectionLength(Point from, Point to) {
    return delegate.connectionLength(from, to);
  }

  @Override
  public int getNumberOfConnections() {
    return delegate.getNumberOfConnections();
  }

  @Override
  public List<Connection<E>> getConnections() {
    return delegate.getConnections();
  }

  @Override
  public int getNumberOfNodes() {
    return delegate.getNumberOfNodes();
  }

  @Override
  public Set<Point> getNodes() {
    return delegate.getNodes();
  }

  @Override
  public void addConnection(Point from, Point to, @Nullable E edgeData) {
    delegate.addConnection(from, to, edgeData);
  }

  @Override
  public void addConnection(Point from, Point to) {
    delegate.addConnection(from, to);
  }

  @Override
  public void addConnection(Connection<E> connection) {
    delegate.addConnection(connection);
  }

  @Nullable
  @Override
  public E setConnectionData(Point from, Point to, @Nullable E edgeData) {
    return delegate.setConnectionData(from, to, edgeData);
  }

  @Override
  public void addConnections(Iterable<? extends Connection<E>> connections) {
    delegate.addConnections(connections);
  }

  @Override
  public void merge(Graph<E> other) {
    delegate.merge(other);
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public void removeNode(Point node) {
    delegate.removeNode(node);
  }

  @Override
  public void removeConnection(Point from, Point to) {
    delegate.removeConnection(from, to);
  }

  @Override
  public Point getRandomNode(RandomGenerator generator) {
    return delegate.getRandomNode(generator);
  }
}
