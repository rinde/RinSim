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
package com.github.rinde.rinsim.central.arrays;

import javax.measure.quantity.Duration;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.central.GlobalStateObject;
import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.central.arrays.ArraysSolvers.MVArraysObject;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.google.common.collect.ImmutableList;

/**
 * Adapter for {@link MultiVehicleArraysSolver} to conform to the {@link Solver}
 * interface.
 * @author Rinde van Lon
 */
public class MultiVehicleSolverAdapter implements Solver {

  private final MultiVehicleArraysSolver solver;
  private final Unit<Duration> outputTimeUnit;

  /**
   * @param s The solver to use.
   * @param timeUnit The time unit which is expected by the specified solver.
   */
  public MultiVehicleSolverAdapter(MultiVehicleArraysSolver s,
      Unit<Duration> timeUnit) {
    solver = s;
    outputTimeUnit = timeUnit;
  }

  @Override
  public ImmutableList<ImmutableList<Parcel>> solve(GlobalStateObject state) {
    final MVArraysObject o = ArraysSolvers.toMultiVehicleArrays(state,
      outputTimeUnit);

    final SolutionObject[] sols = solver.solve(o.travelTime, o.releaseDates,
      o.dueDates, o.servicePairs, o.serviceTimes, o.vehicleTravelTimes,
      o.inventories, o.remainingServiceTimes, o.currentDestinations,
      o.currentSolutions);
    final ImmutableList.Builder<ImmutableList<Parcel>> b = ImmutableList
      .builder();
    for (final SolutionObject sol : sols) {
      b.add(ArraysSolvers.convertSolutionObject(sol, o.index2parcel));
    }
    return b.build();
  }
}
