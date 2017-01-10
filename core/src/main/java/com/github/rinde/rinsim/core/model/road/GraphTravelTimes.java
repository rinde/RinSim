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
package com.github.rinde.rinsim.core.model.road;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.ImmutableGraph;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * The {@link TravelTimes} class to be used with {@link GraphRoadModel} graphs.
 * @author Vincent Van Gestel
 *
 * @param <T> The type of {@link ConnectionData} to expect from the graph.
 *          {@link MultiAttributeData} is required for use of
 *          getTheoreticalShortestTravelTime and getCurrentShortestTravelTimes.
 */
public class GraphTravelTimes<T extends ConnectionData>
    extends AbstractTravelTimes {
  /**
   * Immutable graph.
   */
  private final Graph<T> dynamicGraph;

  private final Table<Point, Point, List<Point>> pathTable;
  private final Map<List<Point>, Double> pathTimeTable;

  /**
   * Create a new {@link GraphTravelTimes} object based on a given graph, using
   * the given measurement units.
   * @param graph The graph to calculate routes on.
   * @param modelTimeUnit The time unit to use.
   * @param modelDistanceUnit The distance unit to use.
   */
  GraphTravelTimes(Graph<T> graph, Unit<Duration> modelTimeUnit,
      Unit<Length> modelDistanceUnit) {
    super(modelTimeUnit, modelDistanceUnit);

    pathTable = HashBasedTable.create();
    pathTimeTable = new HashMap<>();

    this.dynamicGraph = graph;
  }

  /**
   * Creates a new {@link GraphTravelTimes} object based on a previous one.
   * @param travelTimes The previous travel times.
   * @param newGraph The new state of the graph.
   */
  GraphTravelTimes(GraphTravelTimes<T> travelTimes,
      Graph<T> newGraph) {
    super(travelTimes);

    // TODO Check for updates
    pathTable = travelTimes.pathTable;
    pathTimeTable = travelTimes.pathTimeTable;

    this.dynamicGraph = ImmutableGraph.copyOf(newGraph);
  }

  @Override
  public double getTheoreticalShortestTravelTime(Point from, Point to,
      Measure<Double, Velocity> maxVehicleSpeed) {

    final List<Point> path;
    if (pathTable.contains(from, to)) {
      path = pathTable.get(from, to);
    } else {
      path = Graphs.shortestPath(dynamicGraph, from, to,
        Graphs.GraphHeuristics.THEORETICAL_TIME);
      pathTable.put(from, to, path);
    }
    if (pathTimeTable.containsKey(path)) {
      return pathTimeTable.get(path);
    }

    final Iterator<Point> pathI = path.iterator();

    double travelTime = 0d;
    Point prev = pathI.next();
    while (pathI.hasNext()) {
      final Point cur = pathI.next();
      final Connection<T> conn =
        dynamicGraph.getConnection(prev, cur);

      final Measure<Double, Length> distance = Measure.valueOf(
        conn.getLength(), distanceUnit);

      if (!conn.data().isPresent()
        || ((MultiAttributeData) conn.data().get()).getAttributes()
          .get(MultiAttributeData.THEORETICAL_SPEED_ATTRIBUTE) == null) {
        throw new IllegalArgumentException(
          "The graph does not support computations using theoretical speed");
      }

      travelTime +=
        Math.min(RoadModels.computeTravelTime(maxVehicleSpeed, distance,
          timeUnit),
          RoadModels.computeTravelTime(
            Measure.valueOf(
              Double
                .parseDouble((String) ((MultiAttributeData) conn.data().get())
                  .getAttributes()
                  .get(MultiAttributeData.THEORETICAL_SPEED_ATTRIBUTE)),
              maxVehicleSpeed.getUnit()),
            distance, timeUnit));
      prev = cur;
    }
    pathTimeTable.put(path, travelTime);
    return travelTime;

    // TT := millis
    // conn.length := meter
    // speed := kmh
  }

  @Override
  public double getCurrentShortestTravelTime(Point from, Point to,
      Measure<Double, Velocity> maxVehicleSpeed) {
    final Iterator<Point> path =
      Graphs.shortestPath(dynamicGraph, from, to, Graphs.GraphHeuristics.TIME)
        .iterator();

    double travelTime = 0d;
    Point prev = path.next();
    while (path.hasNext()) {
      final Point cur = path.next();
      final Connection<T> conn = dynamicGraph.getConnection(prev, cur);

      final Measure<Double, Length> distance = Measure.valueOf(
        conn.getLength(), distanceUnit);

      if (conn.data().isPresent()
        && ((MultiAttributeData) conn.data().get()).getMaxSpeed().isPresent()) {
        travelTime +=
          Math.min(RoadModels.computeTravelTime(maxVehicleSpeed, distance,
            timeUnit),
            RoadModels.computeTravelTime(
              Measure.valueOf(
                ((MultiAttributeData) conn.data().get()).getMaxSpeed().get(),
                maxVehicleSpeed.getUnit()),
              distance, timeUnit));
      } else {
        // No max speed is defined for this connection
        travelTime +=
          RoadModels.computeTravelTime(maxVehicleSpeed, distance, timeUnit);
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
      Measure<Double, Velocity> maxVehicleSpeed) {
    if (pathTable.contains(from, to)) {
      return Graphs.pathLength(pathTable.get(from, to));
    }
    final List<Point> path = Graphs
      .shortestPath(dynamicGraph, from, to,
        Graphs.GraphHeuristics.THEORETICAL_TIME);
    pathTable.put(from, to, path);
    return Graphs.pathLength(path);
  }

  @Override
  public double computeCurrentDistance(Point from, Point to,
      Measure<Double, Velocity> maxVehicleSpeed) {
    return Graphs.pathLength(
      Graphs.shortestPath(dynamicGraph, from, to, Graphs.GraphHeuristics.TIME));
  }
}
