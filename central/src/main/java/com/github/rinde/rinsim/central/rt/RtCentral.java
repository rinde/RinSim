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

import java.util.Iterator;
import java.util.Set;

import com.github.rinde.rinsim.central.Central;
import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.core.model.CompositeModelBuilder;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
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
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * @author Rinde van Lon
 *
 */
public class RtCentral {

  private RtCentral() {}

  public static ModelBuilder<?, ?> builder(
    StochasticSupplier<? extends RealtimeSolver> solverSupplier) {
    return Builder.create(solverSupplier);
  }

  public static ModelBuilder<?, ?> builderAdapt(
    StochasticSupplier<? extends Solver> solverSupplier) {
    return builder(AdapterSupplier.create(solverSupplier));
  }

  public static MASConfiguration solverConfiguration(
    StochasticSupplier<? extends RealtimeSolver> solverSupplier,
    String nameSuffix) {
    return null;
  }

  public static MASConfiguration solverConfigurationAdapt(
    StochasticSupplier<? extends Solver> solverSupplier, String nameSuffix) {
    return null;
  }

  public static TimedEventHandler<AddVehicleEvent> vehicleHandler() {
    return Central.vehicleHandler();
  }

  public static final class RtCentralModel extends AbstractModel<Parcel>
    implements TickListener {
    private boolean problemHasChanged;

    private final PDPRoadModel roadModel;
    private final RtSimSolver solver;
    private final RealTimeClockController clock;

    RtCentralModel(RealTimeClockController c, RtSimSolver s, PDPRoadModel rm) {
      problemHasChanged = false;
      clock = c;
      solver = s;
      roadModel = rm;
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

    @Override
    public void tick(TimeLapse timeLapse) {
      if (problemHasChanged) {
        problemHasChanged = false;
        System.out.println("problem has changed " + timeLapse);
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

        solver.solve(
          SolveArgs.create().useCurrentRoutes(currentRouteBuilder.build()));

        try {
          Thread.sleep(timeLapse.getTickLength() / 2);
        } catch (InterruptedException e) {
          throw new IllegalStateException(e);
        }

        // TODO
        // wait for about 1 tick ? to immediately use result of solver if it is
        // that fast?
      }

      if (solver.isScheduleUpdated()) {
        System.out.println("solver has new schedule " + timeLapse);
        final Set<RouteFollowingVehicle> vehicles = roadModel
          .getObjectsOfType(RouteFollowingVehicle.class);

        ImmutableList<ImmutableList<Parcel>> schedule = solver
          .getCurrentSchedule();

        checkArgument(schedule.size() == vehicles.size(),
          "An invalid schedule was created, a valid schedule should contain one "
            + "route for each vehicle, routes: %s, vehicles: %s.",
          schedule.size(), vehicles.size());

        Iterator<ImmutableList<Parcel>> routes = schedule.iterator();
        System.out.println(Joiner.on("\n").join(schedule));
        for (final RouteFollowingVehicle vehicle : vehicles) {
          // TODO how to avoid some parcels getting lost? -> central check?

          vehicle.setRouteSafe(routes.next());
        }
      }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}
  }

  @AutoValue
  public abstract static class Builder
    extends AbstractModelBuilder<RtCentral.RtCentralModel, Parcel>
    implements CompositeModelBuilder<RtCentral.RtCentralModel, Parcel> {

    Builder() {
      setDependencies(RandomProvider.class, RtSimSolverBuilder.class,
        RealTimeClockController.class, PDPRoadModel.class);
    }

    @SuppressWarnings("unchecked")
    static ModelBuilder<?, ?> create(
      StochasticSupplier<? extends RealtimeSolver> solverSupplier) {
      return new AutoValue_RtCentral_Builder(
        (StochasticSupplier<RealtimeSolver>) solverSupplier);
    }

    public abstract StochasticSupplier<RealtimeSolver> getSolverSupplier();

    @Override
    public RtCentral.RtCentralModel build(
      DependencyProvider dependencyProvider) {
      RandomProvider rnd = dependencyProvider.get(RandomProvider.class);
      RealtimeSolver solver = getSolverSupplier()
        .get(rnd.masterInstance().nextLong());
      RtSimSolver s = dependencyProvider.get(RtSimSolverBuilder.class)
        .build(solver);
      RealTimeClockController clock = dependencyProvider
        .get(RealTimeClockController.class);
      PDPRoadModel rm = dependencyProvider.get(PDPRoadModel.class);
      return new RtCentral.RtCentralModel(clock, s, rm);
    }

    @Override
    public ImmutableSet<ModelBuilder<?, ?>> getChildren() {
      return ImmutableSet.<ModelBuilder<?, ?>> of(RtSolverModel.builder());
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
  }
}
