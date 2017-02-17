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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Iterator;
import java.util.List;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.Graphs.Heuristic;
import com.github.rinde.rinsim.geom.HeuristicPath;
import com.github.rinde.rinsim.geom.ImmutableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.google.auto.value.AutoValue;

/**
 * The snapshot for a {@link GraphRoadModel}. It can be a snapshot of a
 * {@link DynamicGraphRoadModel} as well, since a snapshot loses its dynamic
 * aspect.
 * @author Vincent Van Gestel
 */
@AutoValue
abstract class GraphRoadModelSnapshot
    implements RoadModelSnapshot {

  GraphRoadModelSnapshot() {}

  public abstract ImmutableGraph<? extends ConnectionData> getGraph();

  public abstract Unit<Length> getModelDistanceUnit();

  @Override
  public HeuristicPath getPathTo(Point from, Point to, Unit<Duration> timeUnit,
      Measure<Double, Velocity> speed, Heuristic heuristic) {
    final List<Point> path =
      Graphs.shortestPath(getGraph(), from, to, heuristic);

    final Iterator<Point> pathIt = path.iterator();

    double cost = 0d;
    double travelTime = 0d;
    Point prev = pathIt.next();
    while (pathIt.hasNext()) {
      final Point cur = pathIt.next();
      cost += heuristic.calculateCost(getGraph(), prev, cur);
      travelTime += heuristic.calculateTravelTime(getGraph(), prev, cur,
        getModelDistanceUnit(), speed, timeUnit);
      prev = cur;
    }
    return HeuristicPath.create(path, cost, travelTime);
  }

  @Override
  public Measure<Double, Length> getDistanceOfPath(Iterable<Point> path)
      throws IllegalArgumentException {
    final Iterator<Point> pathIt = path.iterator();
    checkArgument(pathIt.hasNext(), "Cannot check distance of an empty path.");
    Point prev = pathIt.next();
    Point cur = null;
    checkArgument(pathIt.hasNext(),
      "Cannot check distance of a path with only one element.");
    double distance = 0d;
    while (pathIt.hasNext()) {
      cur = pathIt.next();
      distance += getGraph().connectionLength(prev, cur);
      prev = cur;
    }
    return Measure.valueOf(distance, getModelDistanceUnit());
  }

  static GraphRoadModelSnapshot create(
      ImmutableGraph<ConnectionData> graph, Unit<Length> distanceUnit) {
    return new AutoValue_GraphRoadModelSnapshot(graph, distanceUnit);
  }

}
