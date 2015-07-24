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
package com.github.rinde.rinsim.central.rt;

import java.util.Set;

import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;

/**
 * Builder for creating {@link RtSimSolver} instances. See {@link RtSolverModel}
 * for how to obtain an instance.
 * @author Rinde van Lon
 */
public abstract class RtSimSolverBuilder {

  RtSimSolverBuilder() {}

  /**
   * Associates the specified vehicles to all {@link RtSimSolver}s that are
   * built by this builder. By default, all vehicles in the simulator are
   * associated to
   * @param vehicles The set of vehicles that will be associated to
   *          {@link RtSimSolver} instances. May not be empty.
   * @return This, as per the builder pattern.
   */
  public abstract RtSimSolverBuilder setVehicles(
      Set<? extends Vehicle> vehicles);

  /**
   * Construct a new {@link RtSimSolver} based on the specified
   * {@link RealtimeSolver}.
   * @param solver The solver to use internally in the {@link RtSimSolver}.
   * @return A new {@link RtSimSolver} instance.
   */
  public abstract RtSimSolver build(RealtimeSolver solver);

  /**
   * Construct a new {@link RtSimSolver} based on the specified {@link Solver}.
   * The specified {@link Solver} is converted to a {@link RealtimeSolver} using
   * {@link SolverToRealtimeAdapter}.
   * @param solver The solver to use internally in the {@link RtSimSolver}.
   * @return A new {@link RtSimSolver} instance.
   */
  public abstract RtSimSolver build(Solver solver);
}
