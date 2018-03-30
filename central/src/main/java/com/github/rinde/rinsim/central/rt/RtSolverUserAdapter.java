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

import java.util.Set;

import com.github.rinde.rinsim.central.GlobalStateObject;
import com.github.rinde.rinsim.central.SimSolver;
import com.github.rinde.rinsim.central.SimSolverBuilder;
import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.central.SolverUser;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon
 */
final class RtSolverUserAdapter implements SolverUser {
  final RtSolverUser delegate;

  RtSolverUserAdapter(RtSolverUser deleg) {
    delegate = deleg;
  }

  @Override
  public void setSolverProvider(SimSolverBuilder builder) {
    delegate.setSolverProvider(new SimSolverBuilderAdapter(builder));
  }

  static class SimSolverBuilderAdapter extends RtSimSolverBuilder {
    final SimSolverBuilder delegate;

    SimSolverBuilderAdapter(SimSolverBuilder deleg) {
      delegate = deleg;
    }

    @Override
    public RtSimSolverBuilder setVehicles(Set<? extends Vehicle> vehicles) {
      delegate.setVehicles(vehicles);
      return this;
    }

    @Override
    public RtSimSolver build(RealtimeSolver solver) {
      if (solver instanceof SolverToRealtimeAdapter) {
        return build(((SolverToRealtimeAdapter) solver).solver);
      }
      throw new UnsupportedOperationException(
        "This class only works with Solvers wrapped by "
          + "SolverToRealtimeAdapter");
    }

    @Override
    public RtSimSolver build(Solver solver) {
      return new SimSolverAdapter(delegate.build(solver));
    }
  }

  static class SimSolverAdapter extends RtSimSolver {
    final EventDispatcher eventDispatcher;
    final SimSolver delegate;
    ImmutableList<ImmutableList<Parcel>> currentSchedule;
    GlobalStateObject currentState;
    boolean isUpdated;

    SimSolverAdapter(SimSolver deleg) {
      delegate = deleg;
      eventDispatcher = new EventDispatcher(RtSimSolver.EventType.values());
    }

    @Override
    public void solve(SolveArgs args) {
      solve(delegate.convert(args));
    }

    @Override
    public void solve(GlobalStateObject state) {
      currentState = state;
      currentSchedule = delegate.solve(state);
      isUpdated = true;
      eventDispatcher.dispatchEvent(new SolverEvent(
        RtSimSolver.EventType.NEW_SCHEDULE,
        Optional.of(currentSchedule),
        Optional.of(currentState)));
      eventDispatcher.dispatchEvent(new SolverEvent(
        RtSimSolver.EventType.DONE,
        Optional.of(currentSchedule),
        Optional.of(currentState)));
    }

    @Override
    @Deprecated
    public void sendSnapshot(SolveArgs args) {
      throw new UnsupportedOperationException(
        "Sending snapshots is not supported.");
    }

    @Override
    public GlobalStateObject getCurrentState(SolveArgs args) {
      return delegate.convert(args);
    }

    @Override
    public boolean isScheduleUpdated() {
      return isUpdated;
    }

    @Override
    public ImmutableList<ImmutableList<Parcel>> getCurrentSchedule() {
      isUpdated = false;
      return currentSchedule;
    }

    @Override
    public boolean isComputing() {
      return false;
    }

    @Override
    @Deprecated
    public void cancel() {
      throw new UnsupportedOperationException("Cancelling is not supported.");
    }

    @Override
    public Object getLock() {
      return this;
    }

    @Override
    public EventAPI getEventAPI() {
      return eventDispatcher.getPublicEventAPI();
    }
  }
}
