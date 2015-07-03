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
package com.github.rinde.rinsim.central;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.central.rt.RealtimeSolver;
import com.github.rinde.rinsim.central.rt.Scheduler;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.CompositeModelBuilder;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.core.model.time.RealTimeClockController;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

// FIXME test this class thoroughly
/**
 * A facade for RinSim which provides a centralized interface such that
 * {@link Solver} instances can solve
 * {@link com.github.rinde.rinsim.scenario.Scenario}s.
 * <p>
 * TODO update this comment
 * @author Rinde van Lon
 */
public final class Central {

  private Central() {}

  /**
   * Creates a new {@link Builder} for creating a {@link CentralModel}.
   * @param solverSupplier The solver to use.
   * @return A new instance.
   */
  public static ModelBuilder<?, ?> builder(
    StochasticSupplier<? extends Solver> solverSupplier) {
    return Builder.create(solverSupplier);
  }

  public static ModelBuilder<?, ?> realtimeBuilder(
    StochasticSupplier<? extends Solver> solverSupplier) {

  }

  /**
   * Provides a {@link MASConfiguration} that configures a MAS that is
   * controlled centrally by a {@link Solver}.
   * @param solverCreator The solver creator to use for instantiating solvers.
   * @return A new configuration.
   */
  public static MASConfiguration solverConfiguration(
    StochasticSupplier<? extends Solver> solverCreator) {
    return solverConfiguration(solverCreator, "");
  }

  /**
   * Provides a {@link MASConfiguration} that configures a MAS that is
   * controlled centrally by a {@link Solver}.
   * @param solverCreator The solver creator to use for instantiating solvers.
   * @param nameSuffix A string which is append to the toString() for the
   *          configuration.
   * @return A new configuration.
   */
  public static MASConfiguration solverConfiguration(
    StochasticSupplier<? extends Solver> solverCreator, String nameSuffix) {
    return MASConfiguration.pdptwBuilder()
      .addEventHandler(AddVehicleEvent.class, VehicleCreator.INSTANCE)
      .addModel(Builder.create(solverCreator))
      .setName("Central-" + solverCreator.toString() + nameSuffix)
      .build();
  }

  enum VehicleCreator implements TimedEventHandler<AddVehicleEvent> {
    INSTANCE {
      @Override
      public void handleTimedEvent(AddVehicleEvent event, SimulatorAPI sim) {
        sim.register(new RouteFollowingVehicle(event.getVehicleDTO(), false));
      }

      @Override
      public String toString() {
        return "CentralVehicle";
      }
    }
  }

  @AutoValue
  abstract static class Builder extends
    AbstractModelBuilder<CentralModel, Parcel>implements Serializable {

    private static final long serialVersionUID = 1168431215289110828L;

    Builder() {
      setDependencies(Clock.class,
        PDPRoadModel.class,
        PDPModel.class,
        RandomProvider.class);
    }

    abstract StochasticSupplier<Solver> getSolverSupplier();

    @Override
    public CentralModel build(DependencyProvider dependencyProvider) {
      Clock clock = dependencyProvider.get(Clock.class);
      PDPRoadModel rm = dependencyProvider.get(PDPRoadModel.class);
      PDPModel pm = dependencyProvider.get(PDPModel.class);
      RandomProvider rnd = dependencyProvider.get(RandomProvider.class);
      Solver solver = getSolverSupplier().get(rnd.masterInstance().nextLong());
      return new CentralModel(clock, rm, pm, solver);
    }

    @SuppressWarnings("unchecked")
    static Builder create(StochasticSupplier<? extends Solver> solverSupplier) {
      return new AutoValue_Central_Builder(
        (StochasticSupplier<Solver>) solverSupplier);
    }
  }

  private static final class CentralModel extends AbstractModel<Parcel>
    implements TickListener {
    private boolean hasChanged;
    private final PDPRoadModel roadModel;
    private final SimSolver solverAdapter;
    private final Clock clock;

    CentralModel(Clock c, PDPRoadModel rm, PDPModel pm, Solver solver) {
      clock = c;
      roadModel = rm;
      solverAdapter = Solvers.solverBuilder(solver)
        .with(rm)
        .with(pm)
        .with(clock)
        .build();
    }

    @Override
    public boolean register(Parcel element) {
      hasChanged = true;
      return false;
    }

    @Override
    public boolean unregister(Parcel element) {
      return false;
    }

    @Override
    public void tick(TimeLapse timeLapse) {
      if (hasChanged) {
        hasChanged = false;
        // TODO check to see that this is called the first possible moment after
        // the add parcel event was dispatched

        // TODO it must be checked whether the calculated routes end up in the
        // correct vehicles

        final Set<RouteFollowingVehicle> vehicles = roadModel
          .getObjectsOfType(RouteFollowingVehicle.class);

        // gather current routes
        final ImmutableList.Builder<ImmutableList<Parcel>> currentRouteBuilder = ImmutableList
          .builder();
        for (final RouteFollowingVehicle vehicle : vehicles) {
          final ImmutableList<Parcel> l = ImmutableList.copyOf(vehicle
            .getRoute());
          currentRouteBuilder.add(l);
        }

        final Iterator<Queue<Parcel>> routes = solverAdapter
          .solve(
            SolveArgs.create()
              .useAllParcels()
              .useCurrentRoutes(currentRouteBuilder.build()))
          .iterator();

        for (final RouteFollowingVehicle vehicle : vehicles) {
          vehicle.setRoute(routes.next());
        }
      }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}
  }

  private static abstract class RTBuilder
    extends AbstractModelBuilder<RealtimeCentralModel, Parcel>
    implements CompositeModelBuilder<RealtimeCentralModel, Parcel> {

    RTBuilder() {
      setDependencies(RandomProvider.class, SimulationSolverBuilder.class,
        RealTimeClockController.class, PDPRoadModel.class);
    }

    abstract StochasticSupplier<RealtimeSolver> getSolverSupplier();

    @Override
    public RealtimeCentralModel build(DependencyProvider dependencyProvider) {
      RandomProvider rnd = dependencyProvider.get(RandomProvider.class);
      RealtimeSolver solver = getSolverSupplier()
        .get(rnd.masterInstance().nextLong());
      SimSolver s = dependencyProvider.get(SimulationSolverBuilder.class)
        .build(solver);
      RealTimeClockController clock = dependencyProvider
        .get(RealTimeClockController.class);
      PDPRoadModel rm = dependencyProvider.get(PDPRoadModel.class);
      return new RealtimeCentralModel(clock, s, rm);
    }

    @Override
    public ImmutableSet<ModelBuilder<?, ?>> getChildren() {
      return ImmutableSet.<ModelBuilder<?, ?>> of(SolverModel.builder());
    }
  }

  private static final class RealtimeCentralModel extends AbstractModel<Parcel>
    implements TickListener, Scheduler {
    private boolean problemHasChanged;
    private boolean assignmentHasChanged;
    Optional<ImmutableList<ImmutableList<? extends Parcel>>> schedule;

    private final PDPRoadModel roadModel;
    private final RealtimeSolver solver;
    private final RealTimeClockController clock;

    RealtimeCentralModel(RealTimeClockController c, RealtimeSolver s,
      PDPRoadModel rm) {
      problemHasChanged = false;
      assignmentHasChanged = false;
      clock = c;
      solver = s;
      roadModel = rm;
      schedule = Optional.absent();
      solver.init(this);
    }

    @Override
    public boolean register(Parcel element) {
      problemHasChanged = true;
      clock.switchToRealTime();
      return true;
    }

    @Override
    public boolean unregister(Parcel element) {
      return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void tick(TimeLapse timeLapse) {
      if (problemHasChanged) {
        problemHasChanged = false;
        // TODO check to see that this is called the first possible moment after
        // the add parcel event was dispatched

        // TODO it must be checked whether the calculated routes end up in the
        // correct vehicles

        final Set<RouteFollowingVehicle> vehicles = roadModel
          .getObjectsOfType(RouteFollowingVehicle.class);

        // gather current routes
        final ImmutableList.Builder<ImmutableList<Parcel>> currentRouteBuilder = ImmutableList
          .builder();
        for (final RouteFollowingVehicle vehicle : vehicles) {
          final ImmutableList<Parcel> l = ImmutableList.copyOf(vehicle
            .getRoute());
          currentRouteBuilder.add(l);
        }

        solver.receiveSnapshot(snapshot);

        // final Iterator<Queue<Parcel>> routes = solver.solve(
        // SolveArgs.create()
        // .useAllParcels()
        // .useCurrentRoutes(currentRouteBuilder.build()))
        // .iterator();
        //
        // for (final RouteFollowingVehicle vehicle : vehicles) {
        // vehicle.setRoute(routes.next());
        // }
      }

      if (assignmentHasChanged) {
        final Set<RouteFollowingVehicle> vehicles = roadModel
          .getObjectsOfType(RouteFollowingVehicle.class);

        Iterator<ImmutableList<? extends Parcel>> routes = schedule.get()
          .iterator();
        for (final RouteFollowingVehicle vehicle : vehicles) {
          vehicle.setRoute(routes.next());
        }
      }

    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

    @Override
    public void updateSchedule(
      ImmutableList<ImmutableList<? extends Parcel>> routes) {
      assignmentHasChanged = true;
      schedule = Optional.of(routes);
    }

    @Override
    public List<List<Parcel>> getCurrentSchedule() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void doneForNow() {
      clock.switchToSimulatedTime();

    }

  }
}
