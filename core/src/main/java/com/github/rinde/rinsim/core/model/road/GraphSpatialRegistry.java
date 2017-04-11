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
package com.github.rinde.rinsim.core.model.road;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Point;
import com.google.auto.value.AutoValue;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

// adapter that includes graph specific info
public class GraphSpatialRegistry extends ForwardingSpatialRegistry {
  // contains map: RoadUser -> Point
  final SpatialRegistry delegate;
  final Map<RoadUser, ConnLoc> connLocMap;
  final SetMultimap<Point, RoadUser> posMap;
  final SetMultimap<Connection<?>, RoadUser> connMap;

  GraphSpatialRegistry(SpatialRegistry deleg) {
    delegate = deleg;
    connMap = LinkedHashMultimap.create();
    posMap = LinkedHashMultimap.create();
    connLocMap = new LinkedHashMap<>();
  }

  @Override
  SpatialRegistry delegate() {
    return delegate;
  }

  @Override
  public void addAt(RoadUser object, Point position) {
    addAt(object, position, null);
  }

  public Point addAt(RoadUser object, Connection<?> conn, double relPos,
      double precision) {
    final Point diff = Point.diff(conn.to(), conn.from());
    final double perc = relPos / conn.getLength();
    final Point pos;
    final ConnLoc connLoc;
    if (perc + precision >= 1) {
      connLoc = null;
      pos = conn.to();
    } else {
      pos = new Point(
        conn.from().x + perc * diff.x,
        conn.from().y + perc * diff.y);
      connLoc = ConnLoc.create(pos, conn, relPos);
    }
    addAt(object, pos, connLoc);
    return pos;
  }

  void addAt(RoadUser ru, Point position, @Nullable ConnLoc connLoc) {
    @Nullable
    ConnLoc cl = connLoc;
    // if no ConnLoc is provided but the position is known to be on a
    // connection, we can use that conn instead.
    if (cl == null && isOnConnection(position)) {
      cl = connLocMap.get(posMap.get(position).iterator().next());
    }

    // remove from old position
    if (containsObject(ru)) {
      removeObject(ru);
    }

    delegate().addAt(ru, position);
    posMap.put(position, ru);
    if (cl != null) {
      connMap.put(cl.connection(), ru);
      connLocMap.put(ru, cl);
    }
  }

  @Override
  public void removeObject(RoadUser object) {
    final Point pos = getPosition(object);
    delegate.removeObject(object);
    posMap.remove(pos, object);

    if (connLocMap.containsKey(object)) {
      final ConnLoc connLoc = connLocMap.get(object);
      connMap.remove(connLoc.connection(), object);
      connLocMap.remove(object);
    }
  }

  @Override
  public void clear() {
    super.clear();
    connMap.clear();
    posMap.clear();
  }

  // returns true if it is known that point p is on a connection. this can only
  // the case if a roaduser resides at that location
  public boolean isOnConnection(Point p) {
    return posMap.containsKey(p)
      && isOnConnection(posMap.get(p).iterator().next());
  }

  public boolean isOnConnection(RoadUser ru) {
    return connLocMap.containsKey(ru);
  }

  public Connection<?> getConnection(Point p) {
    return getConnection(posMap.get(p).iterator().next());
  }

  public Connection<?> getConnection(RoadUser ru) {
    return connLocMap.get(ru).connection();
  }

  public double getRelativePosition(Point p) {
    if (!posMap.containsKey(p)) {
      return 0d;
    }
    return getRelativePosition(posMap.get(p).iterator().next());
  }

  public double getRelativePosition(RoadUser ru) {
    if (!connLocMap.containsKey(ru)) {
      return 0d;
    }
    return connLocMap.get(ru).relativePosition();
  }

  // excluding from/to
  public boolean hasRoadUserOn(Connection<?> conn) {
    return connMap.containsKey(conn);
  }

  public boolean hasRoadUserOn(Point from, Point to) {
    return connMap.containsEntry(from, to);
  }

  public boolean hasRoadUserOn(Point pos) {
    return posMap.containsKey(pos);
  }

  public Set<RoadUser> getRoadUsersOn(Connection<?> conn) {
    return Collections.unmodifiableSet(connMap.get(conn));
  }

  public Set<RoadUser> getRoadUsersOn(Point pos) {
    return Collections.unmodifiableSet(posMap.get(pos));
  }

  @AutoValue
  abstract static class ConnLoc {

    abstract Point position();

    abstract Connection<?> connection();

    abstract double relativePosition();

    /**
     * Check if this position is on the same connection as the provided
     * location.
     * @param l The location to compare with.
     * @return <code>true</code> if both {@link ConnLoc}s are on the same
     *         connection, <code>false</code> otherwise.
     */
    boolean isOnSameConnection(ConnLoc l) {
      return connection().equals(l.connection());
    }

    static ConnLoc create(Point p, Connection<?> c, double relPos) {
      return new AutoValue_GraphSpatialRegistry_ConnLoc(p, c, relPos);
    }
  }

}
