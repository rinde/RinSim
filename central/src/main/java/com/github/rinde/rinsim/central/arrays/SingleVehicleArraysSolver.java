/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import javax.annotation.Nullable;

/**
 * Interface for solvers for the single vehicle pickup-and-delivery problem with
 * time windows (SPDPTW). This interface uses <code>int</code> arrays for
 * specification of the inputs and outputs.
 * <p>
 * <b>Note that usage of this interface is not recommended.</b> The
 * {@link com.github.rinde.rinsim.central.Solver} interface provides exactly the same
 * functionality but in a type safe manner.
 * @author Rinde van Lon 
 */
public interface SingleVehicleArraysSolver {

  /**
   * Gives a solution for the single vehicle PDPTW as specified by the
   * parameters. The returned solution does not necessarily need to be optimal
   * but it needs to be feasible. The {@link ArraysSolverValidator} can check
   * whether a {@link SingleVehicleArraysSolver} produces a valid solution and
   * it can check whether the parameters for the
   * {@link SingleVehicleArraysSolver} are valid.
   * <p>
   * All times are in units relative to the current time (0). All constraints
   * are soft, i.e. lateness at service locations and at depot are allowed. The
   * start location has index 0, the end location (depot) has index n-1.
   * 
   * @param travelTime <code>n x n</code> distance matrix expressed in time:
   *          <code>travelTime[i][j]</code> specifies travelTime from location
   *          <code>i</code> to location <code>j</code>.
   * @param releaseDates specifies the left side of the time window for every
   *          location (hard constraint, earlier is not allowed).
   * @param dueDates specifies the right side of the time window for every
   *          location (soft constraint, lateness is allowed).
   * @param servicePairs <code>n x 2</code> matrix of service location pairs,
   *          <code>servicePairs[i][0]</code> and
   *          <code>servicePairs[i][1]</code> specify the pickup and delivery
   *          location respectively. Each location may occur at maximum once in
   *          the matrix (either as an pickup or as a delivery).
   * @param serviceTimes specifies the service time for all locations (both
   *          pickups and deliveries).
   * @param currentSolution If defined it contains the current route of the
   *          vehicle. This can be used as a starting point for a new solution.
   *          If any new parcels have become known since this current solution
   *          was computed these parcels will <i>not</i> be part of the
   *          solutions. This means that these solutions may not be feasible
   *          solutions and thus require adaptation.
   * @return The solution object which indicates a (usually the best found)
   *         solution for the single vehicle PDPTW.
   */
  SolutionObject solve(int[][] travelTime, int[] releaseDates, int[] dueDates,
      int[][] servicePairs, int[] serviceTimes,
      @Nullable SolutionObject currentSolution);
}
