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

import com.github.rinde.rinsim.core.model.pdp.Vehicle;

/**
 * A builder for creating a {@link SimulationSolver}. Can be obtained by
 * implementing {@link SolverUser}.
 * @author Rinde van Lon
 */
public abstract class SimulationSolverBuilder {

  SimulationSolverBuilder() {}

  /**
   * Sets the vehicles to use to the specified vehicle and no other.
   * @param v The only vehicle to use in the solver.
   * @return This, as per the builder pattern.
   */
  public abstract SimulationSolverBuilder setVehicle(Vehicle v);

  /**
   * Sets the vehicles to sue to the specified vehicles and no other.
   * @param vehicles The vehicles to use in the solver.
   * @return This, as per the builder pattern.
   */
  public abstract SimulationSolverBuilder setVehicles(
    Iterable<? extends Vehicle> vehicles);

  /**
   * Construct a new {@link SimulationSolver} instance.
   * @param s The {@link Solver} to use internally.
   * @return A new instance.
   */
  public abstract SimulationSolver build(Solver s);
}
