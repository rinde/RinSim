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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Set;
import java.util.concurrent.CancellationException;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.central.GlobalStateObject;
import com.github.rinde.rinsim.central.GlobalStateObject.VehicleStateObject;
import com.github.rinde.rinsim.central.Solvers;
import com.github.rinde.rinsim.central.Solvers.SimulationConverter;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.central.rt.RtSimSolver.NewScheduleEvent;
import com.github.rinde.rinsim.central.rt.RtSolverModel.SimSolversManager;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * Implementor of both:
 * <ul>
 * <li>{@link RtSimSolver} is used for supplying input and receiving output of a
 * solver.</li>
 * <li>{@link Scheduler} applies the computed schedule.</li>
 * </ul>
 *
 * @author Rinde van Lon
 */
class RtSimSolverSchedulerBridge {
  static final Logger LOGGER =
    LoggerFactory.getLogger(RtSimSolverSchedulerBridge.class);

  final EventDispatcher simSolverEventDispatcher;
  final EventDispatcher eventDispatcher;
  final SimulationConverter converter;
  final RealtimeSolver solver;
  final RealtimeClockController clock;
  final ListeningExecutorService executor;
  final InternalRtSimSolver rtSimSolver;
  final Scheduler scheduler;
  final RtSimSolverSchedulerBridge reference;
  final SimSolversManager simSolversManager;
  Optional<ImmutableList<ImmutableList<Parcel>>> currentSchedule;
  boolean isUpdated;

  RtSimSolverSchedulerBridge(RealtimeClockController c, RealtimeSolver s,
      PDPRoadModel rm, PDPModel pm, Set<Vehicle> vehicles,
      ListeningExecutorService ex, SimSolversManager manager) {
    solver = s;
    clock = c;
    converter = Solvers.converterBuilder()
      .with(clock)
      .with(rm)
      .with(pm)
      .with(vehicles)
      .build();
    currentSchedule = Optional.absent();
    isUpdated = false;

    reference = this;
    eventDispatcher = new EventDispatcher(EventType.values());
    simSolverEventDispatcher =
      new EventDispatcher(RtSimSolver.EventType.values());
    executor = ex;
    rtSimSolver = new InternalRtSimSolver();
    scheduler = new InternalScheduler();
    simSolversManager = manager;
    solver.init(scheduler);
    simSolversManager.register(this);
  }

  public EventAPI getEventAPI() {
    return eventDispatcher.getPublicEventAPI();
  }

  enum EventType {
    START_COMPUTING, DONE_COMPUTING;
  }

  void handleFailure(Throwable t) {
    if (t instanceof CancellationException
      || t instanceof InterruptedException) {
      LOGGER.info("RealtimeSolver execution got cancelled/interrupted");
      try {
        eventDispatcher.dispatchEvent(
          new Event(RtSimSolverSchedulerBridge.EventType.DONE_COMPUTING,
            reference));
      } catch (final RuntimeException e) {
        simSolversManager.addException(e);
      }
    } else {
      simSolversManager.addException(t);
    }
  }

  class InternalRtSimSolver extends RtSimSolver {
    final SnapshotCallback callback;

    InternalRtSimSolver() {
      callback = new SnapshotCallback();
    }

    @Override
    public void solve(SolveArgs args) {
      solve(converter.convert(args).state);
    }

    RealtimeSolver getSolver() {
      return solver;
    }

    @Override
    public void solve(final GlobalStateObject state) {
      LOGGER.trace("Solve: {}, {} available parcels.", solver,
        state.getAvailableParcels().size());
      realtimeCheck();
      eventDispatcher.dispatchEvent(new Event(
        RtSimSolverSchedulerBridge.EventType.START_COMPUTING, reference));

      for (final VehicleStateObject vso : state.getVehicles()) {
        checkArgument(vso.getRoute().isPresent(),
          "A route must be present for each vehicle.");

        if (vso.getDestination().isPresent()) {
          checkArgument(!vso.getRoute().get().isEmpty(),
            "Expected %s but found an empty route.",
            vso.getDestination().get());
          checkArgument(
            vso.getRoute().get().get(0).equals(vso.getDestination().get()),
            "Expected %s at first position but found %s.",
            vso.getDestination().get(), vso.getRoute().get());
        }
      }

      final ListenableFuture<?> fut = executor.submit(new Runnable() {
        @Override
        public void run() {
          LOGGER.trace("calling RealtimeSolver.problemChanged(..)");
          solver.problemChanged(state);
        }
      });
      // catch and re-throw any exception occurring during the invocation
      Futures.addCallback(fut, callback);
    }

    void realtimeCheck() {
      checkState(clock.getClockMode() == ClockMode.REAL_TIME,
        "Clock must be in real-time mode before calling this method, but it "
          + "is in %s mode.",
        clock.getClockMode());
    }

    @Override
    public void sendSnapshot(SolveArgs args) {
      realtimeCheck();
      final GlobalStateObject state = converter.convert(args).state;
      final ListenableFuture<?> fut = executor.submit(new Runnable() {
        @Override
        public void run() {
          LOGGER.trace(
            "calling RealtimeSolver.receiveSnapshot(..) sim time: {}",
            state.getTime());
          solver.receiveSnapshot(state);
        }
      });
      // catch and re-throw any exception occurring during the invocation
      Futures.addCallback(fut, callback);
    }

    @Override
    public GlobalStateObject getCurrentState(SolveArgs args) {
      return converter.convert(args).state;
    }

    @Override
    public boolean isScheduleUpdated() {
      return isUpdated;
    }

    @Override
    public ImmutableList<ImmutableList<Parcel>> getCurrentSchedule() {
      checkState(currentSchedule.isPresent(),
        "No schedule has been computed yet.");
      isUpdated = false;
      return currentSchedule.get();
    }

    @Override
    public EventAPI getEventAPI() {
      return simSolverEventDispatcher;
    }

    @Override
    public void cancel() {
      solver.cancel();
    }

    @Override
    public boolean isComputing() {
      return solver.isComputing();
    }

    class SnapshotCallback implements FutureCallback<Object> {

      SnapshotCallback() {}

      @Override
      public void onSuccess(@Nullable Object result) {}

      @Override
      public void onFailure(Throwable t) {
        handleFailure(t);
      }
    }
  }

  class InternalScheduler extends Scheduler {
    InternalScheduler() {}

    @Override
    public void updateSchedule(GlobalStateObject state,
        ImmutableList<ImmutableList<Parcel>> routes) {
      currentSchedule = Optional.of(routes);
      isUpdated = true;
      LOGGER.trace("new schedule");
      try {
        simSolverEventDispatcher.dispatchEvent(
          new NewScheduleEvent(routes, state));
      } catch (final RuntimeException e) {
        reportException(e);
      }
    }

    @Override
    public ImmutableList<ImmutableList<Parcel>> getCurrentSchedule() {
      checkState(currentSchedule.isPresent(),
        "No schedule has been set, use updateSchedule(..).");
      return currentSchedule.get();
    }

    @Override
    public void doneForNow() {
      LOGGER.trace("doneForNow");
      try {
        eventDispatcher.dispatchEvent(
          new Event(EventType.DONE_COMPUTING, reference));
      } catch (final RuntimeException e) {
        reportException(e);
      }
    }

    @Override
    public ListeningExecutorService getSharedExecutor() {
      return executor;
    }

    @Override
    public void reportException(Throwable t) {
      handleFailure(t);
    }
  }
}
