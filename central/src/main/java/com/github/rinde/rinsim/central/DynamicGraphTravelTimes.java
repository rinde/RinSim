/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.central;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class DynamicGraphTravelTimes<T extends ConnectionData>
    extends AbstractTravelTimes {
  /**
   * Immutable graph
   */
  private final Graph<T> g;
  final Measure<Double, Velocity> vehicleSpeed;
  final Unit<Velocity> speedUnit;

  private final Table<Point, Point, List<Point>> pathTable;
  private final Map<List<Point>, Long> pathTT;

  DynamicGraphTravelTimes(Graph<T> g, Unit<Duration> tu, Unit<Length> du,
      Unit<Velocity> su,
      Iterable<? extends Vehicle> vehicles) {
    super(tu, du);

    pathTable = HashBasedTable.create();
    pathTT = new HashMap<>();

    this.g = g;

    double max = 0;
    for (final Vehicle ave : vehicles) {
      max = Math.max(max, ave.getSpeed());
    }
    vehicleSpeed = Measure.valueOf(max, su);
    speedUnit = su;
  }

  DynamicGraphTravelTimes(DynamicGraphTravelTimes<T> tt, Graph<T> newGraph) {
    super(tt);

    // TODO Check for updates
    pathTable = tt.pathTable;
    pathTT = tt.pathTT;

    this.g = Graphs.unmodifiableGraph(newGraph);
    vehicleSpeed = tt.vehicleSpeed;
    speedUnit = tt.speedUnit;
  }

  @Override
  public long getTheoreticalShortestTravelTime(Point from, Point to) {

    List<Point> path;

    if (pathTable.contains(from, to)) {
      path = pathTable.get(from, to);
    } else {
      path = Graphs
        .shortestPath(g, from, to, Graphs.GraphHeuristics.THEORETICAL_TIME);
      pathTable.put(from, to, path);
    }
    if (pathTT.containsKey(path)) {
      return pathTT.get(path);
    }

    final Iterator<Point> pathI = path.iterator();

    long travelTime = 0L;
    Point prev = pathI.next();
    while (pathI.hasNext()) {
      final Point cur = pathI.next();
      final Connection<T> conn =
        g.getConnection(prev, cur);

      final Measure<Double, Length> distance = Measure.valueOf(
        conn.getLength(), distanceUnit);
      try {
        travelTime +=
          Math.min(RoadModels.computeTravelTime(vehicleSpeed, distance,
            timeUnit),
            RoadModels.computeTravelTime(
              Measure.valueOf(
                Double
                  .parseDouble((String) ((MultiAttributeData) conn.data().get())
                    .getAttributes().get("ts")),
                speedUnit),
              distance, timeUnit));
      } catch (final Exception e) {
        e.printStackTrace();
        // travelTime +=
        // RoadModels.computeTravelTime(vehicleSpeed, distance,
        // timeUnit);
      }
      prev = cur;
    }
    pathTT.put(path, travelTime);
    return travelTime;
    // TT := millis
    // conn.length := meter
    // speed := kmh
  }

  @Override
  public long getCurrentShortestTravelTime(Point from, Point to) {
    final Iterator<Point> path =
      Graphs.shortestPath(g, from, to, Graphs.GraphHeuristics.TIME)
        .iterator();

    long travelTime = 0L;
    Point prev = path.next();
    while (path.hasNext()) {
      final Point cur = path.next();
      @SuppressWarnings("unchecked")
      final Connection<T> conn =
        g.getConnection(prev, cur);

      final Measure<Double, Length> distance = Measure.valueOf(
        conn.getLength(), distanceUnit);
      try {
        travelTime +=
          Math.min(RoadModels.computeTravelTime(vehicleSpeed, distance,
            timeUnit),
            RoadModels.computeTravelTime(
              Measure.valueOf(
                ((MultiAttributeData) conn.data().get()).getMaxSpeed().get(),
                speedUnit),
              distance, timeUnit));
      } catch (final Exception e) {
        travelTime +=
          RoadModels.computeTravelTime(vehicleSpeed, distance,
            timeUnit);
      }
      prev = cur;
    }
    return travelTime;
    // TT := millis
    // conn.length := meter
    // speed := kmh

  }

  @Override
  public double computeTheoreticalDistance(Point from, Point to) {
    if (pathTable.contains(from, to)) {
      return Graphs.pathLength(pathTable.get(from, to));
    }
    final List<Point> path = Graphs
      .shortestPath(g, from, to, Graphs.GraphHeuristics.THEORETICAL_TIME);
    pathTable.put(from, to, path);
    return Graphs.pathLength(path);
  }

  @Override
  public double computeCurrentDistance(Point from, Point to) {
    return Graphs.pathLength(
      Graphs.shortestPath(g, from, to, Graphs.GraphHeuristics.TIME));
  }
}
