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
package com.github.rinde.rinsim.examples.experiment;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.graphics.RGB;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.pdptw.common.RoutePanel;
import com.github.rinde.rinsim.pdptw.common.RouteRenderer;
import com.github.rinde.rinsim.pdptw.common.StatsPanel;
import com.github.rinde.rinsim.pdptw.common.StatsStopConditions;
import com.github.rinde.rinsim.pdptw.common.StatsTracker;
import com.github.rinde.rinsim.pdptw.common.TimeLinePanel;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;

/**
 * This example shows how to use the {@link Experiment} class to define and run
 * an experiment. It shows how to construct a {@link Scenario} and
 * {@link MASConfiguration} which are both requirements for an example. The
 * intermediate steps in the example are documented, however, make sure to also
 * read the documentation of each method for extra information about what it
 * does.
 * <p>
 * If this class is run on MacOS it might be necessary to use
 * -XstartOnFirstThread as a VM argument.
 * @author Rinde van Lon
 */
public final class ExperimentExample {
  // some constants used in the experiment
  private static final Point RESOLUTION = new Point(700, 600);
  private static final double VEHICLE_SPEED_KMH = 30d;
  private static final double MAX_VEHICLE_SPEED_KMH = 50d;
  private static final Point MIN_POINT = new Point(0, 0);
  private static final Point MAX_POINT = new Point(8, 4);
  private static final Point DEPOT_LOC = new Point(5, 2);
  private static final Point P1_PICKUP = new Point(1, 2);
  private static final Point P1_DELIVERY = new Point(4, 2);
  private static final Point P2_PICKUP = new Point(1, 1);
  private static final Point P2_DELIVERY = new Point(4, 1);
  private static final Point P3_PICKUP = new Point(1, 3);
  private static final Point P3_DELIVERY = new Point(4, 3);

  private static final long M1 = 60 * 1000L;
  private static final long M4 = 4 * 60 * 1000L;
  private static final long M5 = 5 * 60 * 1000L;
  private static final long M7 = 7 * 60 * 1000L;
  private static final long M10 = 10 * 60 * 1000L;
  private static final long M12 = 12 * 60 * 1000L;
  private static final long M13 = 13 * 60 * 1000L;
  private static final long M18 = 18 * 60 * 1000L;
  private static final long M20 = 20 * 60 * 1000L;
  private static final long M25 = 25 * 60 * 1000L;
  private static final long M30 = 30 * 60 * 1000L;
  private static final long M40 = 40 * 60 * 1000L;
  private static final long M60 = 60 * 60 * 1000L;

  private static final RGB GRAY = new RGB(200, 200, 200);
  private static final RGB BLUE = new RGB(0, 0, 255);
  private static final RGB PURPLE = new RGB(255, 0, 255);

  private ExperimentExample() {}

  /**
   * It is possible to use the application arguments directly for configuring
   * the experiment. The '-h' or '--help' argument will show the list of
   * options.
   * @param args The arguments supplied to the application.
   */
  public static void main(String[] args) {
    int uiSpeedUp = 1;
    final int index = Arrays.binarySearch(args, "speedup");

    String[] arguments = args;
    if (index >= 0) {
      checkArgument(arguments.length > index + 1,
        "speedup option requires an integer indicating the speedup.");
      uiSpeedUp = Integer.parseInt(arguments[index + 1]);
      checkArgument(uiSpeedUp > 0, "speedup must be a positive integer.");
      final List<String> list = new ArrayList<>(Arrays.asList(arguments));
      list.remove(index + 1);
      list.remove(index);
      arguments = list.toArray(new String[] {});
    }

    final Optional<ExperimentResults> results;

    // Starts the experiment builder.
    results = Experiment.builder()

      // Adds a configuration to the experiment. A configuration configures an
      // algorithm that is supposed to handle or 'solve' a problem specified by
      // a scenario. A configuration can handle a scenario if it contains an
      // event handler for all events that occur in the scenario. The scenario
      // in this example contains four different events and registers an event
      // handler for each of them.
      .addConfiguration(MASConfiguration.builder()
        // NOTE: this example uses 'namedHandler's for Depots and Parcels, while
        // very useful for debugging these should not be used in production as
        // these are not thread safe. Use the 'defaultHandler()' instead.
        .addEventHandler(AddDepotEvent.class, AddDepotEvent.namedHandler())
        .addEventHandler(AddParcelEvent.class, AddParcelEvent.namedHandler())
        // There is no default handle for vehicle events, here a non functioning
        // handler is added, it can be changed to add a custom vehicle to the
        // simulator.
        .addEventHandler(AddVehicleEvent.class, CustomVehicleHandler.INSTANCE)
        .addEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())

        // Note: if you multi-agent system requires the aid of a model (e.g.
        // CommModel) it can be added directly in the configuration. Models that
        // are only used for the solution side should not be added in the
        // scenario as they are not part of the problem.
        .addModel(StatsTracker.builder())
        .build())

      // Adds the newly constructed scenario to the experiment. Every
      // configuration will be run on every scenario.
      .addScenario(createScenario())

      // The number of repetitions for each simulation. Each repetition will
      // have a unique random seed that is given to the simulator.
      .repeat(2)

      // The master random seed from which all random seeds for the
      // simulations will be drawn.
      .withRandomSeed(0)

      // The number of threads the experiment will use, this allows to run
      // several simulations in parallel. Note that when the GUI is used the
      // number of threads must be set to 1.
      .withThreads(1)

      // We add a post processor to the experiment. A post processor can read
      // the state of the simulator after it has finished. It can be used to
      // gather simulation results. The objects created by the post processor
      // end up in the ExperimentResults object that is returned by the
      // perform(..) method of Experiment.
      .usePostProcessor(new ExamplePostProcessor())

      // Adds the GUI just like it is added to a Simulator object.
      .showGui(View.builder()
        .with(PlaneRoadModelRenderer.builder())
        .with(RoadUserRenderer.builder()
          .withToStringLabel()
          .withColorAssociation(Depot.class, GRAY)
          .withColorAssociation(Parcel.class, BLUE)
          .withColorAssociation(Vehicle.class, PURPLE))
        .with(TimeLinePanel.builder())
        .with(RouteRenderer.builder())
        .with(RoutePanel.builder().withPositionLeft())
        .with(StatsPanel.builder())
        .withResolution((int) RESOLUTION.x, (int) RESOLUTION.y)
        .withAutoPlay()
        .withAutoClose()
        // For testing we allow to change the speed up via the args.
        .withSpeedUp(uiSpeedUp)
        .withTitleAppendix("Experiment example"))

      // Starts the experiment, but first reads the command-line arguments
      // that are specified for this application. By supplying the '-h' option
      // you can see an overview of the supported options.
      .perform(System.out, arguments);

    if (results.isPresent()) {

      for (final SimulationResult sr : results.get().getResults()) {
        // The SimulationResult contains all information about a specific
        // simulation, the result object is the object created by the post
        // processor, a String in this case.
        System.out.println(
          sr.getSimArgs().getRandomSeed() + " " + sr.getResultObject());
      }
    } else {
      throw new IllegalStateException("Experiment did not complete.");
    }
  }

  /**
   * Defines a simple scenario with one depot, one vehicle and three parcels.
   * Note that a scenario is supposed to only contain problem specific
   * information it should (generally) not make any assumptions about the
   * algorithm(s) that are used to solve the problem.
   * @return A newly constructed scenario.
   */
  static Scenario createScenario() {
    // In essence a scenario is just a list of events. The events must implement
    // the TimedEvent interface. You are free to construct any object as a
    // TimedEvent but keep in mind that implementations should be immutable.
    return Scenario.builder()

      // Adds one depot.
      .addEvent(AddDepotEvent.create(-1, DEPOT_LOC))

      // Adds one vehicle.
      .addEvent(AddVehicleEvent.create(-1, VehicleDTO.builder()
        .speed(VEHICLE_SPEED_KMH)
        .build()))

      // Three add parcel events are added. They are announced at different
      // times and have different time windows.
      .addEvent(
        AddParcelEvent.create(Parcel.builder(P1_PICKUP, P1_DELIVERY)
          .neededCapacity(0)
          .orderAnnounceTime(M1)
          .pickupTimeWindow(TimeWindow.create(M1, M20))
          .deliveryTimeWindow(TimeWindow.create(M4, M30))
          .buildDTO()))

      .addEvent(
        AddParcelEvent.create(Parcel.builder(P2_PICKUP, P2_DELIVERY)
          .neededCapacity(0)
          .orderAnnounceTime(M5)
          .pickupTimeWindow(TimeWindow.create(M10, M25))
          .deliveryTimeWindow(
            TimeWindow.create(M20, M40))
          .buildDTO()))

      .addEvent(
        AddParcelEvent.create(Parcel.builder(P3_PICKUP, P3_DELIVERY)
          .neededCapacity(0)
          .orderAnnounceTime(M7)
          .pickupTimeWindow(TimeWindow.create(M12, M18))
          .deliveryTimeWindow(
            TimeWindow.create(M13, M60))
          .buildDTO()))

      // Signals the end of the scenario. Note that it is possible to stop the
      // simulation before or after this event is dispatched, that depends on
      // the stop condition (see below).
      .addEvent(TimeOutEvent.create(M60))
      .scenarioLength(M60)

      // Adds a plane road model as this is part of the problem
      .addModel(
        PDPRoadModel.builder(
          RoadModelBuilders.plane()
            .withMinPoint(MIN_POINT)
            .withMaxPoint(MAX_POINT)
            .withMaxSpeed(MAX_VEHICLE_SPEED_KMH)))

      // Adds the pdp model
      .addModel(
        DefaultPDPModel.builder()
          .withTimeWindowPolicy(TimeWindowPolicies.TARDY_ALLOWED))

      // The stop condition indicates when the simulator should stop the
      // simulation. Typically this is the moment when all tasks are performed.
      // Custom stop conditions can be created by implementing the StopCondition
      // interface.
      .setStopCondition(StopConditions.or(
        StatsStopConditions.timeOutEvent(),
        StatsStopConditions.vehiclesDoneAndBackAtDepot()))
      .build();
  }

  enum CustomVehicleHandler implements TimedEventHandler<AddVehicleEvent> {
    INSTANCE {
      @Override
      public void handleTimedEvent(AddVehicleEvent event, SimulatorAPI sim) {
        // add your own vehicle to the simulator here
        sim.register(
          new ExampleRouteFollowingVehicle(event.getVehicleDTO(), true));
      }
    }
  }
}
