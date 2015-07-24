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

import java.util.Set;
import java.util.concurrent.Executors;

import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.central.Solvers;
import com.github.rinde.rinsim.central.Solvers.SimulationConverter;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.central.Solvers.StateContext;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import autovalue.shaded.com.google.common.common.collect.ImmutableSet;

/**
 * {@link RtSolverModel} allows other models and objects added to the simulator
 * to obtain instances of {@link RtSimSolver} to use a {@link RealtimeSolver} in
 * a simulation. For models it is possible to declare a dependency on
 * {@link RtSimSolverBuilder}, other objects can implement the
 * {@link RtSolverUser} interface which allows injection of a
 * {@link RtSimSolverBuilder}.
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
public final class RtSolverModel extends AbstractModel<RtSolverUser> {
  final RealtimeClockController clock;
  final PDPRoadModel roadModel;
  final PDPModel pdpModel;
  final RtSimSolverBuilder builder;

  RtSolverModel(RealtimeClockController c, PDPRoadModel rm, PDPModel pm) {
    clock = c;
    roadModel = rm;
    pdpModel = pm;
    builder = new RtSimSolverBuilderImpl();
  }

  @Override
  public boolean register(RtSolverUser element) {
    element.setSolverProvider(builder);
    return true;
  }

  @Override
  public boolean unregister(RtSolverUser element) {
    return true;
  }

  @Override
  public <U> U get(Class<U> clazz) {
    checkArgument(clazz == RtSimSolverBuilder.class,
      "%s does not provide this type: %s.", getClass().getSimpleName(),
      clazz);
    return clazz.cast(builder);
  }

  /**
   * Constructs a {@link Builder} instance for {@link RtSolverModel}.
   * @return A new instance.
   */
  public static Builder builder() {
    return new AutoValue_RtSolverModel_Builder();
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

    @Override
    public RtSolverModel build(DependencyProvider dependencyProvider) {
      final RealtimeClockController c = dependencyProvider
          .get(RealtimeClockController.class);
      final PDPRoadModel rm = dependencyProvider.get(PDPRoadModel.class);
      final PDPModel pm = dependencyProvider.get(PDPModel.class);
      return new RtSolverModel(c, rm, pm);
    }
  }

  class RtSimSolverBuilderImpl extends RtSimSolverBuilder {
    Set<Vehicle> associatedVehicles;

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
      return new RtSimSolverSchedulerImpl(clock, solver, roadModel,
          pdpModel, associatedVehicles).rtSimSolver;
    }

    @Override
    public RtSimSolver build(Solver solver) {
      return build(new SolverToRealtimeAdapter(solver));
    }
  }

  static class RtSimSolverSchedulerImpl {
    final SimulationConverter converter;
    final RealtimeSolver solver;
    final RealtimeClockController clock;
    Optional<ImmutableList<ImmutableList<Parcel>>> currentSchedule;
    boolean isUpdated;
    final ListeningExecutorService executor;
    final RtSimSolver rtSimSolver;
    final Scheduler scheduler;

    RtSimSolverSchedulerImpl(RealtimeClockController c, RealtimeSolver s,
        PDPRoadModel rm, PDPModel pm, Set<Vehicle> vehicles) {
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

      executor = MoreExecutors
          .listeningDecorator(Executors.newSingleThreadExecutor());

      rtSimSolver = new InternalRtSimSolver();
      scheduler = new InternalScheduler();
      solver.init(scheduler);
    }

    class InternalRtSimSolver extends RtSimSolver {

      InternalRtSimSolver() {}

      @Override
      public void solve(SolveArgs args) {
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
        isUpdated = false;
        return currentSchedule.get();
      }
    }

    class InternalScheduler extends Scheduler {
      InternalScheduler() {}

      @Override
      public void updateSchedule(ImmutableList<ImmutableList<Parcel>> routes) {
        currentSchedule = Optional.of(routes);
        isUpdated = true;
      }

      @Override
      public ImmutableList<ImmutableList<Parcel>> getCurrentSchedule() {
        checkState(currentSchedule.isPresent(),
          "No schedule has been set, use updateSchedule(..).");
        return currentSchedule.get();
      }

      @Override
      public void doneForNow() {
        clock.switchToSimulatedTime();
      }
    }
  }
}
