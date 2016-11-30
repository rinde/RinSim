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
package com.github.rinde.rinsim.core.model.pdp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.model.road.DynamicGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * The {@link TravelTimes} class to be used with {@link DynamicGraphRoadModel}
 * graphs.
 * @author Vincent Van Gestel
 *
 * @param <T> The type of {@link ConnectionData} to expect from the graph.
 *          {@link MultiAttributeData} is required for use of
 *          getTheoreticalShortestTravelTime and getCurrentShortestTravelTimes.
 */
public class DynamicGraphTravelTimes<T extends ConnectionData>
    extends AbstractTravelTimes {
  /**
   * Immutable graph.
   */
  private final Graph<T> g;

  private final Table<Point, Point, List<Point>> pathTable;
  private final Map<List<Point>, Long> pathTT;

  /**
   * Create a new {@link DynamicGraphTravelTimes} object based on a given graph,
   * using the given measurement units.
   * @param graph The graph to calculate routes on.
   * @param tu The time unit to use.
   * @param du The distance unit to use.
   */
  public DynamicGraphTravelTimes(Graph<T> graph, Unit<Duration> tu,
      Unit<Length> du) {
    super(tu, du);

    pathTable = HashBasedTable.create();
    pathTT = new HashMap<>();

    this.g = graph;
  }

  /**
   * Creates a new {@link DynamicGraphTravelTimes} object based on a previous
   * one.
   * @param tt The previous travel times.
   * @param newGraph The new state of the graph.
   */
  public DynamicGraphTravelTimes(DynamicGraphTravelTimes<T> tt,
      Graph<T> newGraph) {
    super(tt);

    // TODO Check for updates
    pathTable = tt.pathTable;
    pathTT = tt.pathTT;

    this.g = Graphs.unmodifiableGraph(newGraph);
  }

  @Override
  public long getTheoreticalShortestTravelTime(Point from, Point to,
      Measure<Double, Velocity> vehicleSpeed) {

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
                    .getAttributes()
                    .get(MultiAttributeData.THEORETICAL_SPEED_ATTRIBUTE)),
                vehicleSpeed.getUnit()),
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
  public long getCurrentShortestTravelTime(Point from, Point to,
      Measure<Double, Velocity> vehicleSpeed) {
    final Iterator<Point> path =
      Graphs.shortestPath(g, from, to, Graphs.GraphHeuristics.TIME)
        .iterator();

    long travelTime = 0L;
    Point prev = path.next();
    while (path.hasNext()) {
      final Point cur = path.next();
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
                vehicleSpeed.getUnit()),
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
  public double computeTheoreticalDistance(Point from, Point to,
      Measure<Double, Velocity> vehicleSpeed) {
    if (pathTable.contains(from, to)) {
      return Graphs.pathLength(pathTable.get(from, to));
    }
    final List<Point> path = Graphs
      .shortestPath(g, from, to, Graphs.GraphHeuristics.THEORETICAL_TIME);
    pathTable.put(from, to, path);
    return Graphs.pathLength(path);
  }

  @Override
  public double computeCurrentDistance(Point from, Point to,
      Measure<Double, Velocity> vehicleSpeed) {
    return Graphs.pathLength(
      Graphs.shortestPath(g, from, to, Graphs.GraphHeuristics.TIME));
  }
}
