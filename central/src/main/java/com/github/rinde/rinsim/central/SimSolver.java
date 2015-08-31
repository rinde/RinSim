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
package com.github.rinde.rinsim.central;

import static com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState.ANNOUNCED;
import static com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState.AVAILABLE;
import static com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState.PICKING_UP;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.measure.Measure;
import javax.measure.quantity.Duration;

import com.github.rinde.rinsim.central.Solvers.SimulationConverter;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.central.Solvers.StateContext;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

/**
 * Adapter for {@link Solver}s.
 * @author Rinde van Lon
 */
public final class SimSolver implements SimulationConverter {
  final Optional<Solver> solver;
  final Clock clock;
  final PDPRoadModel roadModel;
  final PDPModel pdpModel;
  final List<Vehicle> vehicles;

  SimSolver(Optional<Solver> s, PDPRoadModel rm, PDPModel pm,
      Clock sim, List<Vehicle> vs) {
    solver = s;
    clock = sim;
    roadModel = rm;
    pdpModel = pm;
    vehicles = vs;
  }

  /**
   * Calls the {@link Solver} to solve the problem as defined by the current
   * simulation state.
   * @param args {@link SolveArgs} specifying what information to include.
   * @return A list containing routes for each vehicle known to this solver.
   */
  public List<Queue<Parcel>> solve(SolveArgs args) {
    return solve(convert(args));
  }

  /**
   * Calls the {@link Solver} to solve the problem as defined by the current
   * simulation state.
   * @param state The {@link StateContext} that specifies the current simulation
   *          state.
   * @return A list of routes, one for each vehicle.
   */
  public List<Queue<Parcel>> solve(StateContext state) {
    try {
      return Solvers.convertRoutes(state, solver.get().solve(state.state));
    } catch (final InterruptedException e) {
      throw new IllegalStateException(
          "The solver is interrupted, can't continue.", e);
    }
  }

  @Override
  public StateContext convert(SolveArgs args) {
    final Collection<Vehicle> vs = vehicles.isEmpty() ? roadModel
        .getObjectsOfType(Vehicle.class) : vehicles;
    final Set<Parcel> ps = args.parcels.isPresent()
        ? args.parcels.get()
        : ImmutableSet.copyOf(pdpModel.getParcels(ANNOUNCED, AVAILABLE,
          PICKING_UP));
    return Solvers.convert(roadModel, pdpModel, vs, ps, time(),
      args.currentRoutes);
  }

  Measure<Long, Duration> time() {
    return Measure.valueOf(clock.getCurrentTime(),
      clock.getTimeUnit());
  }
}
