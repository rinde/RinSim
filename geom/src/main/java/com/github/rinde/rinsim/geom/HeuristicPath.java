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
package com.github.rinde.rinsim.geom;

import java.util.List;

import com.github.rinde.rinsim.geom.Graphs.Heuristic;

/**
 * An immutable class containing a path with a certain heuristic value and
 * travel time. The heuristic value is the result of determining the value of
 * the path by a {@link Graphs.Heuristic}.
 * @author Vincent Van Gestel
 */
public final class HeuristicPath {

  /**
   * The actual path.
   */
  public final List<Point> path;
  /**
   * The heuristic value of the path.
   */
  public final double value;
  /**
   * The perceived travel time of the path by the heuristic.
   */
  public final double travelTime;

  /**
   * Constructor for the immutable path, heuristic value pairing.
   * @param pointPath The actual path, as indicated by a list of consecutive
   *          points.
   * @param heuristicValue The value of the path as defined by a
   *          {@link Heuristic}.
   * @param pathTravelTime The travel time over the path as perceived by the
   *          {@link Heuristic}.
   */
  public HeuristicPath(List<Point> pointPath, double heuristicValue,
      double pathTravelTime) {
    path = pointPath;
    value = heuristicValue;
    travelTime = pathTravelTime;
  }

}
