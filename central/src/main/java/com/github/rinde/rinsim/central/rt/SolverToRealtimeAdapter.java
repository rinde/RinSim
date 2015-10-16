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

import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.central.GlobalStateObject;
import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Adapter of {@link Solver} to {@link RealtimeSolver}. This real-time solver
 * behaves as follows, upon receiving a new snapshot
 * {@link RealtimeSolver#receiveSnapshot(GlobalStateObject)} the underlying
 * {@link Solver} is called to solve the problem. Any ongoing computation of a
 * previous snapshot is cancelled. When the solver completes its computation,
 * {@link Scheduler#updateSchedule(ImmutableList)} is called to provide the
 * updated schedule. The scheduler is also notified that no computations are
 * currently taking place by calling {@link Scheduler#doneForNow()}.
 * <p>
 * TODO talk about interrupt in solver
 *
 * @author Rinde van Lon
 */
public final class SolverToRealtimeAdapter implements RealtimeSolver {
  static final Logger LOGGER =
    LoggerFactory.getLogger(SolverToRealtimeAdapter.class);

  Optional<Scheduler> scheduler;
  Optional<ListenableFuture<ImmutableList<ImmutableList<Parcel>>>> currentFuture;
  private final Solver solver;

  SolverToRealtimeAdapter(Solver s) {
    solver = s;
    currentFuture = Optional.absent();
    scheduler = Optional.absent();
  }

  @Override
  public void init(Scheduler s) {
    scheduler = Optional.of(s);
  }

  @Override
  public void receiveSnapshot(GlobalStateObject snapshot) {
    checkState(scheduler.isPresent(), "Not yet initialized.");
    if (currentFuture.isPresent() && !currentFuture.get().isDone()) {
      LOGGER.trace("attempt to cancel running Solver..");
      currentFuture.get().cancel(true);
    }
    currentFuture = Optional.of(
      scheduler.get().getSharedExecutor().submit(
        new SolverComputer(solver, snapshot)));

    Futures.addCallback(currentFuture.get(),
      new FutureCallback<ImmutableList<ImmutableList<Parcel>>>() {
        @Override
        public void onSuccess(
            @Nullable ImmutableList<ImmutableList<Parcel>> result) {
          LOGGER.trace("onSuccess: " + result);
          if (result == null) {
            scheduler.get().reportException(
              new IllegalArgumentException("Solver.solve(..) must return a "
                  + "non-null result. Solver: " + solver));
            return;
          }
          scheduler.get().updateSchedule(result);
          scheduler.get().doneForNow();
        }

        @Override
        public void onFailure(Throwable t) {
          if (t instanceof CancellationException) {
            LOGGER.trace("Solver execution got cancelled");
            return;
          }
          scheduler.get().reportException(t);
        }
      });
  }

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
    return new Sup(solver);
  }

  static class SolverComputer
      implements Callable<ImmutableList<ImmutableList<Parcel>>> {
    final Solver solver;
    final GlobalStateObject snapshot;

    SolverComputer(Solver sol, GlobalStateObject snap) {
      solver = sol;
      snapshot = snap;
    }

    @Override
    public ImmutableList<ImmutableList<Parcel>> call() throws Exception {
      return solver.solve(snapshot);
    }
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
      return Joiner.on("").join(SolverToRealtimeAdapter.class.getSimpleName(),
        ".create(", solver, ")");
    }
  }
}
