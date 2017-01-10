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
package com.github.rinde.rinsim.central;

import java.util.Set;

import com.github.rinde.rinsim.core.model.pdp.Vehicle;

/**
 * A builder for creating a {@link SimSolver}. Can be obtained by implementing
 * {@link SolverUser}.
 * @author Rinde van Lon
 */
public abstract class SimSolverBuilder {

  SimSolverBuilder() {}

  /**
   * Sets the vehicles to use to the specified vehicles and no other.
   * @param vehicles The vehicles to use in the solver.
   * @return This, as per the builder pattern.
   */
  public abstract SimSolverBuilder setVehicles(
      Set<? extends Vehicle> vehicles);

  /**
   * Construct a new {@link SimSolver} instance.
   * @param s The {@link Solver} to use internally.
   * @return A new instance.
   */
  public abstract SimSolver build(Solver s);
}
