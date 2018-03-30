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
package com.github.rinde.rinsim.central.rt;

import com.github.rinde.rinsim.central.GlobalStateObject;
import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.central.SolverUser;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

/**
 *
 * @author Rinde van Lon
 */
public final class RtStAdapters {

  private RtStAdapters() {}

  /**
   * Adapts {@link Solver} to {@link RealtimeSolver}. This real-time solver
   * behaves as follows, upon receiving a new snapshot
   * {@link RealtimeSolver#problemChanged(GlobalStateObject)} the underlying
   * {@link Solver} is called to solve the problem. Any ongoing computation of a
   * previous snapshot is cancelled. When the solver completes its computation,
   * {@link Scheduler#updateSchedule(GlobalStateObject,ImmutableList)} is called
   * to provide the updated schedule. The scheduler is also notified that no
   * computations are currently taking place by calling
   * {@link Scheduler#doneForNow()}.
   * <p>
   * TODO talk about interrupt in solver
   *
   * @param solver The solver to adapt.
   * @return The adapted solver.
   */
  public static RealtimeSolver toRealtime(Solver solver) {
    return new SolverToRealtimeAdapter(solver);
  }

  /**
   * Adapts {@link Solver} to {@link RealtimeSolver}. This real-time solver
   * behaves as follows, upon receiving a new snapshot
   * {@link RealtimeSolver#problemChanged(GlobalStateObject)} the underlying
   * {@link Solver} is called to solve the problem. Any ongoing computation of a
   * previous snapshot is cancelled. When the solver completes its computation,
   * {@link Scheduler#updateSchedule(GlobalStateObject,ImmutableList)} is called
   * to provide the updated schedule. The scheduler is also notified that no
   * computations are currently taking place by calling
   * {@link Scheduler#doneForNow()}.
   * <p>
   * TODO talk about interrupt in solver
   *
   * @param solver The solver supplier to adapt.
   * @return The adapted solver supplier.
   */
  public static StochasticSupplier<RealtimeSolver> toRealtime(
      StochasticSupplier<? extends Solver> solver) {
    return new Sup(solver);
  }

  public static SolverUser toSimTime(RtSolverUser solverUser) {
    return new RtSolverUserAdapter(solverUser);
  }

  static class Sup implements StochasticSupplier<RealtimeSolver> {
    StochasticSupplier<? extends Solver> solver;

    Sup(StochasticSupplier<? extends Solver> s) {
      solver = s;
    }

    @Override
    public RealtimeSolver get(long seed) {
      return new SolverToRealtimeAdapter(solver.get(seed));
    }

    @Override
    public String toString() {
      return Joiner.on("").join(RtStAdapters.class.getSimpleName(),
        ".toRealtime(", solver, ")");
    }
  }

}
