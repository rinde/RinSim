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
package com.github.rinde.rinsim.core.model.road;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Objects;
import java.util.Queue;

import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.ListenableGraph.EventTypes;
import com.github.rinde.rinsim.geom.ListenableGraph.GraphEvent;
import com.github.rinde.rinsim.geom.Point;
import com.google.auto.value.AutoValue;
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
 * {@link #isOccupied(Point, Point)} can be of help for this.
 * @author Rinde van Lon
 */
public class DynamicGraphRoadModel extends GraphRoadModel {
  private final ListenableGraph<? extends ConnectionData> listenableGraph;
  final Multimap<Conn, RoadUser> connMap;
  final Multimap<Point, RoadUser> posMap;

  /**
   * Creates a new instance using the specified {@link ListenableGraph} as road
   * structure.
   * @param pGraph The graph which will be used as road structure.
   * @param distanceUnit The distance unit used in the graph.
   * @param speedUnit The speed unit for {@link MovingRoadUser}s in this model.
   */
  public DynamicGraphRoadModel(
      ListenableGraph<? extends ConnectionData> pGraph,
      Unit<Length> distanceUnit, Unit<Velocity> speedUnit) {
    super(pGraph, distanceUnit, speedUnit);
    listenableGraph = pGraph;
    listenableGraph.getEventAPI().addListener(
        new GraphModificationChecker(this));
    connMap = LinkedHashMultimap.create();
    posMap = LinkedHashMultimap.create();
  }

  /**
   * Creates a new instance using the specified {@link ListenableGraph} as road
   * structure. The default units are used as defined by
   * {@link AbstractRoadModel}.
   * @param pGraph The graph which will be used as road structure.
   */
  public DynamicGraphRoadModel(ListenableGraph<? extends ConnectionData> pGraph) {
    this(pGraph, SI.KILOMETER, NonSI.KILOMETERS_PER_HOUR);
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
      final Connection<? extends ConnectionData> conn = Objects
          .requireNonNull(loc.conn);
      connMap.put(Conn.create(conn.from, conn.to), newObj);
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
  public Graph<? extends ConnectionData> getGraph() {
    return listenableGraph;
  }

  @Override
  protected MoveProgress doFollowPath(MovingRoadUser object, Queue<Point> path,
      TimeLapse time) {
    final Loc prevLoc = objLocs.get(object);
    if (prevLoc.isOnConnection()) {
      final Connection<? extends ConnectionData> conn = Objects
          .requireNonNull(prevLoc.conn);
      connMap.remove(Conn.create(conn.from, conn.to), object);
    } else {
      posMap.remove(prevLoc, object);
    }
    final MoveProgress mp = super.doFollowPath(object, path, time);

    final Loc newLoc = objLocs.get(object);
    if (newLoc.isOnConnection()) {
      final Connection<? extends ConnectionData> conn = Objects
          .requireNonNull(newLoc.conn);
      connMap.put(Conn.create(conn.from, conn.to), object);
    } else {
      posMap.put(newLoc, object);
    }
    return mp;
  }

  /**
   * Checks if a connection between <code>from</code> and <code>to</code>
   * exists, if so it will check if it is occupied.
   * @param from The start point of a connection.
   * @param to The end point of a connection.
   * @return <code>true</code> if a {@link RoadUser} occupies either
   *         <code>from</code>, <code>to</code> or the connection from
   *         <code>from</code> to <code>to</code>, <code>false</code> otherwise.
   * @throws IllegalArgumentException if no connection exists between
   *           <code>from</code> and <code>to</code>.
   */
  public boolean isOccupied(Point from, Point to) {
    checkArgument(graph.hasConnection(from, to));
    return connMap.containsKey(Conn.create(from, to))
        || posMap.containsKey(from) || posMap.containsKey(to);
  }

  @Override
  public void removeObject(RoadUser object) {
    checkArgument(objLocs.containsKey(object),
        "RoadUser: %s does not exist.", object);
    final Loc prevLoc = objLocs.get(object);
    if (prevLoc.isOnConnection()) {
      final Connection<? extends ConnectionData> conn = Objects
          .requireNonNull(prevLoc.conn);
      connMap.remove(Conn.create(conn.from, conn.to), object);
    } else {
      posMap.remove(prevLoc, object);
    }
    super.removeObject(object);
  }

  private static class GraphModificationChecker implements Listener {
    private final DynamicGraphRoadModel model;

    GraphModificationChecker(DynamicGraphRoadModel pModel) {
      model = pModel;
    }

    @Override
    public void handleEvent(Event e) {
      final GraphEvent ge = (GraphEvent) e;
      if (ge.getEventType() == EventTypes.REMOVE_CONNECTION
          || ge.getEventType() == EventTypes.CHANGE_CONNECTION_DATA) {

        final Conn conn = Conn.create(ge.getFrom(), ge.getTo());
        checkState(
            !model.connMap.containsKey(conn),
            "A connection (%s->%s) with an object (%s) on it can not be changed or removed: %s.",
            ge.getFrom(), ge.getTo(), model.connMap.get(conn),
            ge.getEventType());

        if (model.posMap.containsKey(ge.getFrom())) {
          checkState(
              ge.getGraph().containsNode(ge.getFrom()),
              "There is an object on (%s) therefore the last connection to that location (%s->%s) can not be changed or removed: %s.",
              ge.getFrom(), ge.getFrom(), ge.getTo(), ge.getEventType());
        }
        if (model.posMap.containsKey(ge.getTo())) {
          checkState(
              ge.getGraph().containsNode(ge.getTo()),
              "There is an object on (%s) therefore the last connection to that location (%s->%s) can not be changed or removed: %s.",
              ge.getTo(), ge.getFrom(), ge.getTo(), ge.getEventType());
        }
      }
      // remove all previously computed shortest paths because they may have
      // been invalidated by the graph modification
      model.objDestinations.clear();
    }
  }

  @AutoValue
  static abstract class Conn {

    abstract Point from();

    abstract Point to();

    static Conn create(Point from, Point to) {
      return new AutoValue_DynamicGraphRoadModel_Conn(from, to);
    }
  }
}
