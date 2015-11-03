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
import static com.google.common.base.Verify.verify;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.central.Central;
import com.github.rinde.rinsim.central.GlobalStateObject;
import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.central.rt.RtCentral.Builder.Options;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.CompositeModelBuilder;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType;
import com.github.rinde.rinsim.core.model.pdp.PDPModelEvent;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
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
 * Real-time version of {@link com.github.rinde.rinsim.central.Central Central}.
 * This class provides methods to add a model to a simulator or a
 * {@link MASConfiguration} to an experiment that allows a {@link Solver} or
 * {@link RealtimeSolver} to compute an entire scenario centrally.
 * @author Rinde van Lon
 */
public final class RtCentral {

  private static final ImmutableSet<Options> DEFAULT_OPTIONS =
    ImmutableSet.of();
  private static final Logger LOGGER = LoggerFactory.getLogger(RtCentral.class);

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
    return Builder.create(solverSupplier, DEFAULT_OPTIONS);
  }

  /**
   * Constructs a {@link Builder} for creating a real-time central model. The
   * real-time central model is a model that takes control of all vehicles in a
   * simulation and uses a {@link RealtimeSolver} to compute routes for all
   * vehicles. The supplied {@link Solver} is wrapped by a
   * {@link SolverToRealtimeAdapter}.
   * @param solverSupplier A {@link StochasticSupplier} that creates the
   *          {@link Solver} that will be used for controlling all vehicles.
   * @return A new {@link Builder} instance.
   */
  public static Builder builderAdapt(
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
  // TODO create builder

  /**
   * Constructs a new {@link MASConfiguration} that uses a {@link Solver}
   * created by the specified <code>solverSupplier</code> to control all
   * vehicles. The {@link Solver} is converted to a {@link RealtimeSolver} using
   * {@link SolverToRealtimeAdapter}.
   * @param solverSupplier A {@link StochasticSupplier} that should create
   *          instances of {@link Solver}.
   * @param nameSuffix The suffix to the name of the {@link MASConfiguration}.
   *          The name is formatted as
   *          <code>RtCentral-[solverSupplier][nameSuffix]</code>.
   * @return A new {@link MASConfiguration} instance.
   */
  public static MASConfiguration solverConfigurationAdapt(
      StochasticSupplier<? extends Solver> solverSupplier, String nameSuffix) {
    return solverConfiguration(AdapterSupplier.create(solverSupplier),
      nameSuffix);
  }

  public static MASConfiguration solverConfigurationAdapt(
      StochasticSupplier<? extends Solver> solverSupplier, String nameSuffix,
      boolean threadGrouping) {
    return MASConfiguration.pdptwBuilder()
        .addModel(builder(AdapterSupplier.create(solverSupplier))
            .withThreadGrouping(threadGrouping))
        .addEventHandler(AddVehicleEvent.class, vehicleHandler())
        .setName(String.format("RtCentral-%s%s-", solverSupplier, nameSuffix))
        .build();
  }

  public static TimedEventHandler<AddVehicleEvent> vehicleHandler() {
    return VehicleCreator.INSTANCE;
  }

  /**
   * Builder for central model.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder
      extends AbstractModelBuilder<RtCentralModel, Parcel>
      implements CompositeModelBuilder<RtCentralModel, Parcel>, Serializable {

    private static final long serialVersionUID = -3900188597329698413L;

    Builder() {
      setDependencies(
        RandomProvider.class,
        RtSimSolverBuilder.class,
        RealtimeClockController.class,
        PDPRoadModel.class,
        PDPModel.class);
    }

    abstract StochasticSupplier<RealtimeSolver> getSolverSupplier();

    enum Options {
      SLEEP_ON_CHANGE, THREAD_GROUPING, CONTINUOUS_UPDATES;
    }

    abstract ImmutableSet<Options> getOptions();

    /**
     * If set to <code>true</code> the model will wait half a tick (wall clock
     * time) after it has started a new computation. By sleeping half a tick the
     * solver has time to do some computations already, and, if it is fast
     * enough if may already be finished. When it is already finished computing,
     * the new schedule will be applied immediately. If set to
     * <code>false</code> the model will not wait, this means that a new
     * schedule will be applied at the earliest the tick after it started the
     * computation. By default sleep on change is set to <code>false</code>.
     * @param flag If set to <code>true</code> sleep on change is enabled,
     *          otherwise it is disabled.
     * @return A new builder instance with the flag.
     */
    public Builder withSleepOnChange(boolean flag) {
      return change(Options.SLEEP_ON_CHANGE, flag);
    }

    public Builder withThreadGrouping(boolean flag) {
      return change(Options.THREAD_GROUPING, flag);
    }

    public Builder withContinuousUpdates(boolean flag) {
      return change(Options.CONTINUOUS_UPDATES, flag);
    }

    Builder change(Options option, boolean toAdd) {
      final Set<Options> opts = new LinkedHashSet<>(getOptions());
      if (toAdd) {
        opts.add(option);
      } else {
        opts.remove(option);
      }
      return create(getSolverSupplier(), opts);
    }

    @Override
    public RtCentralModel build(
        DependencyProvider dependencyProvider) {
      final RandomProvider rnd = dependencyProvider.get(RandomProvider.class);
      final RealtimeSolver solver = getSolverSupplier()
          .get(rnd.masterInstance().nextLong());
      final RtSimSolver s = dependencyProvider.get(RtSimSolverBuilder.class)
          .build(solver);
      final RealtimeClockController clock = dependencyProvider
          .get(RealtimeClockController.class);
      final PDPRoadModel rm = dependencyProvider.get(PDPRoadModel.class);
      final PDPModel pm = dependencyProvider.get(PDPModel.class);
      return new RtCentral.RtCentralModel(clock, s, rm, pm, getOptions());
    }

    @Override
    public ImmutableSet<ModelBuilder<?, ?>> getChildren() {
      return ImmutableSet.<ModelBuilder<?, ?>>of(
        RtSolverModel.builder().withThreadGrouping(
          getOptions().contains(Options.THREAD_GROUPING)));
    }

    @Override
    public String toString() {
      return RtCentral.class.getSimpleName() + ".builder(..)";
    }

    @SuppressWarnings("unchecked")
    static Builder create(
        StochasticSupplier<? extends RealtimeSolver> solverSupplier,
        Set<Options> options) {
      return new AutoValue_RtCentral_Builder(
          (StochasticSupplier<RealtimeSolver>) solverSupplier,
          ImmutableSet.copyOf(options));
    }
  }

  enum VehicleChecker implements Listener {
    INSTANCE {
      @Override
      public void handleEvent(Event e) {
        verify(e instanceof PDPModelEvent);
        final PDPModelEvent event = (PDPModelEvent) e;
        final Vehicle v = event.vehicle;
        checkArgument(v instanceof RouteFollowingVehicle,
          "%s requires that all registered vehicles are a subclass of %s.",
          RtCentral.class.getSimpleName(),
          RouteFollowingVehicle.class.getSimpleName());

        final RouteFollowingVehicle vehicle = (RouteFollowingVehicle) v;
        checkArgument(vehicle.isDelayedRouteChangingAllowed(),
          "%s requires that all registered %s instances allow delayed route "
              + "changing",
          RtCentral.class.getSimpleName(),
          RouteFollowingVehicle.class.getSimpleName());
      }
    }
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
    private final RtSimSolver solver;
    private final RealtimeClockController clock;
    private final boolean sleepOnChange;
    private final List<RouteFollowingVehicle> vehicles;
    private final boolean continuousUpdates;

    RtCentralModel(RealtimeClockController c, RtSimSolver s, PDPRoadModel rm,
        PDPModel pm, ImmutableSet<Options> opts) {
      problemHasChanged = false;
      clock = c;
      solver = s;
      roadModel = rm;
      sleepOnChange = opts.contains(Options.SLEEP_ON_CHANGE);
      continuousUpdates = opts.contains(Options.CONTINUOUS_UPDATES);
      vehicles = new ArrayList<>();

      pm.getEventAPI().addListener(VehicleChecker.INSTANCE,
        PDPModelEventType.NEW_VEHICLE);
    }

    @Override
    public boolean register(Parcel element) {
      verify(clock.getClockMode() == ClockMode.REAL_TIME,
        "Problem detected at %s when registering %s, clock mode is %s.",
        clock.getCurrentTime(), element, clock.getClockMode());
      problemHasChanged = true;
      return true;
    }

    @Override
    public boolean unregister(Parcel element) {
      return false;
    }

    ImmutableList<ImmutableList<Parcel>> getCurrentRoutes() {
      // gather current routes
      final ImmutableList.Builder<ImmutableList<Parcel>> currentRouteBuilder =
        ImmutableList.builder();
      for (final RouteFollowingVehicle vehicle : vehicles) {
        final ImmutableList<Parcel> l =
          ImmutableList.copyOf(vehicle.getRoute());
        currentRouteBuilder.add(l);
      }
      return currentRouteBuilder.build();
    }

    void notifySolverOfChange(TimeLapse timeLapse, boolean sleepAfterNotify) {
      solver.solve(SolveArgs.create().useCurrentRoutes(getCurrentRoutes()));

      if (sleepAfterNotify) {
        try {
          Thread.sleep(timeLapse.getTickLength() / 2);
        } catch (final InterruptedException e) {
          throw new IllegalStateException(e);
        }
      }
    }

    @Override
    public void tick(TimeLapse timeLapse) {
      if (vehicles.isEmpty()) {
        vehicles.addAll(
          roadModel.getObjectsOfType(RouteFollowingVehicle.class));
        checkState(!vehicles.isEmpty(),
          "At least one vehicle must have been added to the simulator in order "
              + "for %s to work.",
          RtCentral.class.getSimpleName());
      }

      if (problemHasChanged) {
        verify(clock.getClockMode() == ClockMode.REAL_TIME,
          "Problem detected at %s.", timeLapse);
        problemHasChanged = false;
        notifySolverOfChange(timeLapse, sleepOnChange);
      } else if (continuousUpdates
          && clock.getClockMode() == ClockMode.REAL_TIME) {
        solver.sendSnapshot(
          SolveArgs.create().useCurrentRoutes(getCurrentRoutes()));
      }

      if (solver.isScheduleUpdated()) {
        final GlobalStateObject state =
          solver.getCurrentState(SolveArgs.create());

        final List<List<Parcel>> schedule =
          ScheduleUtil.fixSchedule(solver.getCurrentSchedule(), state);

        checkArgument(schedule.size() == vehicles.size(),
          "An invalid schedule was created, a valid schedule should contain "
              + "one route for each vehicle, routes: %s, vehicles: %s.",
          schedule.size(), vehicles.size());

        final Iterator<List<Parcel>> routes = schedule.iterator();
        int i = 0;
        for (final RouteFollowingVehicle vehicle : vehicles) {
          final List<Parcel> newRoute = routes.next();
          vehicle.setRoute(newRoute);
          final List<Parcel> actualRoute = new ArrayList<>(vehicle.getRoute());

          if (!actualRoute.equals(newRoute)) {
            LOGGER.warn("something went wrong");
            LOGGER.warn("state");
            LOGGER.warn("available: " + state.getAvailableParcels());
            LOGGER.warn("vehicles:" + Joiner.on(System.lineSeparator())
                .join(state.getVehicles()));
            LOGGER.warn("current schedule: ");
            LOGGER.warn(Joiner.on(System.lineSeparator())
                .join(solver.getCurrentSchedule()));
            LOGGER.warn("fixed");
            LOGGER.warn(Joiner.on(System.lineSeparator()).join(schedule));
            LOGGER.warn("problem in vehicle" + i);
            throw new IllegalStateException(
                "Route was not applied correctly, new route: " + newRoute
                    + " but result is: " + vehicle.getRoute());
          }
          i++;
        }
      }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}
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
