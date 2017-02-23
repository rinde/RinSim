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

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.geom.GeomHeuristic;
import com.github.rinde.rinsim.geom.Point;

/**
 * The interface for {@link RoadModel} snapshots. A snapshot is an immutable
 * view of a {@link RoadModel} at a specific time.
 * @author Vincent Van Gestel
 *
 */
public interface RoadModelSnapshot {

  /**
   * Similar to *
   * {@link RoadModel#getPathTo(Point, Point, Unit, Measure, GeomHeuristic)},
   * but on a static view of the {@link RoadModel}.
   * @param from The starting point.
   * @param to The ending point.
   * @param timeUnit The unit of time.
   * @param speed The maximum speed of the {@link RoadUser} that will travel on
   *          this path.
   * @param heuristic The heuristic to use for finding an optimal path.
   * @return A path decorated with the heuristic value and travel time resulting
   *         from the given {@link GeomHeuristic}.
   */
  RoadPath getPathTo(Point from, Point to, Unit<Duration> timeUnit,
      Measure<Double, Velocity> speed, GeomHeuristic heuristic);

  /**
   * Similar to {@link RoadModel#getDistanceOfPath(Iterable)}, but on a static
   * view of the {@link RoadModel}.
   * @param path The path to determine the distance of.
   * @return The length of the given path in the distance unit of the
   *         {@link RoadModel}.
   * @throws IllegalArgumentException If the path contains less than two points.
   */
  Measure<Double, Length> getDistanceOfPath(Iterable<Point> path)
      throws IllegalArgumentException;

}
