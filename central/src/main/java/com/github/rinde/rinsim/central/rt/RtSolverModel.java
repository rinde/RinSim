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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executors;

import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.central.Solvers;
import com.github.rinde.rinsim.central.Solvers.SimulationConverter;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.central.Solvers.StateContext;
import com.github.rinde.rinsim.central.rt.RtSolverModel.RtSimSolverSchedulerImpl.EventType;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.time.Clock.ClockEventType;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.RtClockEventType;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * {@link RtSolverModel} allows other models and objects added to the simulator
 * to obtain instances of {@link RtSimSolver} to use a {@link RealtimeSolver} in
 * a simulation. For models it is possible to declare a dependency on
 * {@link RtSimSolverBuilder}, other objects can implement the
 * {@link RtSolverUser} interface which allows injection of a
 * {@link RtSimSolverBuilder}.
 * <p>
 * The model can be in two different modes: single mode or multi mode. In single
 * mode only one {@link RtSimSolver} can be created and it can only be obtained
 * by another model (by declaring a dependency). In multi mode there are no
 * limits on the number of {@link RtSimSolver}s that can be created and
 * implementations of {@link RtSolverUser} can created {@link RtSimSolver}s as
 * well. By default the model will use a mode depending on the method through
 * which its <i>first</i> builder is requested. If the first request comes from
 * another model (by declaring a dependency), the model will start in single
 * mode. If the first request comes from an {@link RtSolverUser} the model will
 * be in multi mode.
 * <p>
 * See {@link #builder()} for creating the model and setting the initial mode.
 * <p>
 * <b>Model properties</b>
 * <ul>
 * <li><i>Associated type:</i> {@link RtSolverUser}.</li>
 * <li><i>Provides:</i> {@link RtSimSolverBuilder}.</li>
 * <li><i>Dependencies:</i> {@link RealtimeClockController},
 * {@link PDPRoadModel}, {@link PDPModel}.</li>
 * </ul>
 * @author Rinde van Lon
 */
public final class RtSolverModel extends AbstractModel<RtSolverUser>
    implements TickListener, Listener {
  private static final int NUM_THREADS_IN_SINGLE_MODE = 2;
  final RealtimeClockController clock;
  final PDPRoadModel roadModel;
  final PDPModel pdpModel;
  final SimSolversManager manager;
  Optional<ListeningExecutorService> executor;
  Mode mode;

  boolean checkNextTick;

  enum Mode {
    MULTI_MODE, SINGLE_MODE, UNKNOWN;
  }

  RtSolverModel(RealtimeClockController c, PDPRoadModel rm, PDPModel pm,
      Mode m) {
    clock = c;
    roadModel = rm;
    pdpModel = pm;
    manager = new SimSolversManager();
    executor = Optional.absent();
    mode = m;

    clock.getEventAPI().addListener(this, RtClockEventType.values());
    clock.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        if (executor.isPresent()) {
          executor.get().shutdown();
        }
      }
    }, ClockEventType.STOPPED);
    pdpModel.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        clock.switchToRealTime();
      }
    }, PDPModelEventType.NEW_PARCEL);
    initExecutor();
  }

  @Override
  public boolean register(RtSolverUser element) {
    checkState(mode != Mode.SINGLE_MODE,
      "Can not register a %s because %s is in single mode.",
      RtSolverUser.class.getSimpleName(), RtSolverModel.class.getSimpleName());
    mode = Mode.MULTI_MODE;
    initExecutor();
    element.setSolverProvider(new RtSimSolverBuilderImpl());
    return true;
  }

  @Override
  public boolean unregister(RtSolverUser element) {
    return true;
  }

  @Override
  public <U> U get(Class<U> clazz) {
    checkArgument(clazz == RtSimSolverBuilder.class,
      "%s only provides %s, not %s.", getClass().getSimpleName(),
      RtSimSolverBuilder.class.getSimpleName(), clazz);
    if (mode == Mode.UNKNOWN) {
      mode = Mode.SINGLE_MODE;
      initExecutor();
    }
    return clazz.cast(new RtSimSolverBuilderImpl());
  }

  @Override
  public void tick(TimeLapse timeLapse) {}

  @Override
  public void afterTick(TimeLapse timeLapse) {
    if (checkNextTick) {
      checkNextTick = false;
      if (!manager.isComputing() && clock.isTicking()) {
        clock.switchToSimulatedTime();
      }
    }
  }

  @Override
  public void handleEvent(Event e) {
    if (e.getEventType() == RtClockEventType.SWITCH_TO_REAL_TIME) {
      checkNextTick = true;
    }
  }

  void initExecutor() {
    if (!executor.isPresent() && mode != Mode.UNKNOWN) {
      if (mode == Mode.SINGLE_MODE) {
        executor = Optional.of(
          MoreExecutors.listeningDecorator(
            Executors.newFixedThreadPool(NUM_THREADS_IN_SINGLE_MODE)));
      } else {
        // multi mode
        executor = Optional.of(
          MoreExecutors.listeningDecorator(
            Executors.newFixedThreadPool(
              Runtime.getRuntime().availableProcessors())));
      }
    }
  }

  /**
   * Constructs a {@link Builder} instance for {@link RtSolverModel}.
   * @return A new instance.
   */
  public static Builder builder() {
    return Builder.create(Mode.UNKNOWN);
  }

  /**
   * A builder for {@link RtSolverModel} instances.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder
      extends AbstractModelBuilder<RtSolverModel, RtSolverUser> {

    Builder() {
      setDependencies(RealtimeClockController.class, PDPRoadModel.class,
        PDPModel.class);
      setProvidingTypes(RtSimSolverBuilder.class);
    }

    abstract Mode getMode();

    /**
     * The initial mode of the produced {@link RtSolverModel} will be 'single'.
     * See {@link RtSolverModel} for more information.
     * @return This, as per the builder pattern.
     */
    public Builder withSingleMode() {
      return create(Mode.SINGLE_MODE);
    }

    /**
     * The initial mode of the produced {@link RtSolverModel} will be 'multi'.
     * See {@link RtSolverModel} for more information.
     * @return This, as per the builder pattern.
     */
    public Builder withMultiMode() {
      return create(Mode.MULTI_MODE);
    }

    @Override
    public RtSolverModel build(DependencyProvider dependencyProvider) {
      final RealtimeClockController c = dependencyProvider
          .get(RealtimeClockController.class);
      final PDPRoadModel rm = dependencyProvider.get(PDPRoadModel.class);
      final PDPModel pm = dependencyProvider.get(PDPModel.class);
      return new RtSolverModel(c, rm, pm, getMode());
    }

    static Builder create(Mode m) {
      return new AutoValue_RtSolverModel_Builder(m);
    }
  }

  class SimSolversManager implements Listener {
    final Set<RtSimSolverSchedulerImpl> simSolvers;
    final Set<RtSimSolverSchedulerImpl> computingSimSolvers;

    SimSolversManager() {
      simSolvers = new LinkedHashSet<>();
      computingSimSolvers = new LinkedHashSet<>();
    }

    void register(RtSimSolverSchedulerImpl s) {
      simSolvers.add(s);
      s.getEventAPI().addListener(this,
        EventType.START_COMPUTING,
        EventType.DONE_COMPUTING);
    }

    boolean isComputing() {
      return !computingSimSolvers.isEmpty();
    }

    @Override
    public void handleEvent(Event e) {
      if (e.getEventType() == EventType.START_COMPUTING) {
        clock.switchToRealTime();
        computingSimSolvers.add((RtSimSolverSchedulerImpl) e.getIssuer());
      } else {
        // done computing
        checkState(computingSimSolvers.remove(e.getIssuer()), "Internal error",
          computingSimSolvers, e.getIssuer());
        if (!isComputing()) {
          clock.switchToSimulatedTime();
        }
      }
    }
  }

  class RtSimSolverBuilderImpl extends RtSimSolverBuilder {
    private Set<Vehicle> associatedVehicles;

    RtSimSolverBuilderImpl() {
      associatedVehicles = ImmutableSet.of();
    }

    @Override
    public RtSimSolverBuilder setVehicles(Set<? extends Vehicle> vehicles) {
      checkArgument(!vehicles.isEmpty());
      associatedVehicles = ImmutableSet.copyOf(vehicles);
      return this;
    }

    @Override
    public RtSimSolver build(RealtimeSolver solver) {
      if (mode == Mode.SINGLE_MODE) {
        checkState(manager.simSolvers.isEmpty(),
          "In single mode %s can build only one %s.",
          RtSimSolverBuilder.class.getSimpleName(),
          RtSimSolver.class.getSimpleName());
      }
      final RtSimSolverSchedulerImpl s =
        new RtSimSolverSchedulerImpl(clock, solver, roadModel, pdpModel,
            associatedVehicles, executor.get());
      manager.register(s);
      return s.rtSimSolver;
    }

    @Override
    public RtSimSolver build(Solver solver) {
      return build(new SolverToRealtimeAdapter(solver));
    }
  }

  static class RtSimSolverSchedulerImpl {
    final EventDispatcher simSolverEventDispatcher;
    final EventDispatcher eventDispatcher;
    final SimulationConverter converter;
    final RealtimeSolver solver;
    final RealtimeClockController clock;
    final ListeningExecutorService executor;
    final RtSimSolver rtSimSolver;
    final Scheduler scheduler;
    final RtSimSolverSchedulerImpl reference;
    Optional<ImmutableList<ImmutableList<Parcel>>> currentSchedule;
    boolean isUpdated;

    RtSimSolverSchedulerImpl(RealtimeClockController c, RealtimeSolver s,
        PDPRoadModel rm, PDPModel pm, Set<Vehicle> vehicles,
        ListeningExecutorService ex) {
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
      solver.init(scheduler);
    }

    public EventAPI getEventAPI() {
      return eventDispatcher.getPublicEventAPI();
    }

    enum EventType {
      START_COMPUTING, DONE_COMPUTING;
    }

    class InternalRtSimSolver extends RtSimSolver {

      InternalRtSimSolver() {}

      @Override
      public void solve(SolveArgs args) {
        checkState(clock.getClockMode() == ClockMode.REAL_TIME,
          "Clock must be in real-time mode before calling this method.");
        eventDispatcher.dispatchEvent(new Event(
            RtSimSolverSchedulerImpl.EventType.START_COMPUTING, reference));
        final StateContext sc = converter.convert(args);
        executor.submit(new Runnable() {
          @Override
          public void run() {
            solver.receiveSnapshot(sc.state);
          }
        });
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
    }

    class InternalScheduler extends Scheduler {
      InternalScheduler() {}

      @Override
      public void updateSchedule(ImmutableList<ImmutableList<Parcel>> routes) {
        currentSchedule = Optional.of(routes);
        isUpdated = true;
        simSolverEventDispatcher.dispatchEvent(
          new Event(RtSimSolver.EventType.NEW_SCHEDULE, rtSimSolver));
      }

      @Override
      public ImmutableList<ImmutableList<Parcel>> getCurrentSchedule() {
        checkState(currentSchedule.isPresent(),
          "No schedule has been set, use updateSchedule(..).");
        return currentSchedule.get();
      }

      @Override
      public void doneForNow() {
        eventDispatcher.dispatchEvent(
          new Event(EventType.DONE_COMPUTING, reference));
      }

      @Override
      public ListeningExecutorService getSharedExecutor() {
        return executor;
      }
    }
  }
}
