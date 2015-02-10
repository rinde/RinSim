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

import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.google.common.base.MoreObjects;

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
  public Connection<E> getConnection(Point from, Point to) {
    return Graphs.unmodifiableConnection(delegate.getConnection(from, to));
  }

  @Override
  public List<Connection<E>> getConnections() {
    List<Connection<E>> unmod = new ArrayList<>();
    for (Connection<E> c : delegate.getConnections()) {
      unmod.add(Graphs.unmodifiableConnection(c));
    }
    return Collections.unmodifiableList(unmod);
  }

  @Override
  public void addConnection(Point from, Point to, @Nullable E edgeData) {
    delegate.addConnection(from, to, edgeData);
    eventDispatcher.dispatchEvent(new GraphEvent(EventTypes.ADD_CONNECTION,
        this, from, to, edgeData));
  }

  @Override
  public void addConnection(Point from, Point to) {
    delegate.addConnection(from, to);
    eventDispatcher.dispatchEvent(new GraphEvent(EventTypes.ADD_CONNECTION,
        this, from, to, null));
  }

  @Override
  public void addConnection(Connection<E> connection) {
    delegate.addConnection(connection);
    eventDispatcher.dispatchEvent(new GraphEvent(EventTypes.ADD_CONNECTION,
        this, connection.from, connection.to, connection.getData()));
  }

  @Override
  public void addConnections(Iterable<? extends Connection<E>> connections) {
    for (Connection<E> c : connections) {
      addConnection(c);
    }
  }

  @Deprecated
  @Override
  public void addConnections(Collection<? extends Connection<E>> connections) {
    addConnections((Iterable<? extends Connection<E>>) connections);
  }

  @Override
  @Nullable
  public E setConnectionData(Point from, Point to, @Nullable E connectionData) {
    E val = delegate.setConnectionData(from, to, connectionData);
    eventDispatcher.dispatchEvent(new GraphEvent(
        EventTypes.CHANGE_CONNECTION_DATA, this, from, to, connectionData));
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
          EventTypes.REMOVE_CONNECTION, this, c.from, c.to, c.getData()));
    }
  }

  @Override
  public void removeConnection(Point from, Point to) {
    Connection<?> conn = delegate.getConnection(from, to);
    delegate.removeConnection(from, to);
    eventDispatcher
        .dispatchEvent(new GraphEvent(
            EventTypes.REMOVE_CONNECTION, this, conn.from, conn.to, conn
                .getData()));
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
    private final Point from;
    private final Point to;
    private final @Nullable Object connData;

    GraphEvent(Enum<?> type, ListenableGraph<?> issuer, Point from,
        Point to, @Nullable Object connData) {
      super(type, issuer);
      this.from = from;
      this.to = to;
      this.connData = connData;
    }

    /**
     * @return The start point of the connection that is changed.
     */
    public Point getFrom() {
      return from;
    }

    /**
     * @return The end point of the connection that is changed.
     */
    public Point getTo() {
      return to;
    }

    /**
     * @return The data of the connection that is changed.
     */
    public @Nullable Object getConnData() {
      return connData;
    }

    /**
     * @return The {@link Graph} that was changed.
     */
    public Graph<?> getGraph() {
      return (Graph<?>) getIssuer();
    }

    @Override
    public int hashCode() {
      return Objects.hash(eventType, getIssuer(), from, to, connData);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (null == other || other.getClass() != this.getClass()) {
        return false;
      }
      GraphEvent o = (GraphEvent) other;
      return Objects.equals(o.eventType, eventType) &&
          Objects.equals(o.getIssuer(), getIssuer()) &&
          Objects.equals(o.from, from) &&
          Objects.equals(o.to, to) &&
          Objects.equals(o.connData, connData);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper("GraphEvent")
          .add("type", this.eventType)
          .add("issuer", this.getIssuer())
          .add("from", from)
          .add("to", to)
          .add("connData", connData)
          .toString();
    }
  }
}
