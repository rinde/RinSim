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
import static com.google.common.collect.Sets.newHashSet;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

import com.github.rinde.rinsim.central.Central;
import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.CompositeModelBuilder;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Real-time version of {@link com.github.rinde.rinsim.central.Central Central}.
 * 
 * TODO write full doc
 * @author Rinde van Lon
 */
public final class RtCentral {

  private RtCentral() {}

  /**
   * Constructs a {@link Builder} for creating a real-time central model. The
   * real-time central model is a model that takes control of all vehicles in a
   * simulation and uses a {@link RealtimeSolver} to compute routes for all
   * vehicles.
   * @param solverSupplier A {@link StochasticSupplier} that creates the
   *          {@link RealtimeSolver} that will be used for controlling all
   *          vehicles.
   * @return A new {@link Builder} instance.
   */
  public static Builder builder(
    StochasticSupplier<? extends RealtimeSolver> solverSupplier) {
    return Builder.create(solverSupplier, true);
  }

  public static ModelBuilder<?, ?> builderAdapt(
    StochasticSupplier<? extends Solver> solverSupplier) {
    return builder(AdapterSupplier.create(solverSupplier));
  }

  /**
   * Constructs a new {@link MASConfiguration} that uses a
   * {@link RealtimeSolver} created by the specified <code>solverSupplier</code>
   * to control all vehicles.
   * @param solverSupplier A {@link StochasticSupplier} that should create
   *          instances of {@link RealtimeSolver}.
   * @param nameSuffix The suffix to the name of the {@link MASConfiguration}.
   *          The name is formatted as
   *          <code>RtCentral-[solverSupplier][nameSuffix]</code>.
   * @return A new {@link MASConfiguration} instance.
   */
  public static MASConfiguration solverConfiguration(
    StochasticSupplier<? extends RealtimeSolver> solverSupplier,
    String nameSuffix) {
    return MASConfiguration.pdptwBuilder()
      .addModel(builder(solverSupplier))
      .addEventHandler(AddVehicleEvent.class, vehicleHandler())
      .setName(String.format("RtCentral-%s%s", solverSupplier, nameSuffix))
      .build();
  }

  public static MASConfiguration solverConfigurationAdapt(
    StochasticSupplier<? extends Solver> solverSupplier, String nameSuffix) {
    return solverConfiguration(AdapterSupplier.create(solverSupplier),
      nameSuffix);
  }

  public static TimedEventHandler<AddVehicleEvent> vehicleHandler() {
    return VehicleCreator.INSTANCE;
  }

  enum VehicleCreator implements TimedEventHandler<AddVehicleEvent> {
    INSTANCE {
      @Override
      public void handleTimedEvent(AddVehicleEvent event, SimulatorAPI sim) {
        sim.register(new RouteFollowingVehicle(event.getVehicleDTO(), true));
      }

      @Override
      public String toString() {
        return Central.class.getName() + ".vehicleHandler()";
      }
    }
  }

  static final class RtCentralModel extends AbstractModel<Parcel>
    implements TickListener {
    private boolean problemHasChanged;

    private final PDPRoadModel roadModel;
    private final PDPModel pdpModel;
    private final RtSimSolver solver;
    private final RealTimeClockController clock;
    private final boolean sleepOnChange;

    RtCentralModel(RealTimeClockController c, RtSimSolver s, PDPRoadModel rm,
      PDPModel pm, boolean sleepOnC) {
      problemHasChanged = false;
      clock = c;
      solver = s;
      roadModel = rm;
      pdpModel = pm;
      sleepOnChange = sleepOnC;
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

    void notifySolverOfChange(TimeLapse timeLapse, boolean sleepAfterNotify) {
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

      solver.solve(
        SolveArgs.create().useCurrentRoutes(currentRouteBuilder.build()));

      if (sleepAfterNotify) {
        try {
          Thread.sleep(timeLapse.getTickLength() / 2);
        } catch (InterruptedException e) {
          throw new IllegalStateException(e);
        }
      }
    }

    @Override
    public void tick(TimeLapse timeLapse) {
      if (problemHasChanged) {
        problemHasChanged = false;
        notifySolverOfChange(timeLapse, sleepOnChange);
      }

      if (solver.isScheduleUpdated()) {
        final Set<RouteFollowingVehicle> vehicles = roadModel
          .getObjectsOfType(RouteFollowingVehicle.class);

        ImmutableList<ImmutableList<Parcel>> schedule = solver
          .getCurrentSchedule();

        checkArgument(schedule.size() == vehicles.size(),
          "An invalid schedule was created, a valid schedule should contain one"
            + " route for each vehicle, routes: %s, vehicles: %s.",
          schedule.size(), vehicles.size());

        Iterator<ImmutableList<Parcel>> routes = schedule.iterator();
        boolean inconsistencyDetected = false;
        for (final RouteFollowingVehicle vehicle : vehicles) {
          vehicle.setRouteSafe(routes.next());

          Set<Parcel> contents = pdpModel.getContents(vehicle);
          Set<Parcel> routeSet = newHashSet(vehicle.getRoute());
          for (Parcel p : contents) {
            if (!routeSet.contains(p)) {
              inconsistencyDetected = true;
              break;
            }
          }
        }

        if (inconsistencyDetected) {
          // launch solver again
          notifySolverOfChange(timeLapse, false);
        }
      }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}
  }

  @AutoValue
  public abstract static class Builder
    extends AbstractModelBuilder<RtCentralModel, Parcel>
    implements CompositeModelBuilder<RtCentralModel, Parcel>, Serializable {

    Builder() {
      setDependencies(
        RandomProvider.class,
        RtSimSolverBuilder.class,
        RealTimeClockController.class,
        PDPRoadModel.class,
        PDPModel.class);
    }

    @SuppressWarnings("unchecked")
    static Builder create(
      StochasticSupplier<? extends RealtimeSolver> solverSupplier,
      boolean sleepOnChange) {
      return new AutoValue_RtCentral_Builder(
        (StochasticSupplier<RealtimeSolver>) solverSupplier, sleepOnChange);
    }

    public Builder withSleepOnChange(boolean flag) {
      return create(getSolverSupplier(), flag);
    }

    abstract StochasticSupplier<RealtimeSolver> getSolverSupplier();

    abstract boolean getSleepOnChange();

    @Override
    public RtCentralModel build(
      DependencyProvider dependencyProvider) {
      RandomProvider rnd = dependencyProvider.get(RandomProvider.class);
      RealtimeSolver solver = getSolverSupplier()
        .get(rnd.masterInstance().nextLong());
      RtSimSolver s = dependencyProvider.get(RtSimSolverBuilder.class)
        .build(solver);
      RealTimeClockController clock = dependencyProvider
        .get(RealTimeClockController.class);
      PDPRoadModel rm = dependencyProvider.get(PDPRoadModel.class);
      PDPModel pm = dependencyProvider.get(PDPModel.class);
      return new RtCentral.RtCentralModel(clock, s, rm, pm, getSleepOnChange());
    }

    @Override
    public ImmutableSet<ModelBuilder<?, ?>> getChildren() {
      return ImmutableSet.<ModelBuilder<?, ?>> of(RtSolverModel.builder(),
        VehicleCheckerModel.builder());
    }

    @Override
    public String toString() {
      return RtCentral.class.getSimpleName() + ".builder(..)";
    }
  }

  static class VehicleCheckerModel
    extends AbstractModel<RouteFollowingVehicle> {

    @Override
    public boolean register(RouteFollowingVehicle element) {
      checkArgument(element.isDelayedRouteChangingAllowed(),
        "%s requires that all registered %s instances allow delayed route "
          + "changing",
        RtCentral.class.getSimpleName(),
        RouteFollowingVehicle.class.getSimpleName());
      return true;
    }

    @Override
    public boolean unregister(RouteFollowingVehicle element) {
      return true;
    }

    static Builder builder() {
      return new AutoValue_RtCentral_VehicleCheckerModel_Builder();
    }

    @AutoValue
    abstract static class Builder
      extends AbstractModelBuilder<VehicleCheckerModel, RouteFollowingVehicle> {

      @Override
      public VehicleCheckerModel build(DependencyProvider dependencyProvider) {
        return new VehicleCheckerModel();
      }
    }
  }

  @AutoValue
  abstract static class AdapterSupplier
    implements StochasticSupplier<RealtimeSolver> {

    abstract StochasticSupplier<Solver> getSolverSupplier();

    @Override
    public RealtimeSolver get(long seed) {
      return new SolverToRealtimeAdapter(getSolverSupplier().get(seed));
    }

    @SuppressWarnings("unchecked")
    static AdapterSupplier create(StochasticSupplier<? extends Solver> ss) {
      return new AutoValue_RtCentral_AdapterSupplier(
        (StochasticSupplier<Solver>) ss);
    }

    @Override
    public String toString() {
      return String.format("RtAdapter{%s}", getSolverSupplier());
    }
  }
}
