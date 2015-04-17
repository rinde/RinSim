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
package com.github.rinde.rinsim.core.model.road;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;

import java.util.Queue;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.ListenableGraph.EventTypes;
import com.github.rinde.rinsim.geom.ListenableGraph.GraphEvent;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * {@link GraphRoadModel} that allows adding and removing connections and nodes
 * in the road graph. The {@link Graph} needs to be supplied as a
 * {@link ListenableGraph}. When the graph reports a change of its structure the
 * model will check whether the modification is allowed. There are two
 * situations in which a graph modification is not allowed:
 * <ul>
 * <li>Removal of a connection or changing a connection when a {@link RoadUser}
 * is on that connection.</li>
 * <li>Removal of the last connection to a node with a {@link RoadUser} on it.</li>
 * </ul>
 * An {@link IllegalStateException} will be thrown upon detection of an invalid
 * modification. It is up to the user to prevent this from happening. The method
 * {@link #hasRoadUserOn(Point, Point)} can be of help for this. Instances can
 * be obtained via {@link #builderDynamic(ListenableGraph)}.
 * @author Rinde van Lon
 */
public class DynamicGraphRoadModel extends GraphRoadModel {
  final Multimap<Connection<?>, RoadUser> connMap;
  final Multimap<Point, RoadUser> posMap;

  /**
   * Creates a new instance.
   * @param g The graph to use.
   * @param b The builder that contains the properties to initialize this model.
   */
  protected DynamicGraphRoadModel(ListenableGraph<?> g, AbstractBuilder<?, ?> b) {
    super(g, b);
    getGraph().getEventAPI().addListener(
      new GraphModificationChecker(this));
    connMap = LinkedHashMultimap.create();
    posMap = LinkedHashMultimap.create();
  }

  @Override
  public void addObjectAt(RoadUser newObj, Point pos) {
    posMap.put(pos, newObj);
    super.addObjectAt(newObj, pos);
  }

  @Override
  public void addObjectAtSamePosition(RoadUser newObj, RoadUser existingObj) {
    super.addObjectAtSamePosition(newObj, existingObj);
    final Loc loc = objLocs.get(newObj);
    if (loc.isOnConnection()) {
      final Connection<? extends ConnectionData> conn = loc.conn.get();
      connMap.put(conn, newObj);
    } else {
      posMap.put(loc, newObj);
    }
  }

  @Override
  public void clear() {
    super.clear();
    connMap.clear();
    posMap.clear();
  }

  /**
   * @return A reference to the graph.
   */
  @Override
  public ListenableGraph<? extends ConnectionData> getGraph() {
    return (ListenableGraph<? extends ConnectionData>) graph;
  }

  @Override
  protected MoveProgress doFollowPath(MovingRoadUser object, Queue<Point> path,
    TimeLapse time) {
    final Loc prevLoc = objLocs.get(object);
    if (prevLoc.isOnConnection()) {
      connMap.remove(prevLoc.conn.get(), object);
    } else {
      posMap.remove(prevLoc, object);
    }
    final MoveProgress mp;
    try {
      mp = super.doFollowPath(object, path, time);
    } catch (final IllegalArgumentException e) {
      throw e;
    } finally {
      final Loc newLoc = objLocs.get(object);
      if (newLoc.isOnConnection()) {
        connMap.put(newLoc.conn.get(), object);
      } else {
        posMap.put(newLoc, object);
      }
    }
    return mp;
  }

  /**
   * Checks whether there is a {@link RoadUser} on the connection between
   * <code>from</code> and <code>to</code>.
   * @param from The start point of a connection.
   * @param to The end point of a connection.
   * @return <code>true</code> if a {@link RoadUser} occupies either
   *         <code>from</code>, <code>to</code> or the connection between
   *         <code>from</code> and <code>to</code>, <code>false</code>
   *         otherwise.
   * @throws IllegalArgumentException if no connection exists between
   *           <code>from</code> and <code>to</code>.
   */
  public boolean hasRoadUserOn(Point from, Point to) {
    checkArgument(graph.hasConnection(from, to),
      "There is no connection between %s and %s.", from, to);
    return connMap.containsKey(graph.getConnection(from, to))
      || posMap.containsKey(from) || posMap.containsKey(to);
  }

  @Override
  public void removeObject(RoadUser object) {
    checkArgument(objLocs.containsKey(object),
      "RoadUser: %s does not exist.", object);
    final Loc prevLoc = objLocs.get(object);
    if (prevLoc.isOnConnection()) {
      final Connection<? extends ConnectionData> conn = prevLoc.conn.get();
      connMap.remove(conn, object);
    } else {
      posMap.remove(prevLoc, object);
    }
    super.removeObject(object);
  }

  /**
   * Create a {@link Builder} for constructing {@link DynamicGraphRoadModel}
   * instances.
   * @param g A {@link ListenableGraph}.
   * @return A new {@link Builder} instance.
   */
  public static Builder builderDynamic(ListenableGraph<?> g) {
    return new Builder(Suppliers.ofInstance(g));
  }

  /**
   * Create a {@link Builder} for constructing {@link DynamicGraphRoadModel}
   * instances.
   * @param g A supplier of {@link ListenableGraph}.
   * @return A new {@link Builder} instance.
   */
  public static Builder builderDynamic(Supplier<? extends ListenableGraph<?>> g) {
    return new Builder(g);
  }

  /**
   * A builder for constructing {@link DynamicGraphRoadModel} instances. Use
   * {@link DynamicGraphRoadModel#builderDynamic(ListenableGraph)} for obtaining
   * builder instances.
   * @author Rinde van Lon
   */
  public static class Builder extends
    AbstractBuilder<DynamicGraphRoadModel, Builder> {

    Builder(Supplier<? extends ListenableGraph<?>> g) {
      super(g);
    }

    @Override
    public DynamicGraphRoadModel build(DependencyProvider dependencyProvider) {
      return new DynamicGraphRoadModel(getGraph(), this);
    }

    @Override
    protected Builder self() {
      return this;
    }
  }

  /**
   * Abstract builder for constructing subclasses of
   * {@link DynamicGraphRoadModel}.
   * @param <T> The type of the model that the builder is constructing.
   * @param <S> The builder type itself, necessary to make a inheritance-based
   *          builder.
   * @author Rinde van Lon
   */
  public static abstract class AbstractBuilder<T extends DynamicGraphRoadModel, S>
    extends GraphRoadModel.AbstractBuilder<T, S> {

    /**
     * Create a new instance.
     * @param supplier Supplier of the graph that will be used as road
     *          structure.
     */
    protected AbstractBuilder(Supplier<? extends ListenableGraph<?>> supplier) {
      super(supplier);
    }

    @Override
    public ListenableGraph<?> getGraph() {
      return (ListenableGraph<?>) super.getGraph();
    }
  }

  private static class GraphModificationChecker implements Listener {
    private final DynamicGraphRoadModel model;

    GraphModificationChecker(DynamicGraphRoadModel pModel) {
      model = pModel;
    }

    @Override
    public void handleEvent(Event e) {
      verify(e instanceof GraphEvent);
      final GraphEvent ge = (GraphEvent) e;
      if (ge.getEventType() == EventTypes.REMOVE_CONNECTION
        || ge.getEventType() == EventTypes.CHANGE_CONNECTION_DATA) {

        final Connection<?> conn = ge.getConnection();
        checkState(
          !model.connMap.containsKey(conn),
          "A connection (%s->%s) with an object (%s) on it can not be changed or removed: %s.",
          conn.from(), conn.to(), model.connMap.get(conn), ge.getEventType());

        if (model.posMap.containsKey(conn.from())) {
          checkState(
            ge.getGraph().containsNode(conn.from()),
            "There is an object on (%s) therefore the last connection to that location (%s->%s) can not be changed or removed: %s.",
            conn.from(), conn.from(), conn.to(), ge.getEventType());
        }
        if (model.posMap.containsKey(conn.to())) {
          checkState(
            ge.getGraph().containsNode(conn.to()),
            "There is an object on (%s) therefore the last connection to that location (%s->%s) can not be changed or removed: %s.",
            conn.to(), conn.from(), conn.to(), ge.getEventType());
        }
      }
      // remove all previously computed shortest paths because they may have
      // been invalidated by the graph modification
      model.objDestinations.clear();
    }
  }
}
