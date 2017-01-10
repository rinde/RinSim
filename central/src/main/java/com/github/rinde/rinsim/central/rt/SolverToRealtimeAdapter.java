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
package com.github.rinde.rinsim.central.rt;

import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.CancellationException;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.central.GlobalStateObject;
import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.central.Solvers;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

final class SolverToRealtimeAdapter implements RealtimeSolver {
  static final Logger LOGGER =
    LoggerFactory.getLogger(SolverToRealtimeAdapter.class);
  private static final String R_BRACE = ")";

  Optional<Scheduler> scheduler;
  Optional<ListenableFuture<ImmutableList<ImmutableList<Parcel>>>> currentFuture;
  final Solver solver;

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
  public void problemChanged(final GlobalStateObject snapshot) {
    checkState(scheduler.isPresent(), "Not yet initialized.");
    cancel();
    currentFuture = Optional.of(
      scheduler.get().getSharedExecutor().submit(
        Solvers.createSolverCallable(solver, snapshot)));

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
          } else {
            scheduler.get().updateSchedule(snapshot, result);
            scheduler.get().doneForNow();
          }
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

  @Override
  public void cancel() {
    if (isComputing()) {
      LOGGER.trace("attempt to cancel running Solver..");
      currentFuture.get().cancel(true);
      scheduler.get().doneForNow();
    }
  }

  @Override
  public boolean isComputing() {
    return currentFuture.isPresent() && !currentFuture.get().isDone();
  }

  @Override
  public void receiveSnapshot(GlobalStateObject snapshot) {}

  @Override
  public String toString() {
    return Joiner.on("").join(getClass().getSimpleName(), "(",
      solver.toString(), R_BRACE);
  }
}
