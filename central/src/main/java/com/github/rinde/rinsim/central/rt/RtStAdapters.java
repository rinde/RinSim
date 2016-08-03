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
package com.github.rinde.rinsim.central.rt;

import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.central.SolverUser;
import com.github.rinde.rinsim.util.StochasticSupplier;

/**
 *
 * @author Rinde van Lon
 */
public final class RtStAdapters {

  private RtStAdapters() {}

  /**
   * Constructs an adapter of {@link Solver} to {@link RealtimeSolver}. The
   * resulting solver behaves as is documented in
   * {@link SolverToRealtimeAdapter}.
   * @param solver The solver to adapt.
   * @return The adapted solver.
   */
  public static RealtimeSolver create(Solver solver) {
    return new SolverToRealtimeAdapter(solver);
  }

  public static StochasticSupplier<RealtimeSolver> create(
      StochasticSupplier<? extends Solver> solver) {
    return new SolverToRealtimeAdapter.Sup(solver);
  }

  public static SolverUser toSimTime(RtSolverUser solverUser) {
    return new RtSolverUserAdapter(solverUser);
  }

}
