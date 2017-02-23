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

import static com.github.rinde.rinsim.geom.Graphs.shortestPathEuclideanDistance;
import static com.github.rinde.rinsim.geom.Graphs.unmodifiableGraph;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.road.GraphRoadModelImpl.Loc;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.GeomHeuristic;
import com.github.rinde.rinsim.geom.ImmutableGraph;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

/**
 * A {@link RoadModel} that uses a {@link Graph} as road structure.
 * {@link RoadUser}s can only be added on nodes on the graph. This model assumes
 * that the {@link Graph} does <b>not</b> change. Modifying the graph after
 * passing it to the model may break this model, for support for modifications
 * take a look at {@link DynamicGraphRoadModelImpl}. The graph can define
 * {@link Connection} specific speed limits using {@link MultiAttributeData}.
 *
 * @author Rinde van Lon
 * @author Bartosz Michalik changes wrt. models infrastructure
 */
public class GraphRoadModelImpl extends AbstractRoadModel<Loc>
    implements GraphRoadModel {

  /**
   * Precision.
   */
  protected static final double DELTA = 0.000001;

  /**
   * The graph that is used as road structure.
   */
  protected final Graph<? extends ConnectionData> graph;

  /**
   * The snapshot of this model.
   */
  private final RoadModelSnapshot snapshot;

  /**
   * Creates a new instance using the specified {@link Graph} as road structure.
   * The default units are used as defined by {@link AbstractRoadModel}.
   * @param g The graph which will be used as road structure.
   * @param b The builder that contains the properties.
   */
  protected GraphRoadModelImpl(Graph<? extends ConnectionData> g,
      RoadModelBuilders.AbstractGraphRMB<?, ?, ?> b) {
    super(b.getDistanceUnit(), b.getSpeedUnit());
    graph = g;
    snapshot = GraphRoadModelSnapshot.create(
      ImmutableGraph.copyOf(graph), b.getDistanceUnit());
  }

  @Override
  public void addObjectAt(RoadUser newObj, Point pos) {
    checkArgument(graph.containsNode(pos),
      "Object must be initiated on a crossroad.");
    super.addObjectAt(newObj, asLoc(pos));
  }

  /**
   * Computes the distance that can be traveled on the connection between
   * <code>from</code> and <code>to</code> at the specified <code>speed</code>
   * and using the available <code>time</code>. This method can optionally be
   * overridden to change the move behavior of the model. The return value of
   * the method is interpreted in the following way:
   * <ul>
   * <li><code>if travelableDistance &lt; distance(from,to)</code> then there is
   * either:
   * <ul>
   * <li>not enough time left to travel the whole distance</li>
   * <li>another reason (e.g. an obstacle on the way) that prevents traveling
   * the whole distance</li>
   * </ul>
   * <li><code>if travelableDistance &ge; distance(from,to)</code> then it is
   * possible to travel the whole distance at once.</li>
   * </ul>
   * Note that <code>from</code> and <code>to</code> do not necessarily
   * correspond to the start and end points of a connection. However, it is
   * guaranteed that the two points are <i>on</i> the same connection.
   * @param from The start position for this travel.
   * @param to The destination position for this travel.
   * @param speed The travel speed.
   * @param timeLeft The time available for traveling.
   * @param timeUnit Unit in which <code>timeLeft</code> is expressed.
   * @return The distance that can be traveled, must be &ge; 0.
   */
  protected double computeTravelableDistance(Loc from, Point to, double speed,
      long timeLeft, Unit<Duration> timeUnit) {
    return speed * unitConversion.toInTime(timeLeft, timeUnit);
  }

  /**
   * Determines if the {@link #doFollowPath(MovingRoadUser, Queue, TimeLapse)}
   * method should continue moving towards the next hop in path. This method can
   * be overridden to add additional constraints.
   * @param curLoc The current location of the moving object.
   * @param path The path of the moving object.
   * @param time The time available for moving.
   * @return <code>true</code> if the object should keep moving,
   *         <code>false</code> otherwise.
   */
  protected boolean keepMoving(Loc curLoc, Queue<Point> path, TimeLapse time) {
    return time.hasTimeLeft() && !path.isEmpty();
  }

  @Override
  protected MoveProgress doFollowPath(MovingRoadUser object, Queue<Point> path,
      TimeLapse time) {
    final Loc objLoc = verifyLocation(objLocs.get(object));
    Loc tempLoc = objLoc;
    final MoveProgress.Builder mpBuilder = MoveProgress.builder(unitConversion,
      time);

    boolean cont = true;
    long timeLeft = time.getTimeLeft();
    while (timeLeft > 0 && !path.isEmpty() && cont) {
      checkMoveValidity(tempLoc, path.peek());
      // speed in internal speed unit
      final double speed = getMaxSpeed(object, tempLoc, path.peek());
      checkState(speed >= 0,
        "Found a bug in getMaxSpeed, return value must be >= 0, but is %s.",
        speed);
      // distance that can be traveled in current conn with timeleft
      final double travelableDistance = computeTravelableDistance(tempLoc,
        path.peek(), speed, timeLeft, time.getTimeUnit());
      checkState(
        travelableDistance >= 0d,
        "Found a bug in computeTravelableDistance, return value must be >= 0,"
          + " but is %s.",
        travelableDistance);
      final double connLength = unitConversion.toInDist(
        computeDistanceOnConnection(tempLoc, path.peek()));
      checkState(
        connLength >= 0d,
        "Found a bug in computeDistanceOnConnection, return value must be "
          + ">= 0, but is %s.",
        connLength);

      final double traveledDistance;
      if (travelableDistance >= connLength) {
        // jump to next node in path (this may be a node or a point on a
        // connection)
        tempLoc = verifyLocation(asLoc(path.remove()));
        traveledDistance = connLength;
        mpBuilder.addNode(tempLoc);
      } else {
        // travelableDistance < connLength
        cont = false;
        traveledDistance = travelableDistance;
        final Connection<?> conn = getConnection(tempLoc, path.peek());
        tempLoc = verifyLocation(newLoc(conn, tempLoc.relativePos
          + unitConversion.toExDist(travelableDistance)));
      }
      mpBuilder.addDistance(traveledDistance);
      final long timeSpent = DoubleMath.roundToLong(
        unitConversion.toExTime(traveledDistance / speed,
          time.getTimeUnit()),
        RoundingMode.HALF_DOWN);
      timeLeft -= timeSpent;
    }
    time.consume(time.getTimeLeft() - timeLeft);
    // update location and construct a MoveProgress object
    objLocs.put(object, tempLoc);
    return mpBuilder.build();
  }

  /**
   * Check if it is possible to move from <code>objLoc</code> to
   * <code>nextHop</code>.
   * @param objLoc The current location.
   * @param nextHop The destination node.
   * @throws IllegalArgumentException if it the proposed move is invalid.
   */
  protected void checkMoveValidity(Loc objLoc, Point nextHop) {
    // in case we start from an conn and our next destination is to go to
    // the end of the current conn then its ok. Otherwise more checks are
    // required..
    if (objLoc.isOnConnection() && !nextHop.equals(objLoc.conn.get().to())) {
      // check if next destination is a MidPoint
      checkArgument(
        nextHop instanceof Loc,
        "Illegal path for this object, from a position on a connection we can"
          + " not jump to another connection or go back. From %s, to %s.",
        objLoc, nextHop);
      final Loc dest = (Loc) nextHop;
      // check for same conn
      checkArgument(
        objLoc.isOnSameConnection(dest),
        "Illegal path for this object, first point is not on the same "
          + "connection as the object.");
      // check for relative position
      checkArgument(objLoc.relativePos <= dest.relativePos,
        "Illegal path for this object, can not move backward over an "
          + "connection.");
    } else if (!objLoc.isOnConnection() && !nextHop.equals(objLoc)
      && !graph.hasConnection(objLoc, nextHop)) {
      // in case we start from a node and we are not going to another node
      checkArgument(nextHop instanceof Loc,
        "Illegal path, first point should be directly connected to object "
          + "location.");
      final Loc dest = (Loc) nextHop;
      checkArgument(graph.hasConnection(objLoc, dest.conn.get().to()),
        "Illegal path, first point is on an connection not connected to "
          + "object location. ");
      checkArgument(objLoc.equals(dest.conn.get().from()),
        "Illegal path, first point is on a different connection.");
    }
  }

  /**
   * Compute distance between the two specified points, both of which need to be
   * on the same connection (or are equal). If points are equal the distance is
   * 0. This method uses length stored in {@link ConnectionData} objects when
   * available.
   * @param from Start of the connection.
   * @param to End of the connection.
   * @return the distance between two points, must be &ge; 0.
   * @throws IllegalArgumentException when two points are part of the graph but
   *           are not equal or there is no connection between them
   */
  protected double computeDistanceOnConnection(Point from, Point to) {
    if (from.equals(to)) {
      return 0;
    }
    final Connection<?> conn = getConnection(from, to);
    if (isOnConnection(from) && isOnConnection(to)) {
      return Math.abs(((Loc) from).relativePos - ((Loc) to).relativePos);
    } else if (isOnConnection(from)) {
      return conn.getLength() - ((Loc) from).relativePos;
    } else if (isOnConnection(to)) {
      return ((Loc) to).relativePos;
    }
    return conn.getLength();
  }

  /**
   * Checks if the point is on a connection.
   * @param p The point to check.
   * @return <code>true</code> if the point is on a connection,
   *         <code>false</code> otherwise.
   */
  protected static boolean isOnConnection(Point p) {
    return p instanceof Loc && ((Loc) p).isOnConnection();
  }

  /**
   * Checks whether the specified location is valid.
   * @param l The location to check.
   * @return The location if it is valid.
   * @throws VerifyException if the location is not valid.
   */
  protected Loc verifyLocation(Loc l) {
    verify(l.isOnConnection() || graph.containsNode(l),
      "Location points to non-existing node: %s.", l);
    verify(!l.isOnConnection()
      || graph.hasConnection(l.conn.get().from(), l.conn.get().to()),
      "Location points to non-existing connection: %s.", l.conn);
    return l;
  }

  /**
   * Compute speed of the object taking into account the speed limits of the
   * object.
   * @param object traveling object
   * @param from the point on the graph object is located
   * @param to the next point on the path it want to reach
   * @return The maximum speed in the internal unit, must be &ge; 0.
   */
  protected double getMaxSpeed(MovingRoadUser object, Point from, Point to) {
    final double objSpeed = unitConversion.toInSpeed(object.getSpeed());
    if (!from.equals(to)) {
      final Connection<?> conn = getConnection(from, to);
      if (conn.data().isPresent()
        && conn.data().get() instanceof MultiAttributeData) {
        final MultiAttributeData maed = (MultiAttributeData) conn.data()
          .get();

        if (maed.getMaxSpeed().isPresent()) {
          return Math.min(unitConversion.toInSpeed(maed.getMaxSpeed().get()),
            objSpeed);
        }
        return objSpeed;
      }
    }
    return objSpeed;
  }

  /**
   * Precondition: the specified {@link Point}s are part of a {@link Connection}
   * which exists in the {@link Graph}. This method figures out which
   * {@link Connection} the two {@link Point}s share.
   * @param from The start point.
   * @param to The end point.
   * @return The {@link Connection} shared by the points.
   */
  protected Connection<?> getConnection(Point from, Point to) {
    final boolean fromIsOnConn = isOnConnection(from);
    final boolean toIsOnConn = isOnConnection(to);
    final Connection<?> conn;
    final String errorMsg =
      "The specified points must be part of the same connection.";
    if (fromIsOnConn) {
      final Loc start = (Loc) from;
      if (toIsOnConn) {
        checkArgument(start.isOnSameConnection((Loc) to), errorMsg);
      } else {
        checkArgument(start.conn.get().to().equals(to), errorMsg);
      }
      conn = start.conn.get();

    } else if (toIsOnConn) {
      final Loc end = (Loc) to;
      checkArgument(end.conn.get().from().equals(from), errorMsg);
      conn = end.conn.get();
    } else {
      checkArgument(
        graph.hasConnection(from, to),
        "The specified points (%s and %s) must be part of an existing "
          + "connection in the graph.",
        from, to);
      conn = graph.getConnection(from, to);
    }
    return conn;
  }

  @Override
  public List<Point> getShortestPathTo(Point from, Point to) {
    final List<Point> path = new ArrayList<>();
    Point start = from;
    if (isOnConnection(from)) {
      start = asLoc(from).conn.get().to();
      path.add(from);
    }

    Point end = to;
    if (isOnConnection(to)) {
      end = asLoc(to).conn.get().from();
    }
    path.addAll(doGetShortestPathTo(start, end));
    if (isOnConnection(to)) {
      path.add(to);
    }
    return path;
  }

  /**
   * Uses the A* algorithm:
   * {@link com.github.rinde.rinsim.geom.Graphs#shortestPathEuclideanDistance}.
   * This method can optionally be overridden by subclasses to define another
   * shortest path algorithm.
   * @param from The start point of the path.
   * @param to The end point of the path.
   * @return The shortest path.
   */
  protected List<Point> doGetShortestPathTo(Point from, Point to) {
    return shortestPathEuclideanDistance(graph, from, to);
  }

  @Override
  public RoadPath getPathTo(Point from, Point to, Unit<Duration> timeUnit,
      Measure<Double, Velocity> speed, GeomHeuristic heuristic) {
    return snapshot.getPathTo(from, to, timeUnit, speed, heuristic);
  }

  @Override
  public Measure<Double, Length> getDistanceOfPath(Iterable<Point> path)
      throws IllegalArgumentException {
    return snapshot.getDistanceOfPath(path);
  }

  /**
   * @return An unmodifiable view on the graph.
   */
  @Override
  public Graph<? extends ConnectionData> getGraph() {
    return unmodifiableGraph(graph);
  }

  /**
   * Retrieves the connection which the specified {@link RoadUser} is at. If the
   * road user is at a node, {@link Optional#absent()} is returned instead.
   * @param obj The object which position is checked.
   * @return A {@link Connection} if <code>obj</code> is on one,
   *         {@link Optional#absent()} otherwise.
   */
  @Override
  public Optional<? extends Connection<?>> getConnection(RoadUser obj) {
    final Loc point = objLocs.get(obj);
    if (isOnConnection(point)) {
      return Optional.of(graph
        .getConnection(point.conn.get().from(), point.conn.get().to()));
    }
    return Optional.absent();
  }

  /**
   * Creates a new {@link Loc} based on the provided {@link Point}.
   * @param p The point used as input.
   * @return A {@link Loc} with identical position as the specified
   *         {@link Point}.
   */
  protected static Loc asLoc(Point p) {
    if (p instanceof Loc) {
      return (Loc) p;
    }
    return new Loc(p.x, p.y, null, -1, 0);
  }

  /**
   * Creates a new {@link Loc} based on the provided {@link Connection} and the
   * relative position. The new {@link Loc} will be placed on the connection
   * with a distance of <code>relativePos</code> to the start of the connection.
   * @param conn The {@link Connection} to use.
   * @param relativePos The relative position measured from the start of the
   *          {@link Connection}.
   * @return A new {@link Loc}
   */
  protected static Loc newLoc(Connection<? extends ConnectionData> conn,
      double relativePos) {
    final Point diff = Point.diff(conn.to(), conn.from());
    final double roadLength = conn.getLength();

    final double perc = relativePos / roadLength;
    if (perc + DELTA >= 1) {
      return new Loc(conn.to().x, conn.to().y, null, -1, 0);
    }
    return new Loc(conn.from().x + perc * diff.x,
      conn.from().y + perc * diff.y,
      conn, roadLength, relativePos);
  }

  @Override
  protected Point locObj2point(Loc locObj) {
    return locObj;
  }

  @Override
  protected Loc point2LocObj(Point point) {
    return asLoc(point);
  }

  @Override
  public Point getRandomPosition(RandomGenerator rnd) {
    return graph.getRandomNode(rnd);
  }

  @Override
  @Nonnull
  public <U> U get(Class<U> type) {
    return type.cast(self);
  }

  @Deprecated
  @Override
  public ImmutableList<Point> getBounds() {
    throw new UnsupportedOperationException("Not yet implemented.");
  }

  @Override
  public RoadModelSnapshot getSnapshot() {
    return snapshot;
  }

  /**
   * Location representation in a {@link Graph} for the
   * {@link GraphRoadModelImpl}.
   * @author Rinde van Lon
   */
  protected static final class Loc extends Point {
    private static final long serialVersionUID = 7070585967590832300L;
    /**
     * The length of the current connection.
     */
    public final double connLength;
    /**
     * The relative position of this instance compared to the start of the
     * connection.
     */
    public final double relativePos;
    /**
     * The {@link Connection} which this position is on if present.
     */
    public final Optional<? extends Connection<?>> conn;

    Loc(double pX, double pY,
        @Nullable Connection<? extends ConnectionData> pConn,
        double pConnLength, double pRelativePos) {
      super(pX, pY);
      connLength = pConnLength;
      relativePos = pRelativePos;
      conn = Optional.fromNullable(pConn);
    }

    /**
     * @return <code>true</code> if the position is on a connection.
     */
    public boolean isOnConnection() {
      return conn.isPresent();
    }

    /**
     * Check if this position is on the same connection as the provided
     * location.
     * @param l The location to compare with.
     * @return <code>true</code> if both {@link Loc}s are on the same
     *         connection, <code>false</code> otherwise.
     */
    public boolean isOnSameConnection(Loc l) {
      return conn.equals(l.conn);
    }
  }
}
