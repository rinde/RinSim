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
package com.github.rinde.rinsim.central.arrays;

import java.util.Arrays;

/**
 * Solution object for single vehicle pickup-and-delivery problem with time
 * windows.
 *
 * @author Rinde van Lon
 */
public class SolutionObject {

  /**
   * Array of locations which have to be serviced in the specified sequence,
   * starts with the begin location and ends with depot. Should have the same
   * length as {@link #arrivalTimes}.
   */
  public final int[] route;

  /**
   * Array of times at which every location servicing <i>starts</i>. The number
   * at <code>arrivalTimes[i]</code> indicates the arrival time at location
   * <code>route[i]</code>. Index 0 always contains the specified remaining
   * service time for this vehicle. Should have the same length as
   * {@link #route}.
   */
  public final int[] arrivalTimes;

  /**
   * The objective value for this solution as computed by the solver.
   */
  public final int objectiveValue;

  /**
   *
   * @param pRoute {@link #route}
   * @param pArrivalTimes {@link #arrivalTimes}
   * @param pObjectiveValue {@link #objectiveValue}
   */
  public SolutionObject(int[] pRoute, int[] pArrivalTimes,
      int pObjectiveValue) {
    route = Arrays.copyOf(pRoute, pRoute.length);
    arrivalTimes = Arrays.copyOf(pArrivalTimes, pArrivalTimes.length);
    objectiveValue = pObjectiveValue;
  }

  @Override
  public String toString() {
    return new StringBuilder("Route: ").append(Arrays.toString(route))
      .append("\n").append("Arrival times: ")
      .append(Arrays.toString(arrivalTimes))
      .append(System.getProperty("line.separator"))
      .append("Objective: ").append(objectiveValue).toString();
  }

}
