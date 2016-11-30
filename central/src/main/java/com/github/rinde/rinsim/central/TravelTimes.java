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

import com.github.rinde.rinsim.geom.Point;

public interface TravelTimes {

  /**
   * Computes the theoretical travel time between <code>from</code> and
   * <code>to</code> using the fastest available vehicle.
   * @param from The origin position.
   * @param to The destination position.
   * @return The expected travel time between the two positions.
   */
  long getTheoreticalShortestTravelTime(Point from, Point to);

  /**
   * Computes the current (based on a snapshot) travel time between
   * <code>from</code> and <code>to</code> using the fastest available vehicle.
   * @param from The origin position.
   * @param to The destination position.
   * @return The expected travel time between the two positions.
   */
  long getCurrentShortestTravelTime(Point from, Point to);

  /**
   * Computes the distance between two points, denoted as <code>from</code> and
   * <code>to</code> using the theoretically fastest possible route.
   * @param from The origin position
   * @param to The destination position.
   * @return The expected distance between two positions.
   */
  double computeTheoreticalDistance(Point from, Point to);

  /**
   * Computes the current distance between two points, denoted as
   * <code>from</code> and <code>to</code> using the fastest possible route.
   * @param from The origin position
   * @param to The destination position.
   * @return The expected distance between two positions.
   */
  double computeCurrentDistance(Point from, Point to);

}
