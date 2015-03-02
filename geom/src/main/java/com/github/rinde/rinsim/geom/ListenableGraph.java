/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

/**
 * An observable decorator for {@link Graph} instances. This implementation
 * dispatches events for every graph modifying operation. Any modifications made
 * directly to the underlying graph are not observed, in order to get notified
 * of all modifications ensure that all modifications go exclusively via the
 * decorator instance. To help prevent unauthorized modifications this decorator
 * returns only unmodifiable {@link Connection} instances, the data of a
 * connection can be changed via
 * {@link #setConnectionData(Point, Point, ConnectionData)}. The list of
 * supported event types is {@link EventTypes} the event class is
 * {@link GraphEvent}.
 * 
 * @author Rinde van Lon
 * @param <E> The type of {@link ConnectionData} that is used in the edges.
 */
public final class ListenableGraph<E extends ConnectionData> extends
    ForwardingGraph<E> {

  /**
   * The event types dispatched by {@link ListenableGraph}.
   * @author Rinde van Lon
   */
  public enum EventTypes {
    /**
     * Event type that indicates that a connection is added to a graph.
     */
    ADD_CONNECTION,

    /**
     * Event type that indicates that the connection data of a connection in a
     * graph is changed.
     */
    CHANGE_CONNECTION_DATA,

    /**
     * Event type that indicates that a connection is removed from a graph.
     */
    REMOVE_CONNECTION,
  }

  private final EventDispatcher eventDispatcher;

  /**
   * Decorates the specified graph such that all modifications are monitored.
   * See {@link ListenableGraph} for more information.
   * @param delegate The graph to decorate.
   */
  public ListenableGraph(Graph<E> delegate) {
    super(delegate);
    eventDispatcher = new EventDispatcher(EventTypes.values());
  }

  /**
   * The {@link EventAPI} for this graph. Can be used to listen to graph
   * modifications.
   * @return The event api instance.
   */
  public EventAPI getEventAPI() {
    return eventDispatcher.getPublicEventAPI();
  }

  @Override
  public List<Connection<E>> getConnections() {
    return Collections.unmodifiableList(delegate.getConnections());
  }

  @Override
  public void addConnection(Point from, Point to, E connData) {
    delegate.addConnection(from, to, connData);
    eventDispatcher.dispatchEvent(new GraphEvent(EventTypes.ADD_CONNECTION,
        this, getConnection(from, to)));
  }

  @Override
  public void addConnection(Point from, Point to) {
    delegate.addConnection(from, to);
    eventDispatcher.dispatchEvent(new GraphEvent(EventTypes.ADD_CONNECTION,
        this, getConnection(from, to)));
  }

  @Override
  public void addConnection(Connection<E> connection) {
    delegate.addConnection(connection);
    eventDispatcher.dispatchEvent(new GraphEvent(EventTypes.ADD_CONNECTION,
        this, connection));
  }

  @Override
  public void addConnections(Iterable<? extends Connection<E>> connections) {
    for (Connection<E> c : connections) {
      addConnection(c);
    }
  }

  @Override
  public Optional<E> setConnectionData(Point from, Point to, E connectionData) {
    Optional<E> val = delegate.setConnectionData(from, to, connectionData);
    eventDispatcher.dispatchEvent(new GraphEvent(
        EventTypes.CHANGE_CONNECTION_DATA, this, getConnection(from, to)));
    return val;
  }

  @Override
  public Optional<E> removeConnectionData(Point from, Point to) {
    Optional<E> val = delegate.removeConnectionData(from, to);

    eventDispatcher.dispatchEvent(new GraphEvent(
        EventTypes.CHANGE_CONNECTION_DATA, this, getConnection(from, to)));
    return val;
  }

  @Override
  public void removeNode(Point node) {
    // collect data of removed connections but only if there is a listener
    List<Connection<?>> removedConnections = newArrayList();
    if (eventDispatcher.hasListenerFor(EventTypes.REMOVE_CONNECTION)) {
      for (Point p : delegate.getIncomingConnections(node)) {
        removedConnections.add(delegate.getConnection(p, node));
      }
      for (Point p : delegate.getOutgoingConnections(node)) {
        removedConnections.add(delegate.getConnection(node, p));
      }
    }
    delegate.removeNode(node);
    // notify listeners
    for (Connection<?> c : removedConnections) {
      eventDispatcher.dispatchEvent(new GraphEvent(
          EventTypes.REMOVE_CONNECTION, this, c));
    }
  }

  @Override
  public void removeConnection(Point from, Point to) {
    Connection<?> conn = delegate.getConnection(from, to);
    delegate.removeConnection(from, to);
    eventDispatcher
        .dispatchEvent(new GraphEvent(
            EventTypes.REMOVE_CONNECTION, this, conn));
  }

  @Override
  public void merge(Graph<E> other) {
    for (Connection<E> connection : other.getConnections()) {
      addConnection(connection);
    }
  }

  /**
   * Event indicating a change in a graph.
   * @author Rinde van Lon
   */
  public static final class GraphEvent extends Event {
    private final Connection<?> connection;

    GraphEvent(Enum<?> type, ListenableGraph<?> issuer, Connection<?> conn) {
      super(type, issuer);
      connection = conn;
    }

    /**
     * @return The connection that is subject of this event.
     */
    public Connection<?> getConnection() {
      return connection;
    }

    /**
     * @return The {@link Graph} that was changed.
     */
    public Graph<?> getGraph() {
      return (Graph<?>) getIssuer();
    }

    @Override
    public int hashCode() {
      return Objects.hash(eventType, getIssuer(), connection);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (null == other || other.getClass() != this.getClass()) {
        return false;
      }
      GraphEvent o = (GraphEvent) other;
      return Objects.equals(o.eventType, eventType) &&
          Objects.equals(o.getIssuer(), getIssuer()) &&
          Objects.equals(o.connection, connection);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper("GraphEvent")
          .add("type", this.eventType)
          .add("issuer", this.getIssuer())
          .add("connection", connection)
          .toString();
    }
  }
}
