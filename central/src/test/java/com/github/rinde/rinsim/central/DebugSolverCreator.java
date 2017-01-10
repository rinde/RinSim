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

import javax.measure.quantity.Duration;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.MersenneTwister;

import com.github.rinde.rinsim.central.arrays.ArraysSolverDebugger;
import com.github.rinde.rinsim.central.arrays.ArraysSolverDebugger.MVASDebugger;
import com.github.rinde.rinsim.central.arrays.ArraysSolverValidator;
import com.github.rinde.rinsim.central.arrays.MultiVehicleArraysSolver;
import com.github.rinde.rinsim.central.arrays.MultiVehicleSolverAdapter;
import com.github.rinde.rinsim.central.arrays.RandomMVArraysSolver;
import com.github.rinde.rinsim.util.StochasticSupplier;

/**
 * A solver creator useful for debugging.
 * @author Rinde van Lon
 */
public class DebugSolverCreator implements StochasticSupplier<Solver> {
  /**
   * The arrays solver that is used to compute solutions.
   */
  public final MVASDebugger arraysSolver;

  /**
   * The solver that a wrapper for {@link #arraysSolver}.
   */
  public final SolverDebugger solver;

  /**
   * Create a new instance using the specified time unit and seed.
   * @param timeUnit The time unit to use for the underlying
   *          {@link RandomMVArraysSolver}.
   * @param seed The seed to use for the {@link RandomMVArraysSolver}.
   */
  public DebugSolverCreator(long seed, Unit<Duration> timeUnit) {
    this(new RandomMVArraysSolver(new MersenneTwister(seed)), timeUnit);
  }

  /**
   * Create a new instance using the specified time unit and seed.
   * @param arrSolver The {@link MultiVehicleArraysSolver} that is used for
   *          solving.
   * @param timeUnit The time unit to use for the underlying
   *          {@link RandomMVArraysSolver}.
   */
  public DebugSolverCreator(MultiVehicleArraysSolver arrSolver,
      Unit<Duration> timeUnit) {
    arraysSolver = ArraysSolverDebugger.wrap(
      ArraysSolverValidator.wrap(arrSolver), false);
    solver = SolverDebugger.wrap(SolverValidator
      .wrap(new MultiVehicleSolverAdapter(arraysSolver, timeUnit)), false);
  }

  @Override
  public Solver get(long seed) {
    return solver;
  }
}
