/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

/**
 * A heuristic can be used to direct the {@link Graphs#shortestPath} algorithm,
 * it determines the cost of traveling which should be minimized.
 * @author Rinde van Lon
 * @see GeomHeuristics
 */
public interface GeomHeuristic {
  /**
   * Can be used to estimate the cost of traveling a distance.
   * @param graph The graph.
   * @param from Start point of a connection.
   * @param to End point of a connection.
   * @return The estimate of the cost.
   */
  double estimateCost(Graph<?> graph, Point from, Point to);

  /**
   * Computes the cost of traveling over the connection as specified by the
   * provided points.
   * @param graph The graph.
   * @param from Start point of a connection.
   * @param to End point of a connection.
   * @return The cost of traveling.
   */
  double calculateCost(Graph<?> graph, Point from, Point to);

  /**
   * Computes the travel time that it would take, according to <b>this
   * heuristic</b>, to traverse the distance between <code>from</code> and
   * <code>to</code>.
   * @param graph The graph.
   * @param from Start point of a connection.
   * @param to The end point of a connection.
   * @param distanceUnit The distance unit of the graph.
   * @param speed The maximum travel speed.
   * @param outputTimeUnit The time unit of the return type.
   * @return The travel time.
   */
  double calculateTravelTime(Graph<?> graph, Point from, Point to,
      Unit<Length> distanceUnit,
      Measure<Double, Velocity> speed, Unit<Duration> outputTimeUnit);
}
