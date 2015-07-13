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
package com.github.rinde.rinsim.examples.core.taxi;

import static com.google.common.collect.Maps.newHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.examples.core.taxi.TaxiRenderer.Language;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.io.DotGraphIO;
import com.github.rinde.rinsim.geom.io.Filters;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;

/**
 * Example showing a fleet of taxis that have to pickup and transport customers
 * around the city of Leuven.
 * <p>
 * If this class is run on MacOS it might be necessary to use
 * -XstartOnFirstThread as a VM argument.
 * @author Rinde van Lon
 */
public final class TaxiExample {

  private static final int NUM_DEPOTS = 1;
  private static final int NUM_TAXIS = 20;
  private static final int NUM_CUSTOMERS = 30;

  // time in ms
  private static final long SERVICE_DURATION = 60000;
  private static final int TAXI_CAPACITY = 10;
  private static final int DEPOT_CAPACITY = 100;

  private static final String MAP_FILE = "/data/maps/leuven-simple.dot";
  private static final Map<String, Graph<MultiAttributeData>> GRAPH_CACHE =
      newHashMap();

  private TaxiExample() {}

  /**
   * Starts the {@link TaxiExample}.
   * @param args The first option may optionally indicate the end time of the
   *          simulation.
   */
  public static void main(@Nullable String[] args) {
    final long endTime = args != null && args.length >= 1 ? Long
        .parseLong(args[0]) : Long.MAX_VALUE;

    final String graphFile = args != null && args.length >= 2 ? args[1]
        : MAP_FILE;
    run(false, endTime, graphFile, null /* new Display() */, null, null);
  }

  /**
   * Run the example.
   * @param testing If <code>true</code> enables the test mode.
   */
  public static void run(boolean testing) {
    run(testing, Long.MAX_VALUE, MAP_FILE, null, null, null);
  }

  /**
   * Starts the example.
   * @param testing Indicates whether the method should run in testing mode.
   * @param endTime The time at which simulation should stop.
   * @param graphFile The graph that should be loaded.
   * @param display The display that should be used to show the ui on.
   * @param m The monitor that should be used to show the ui on.
   * @param list A listener that will receive callbacks from the ui.
   * @return The simulator instance.
   */
  public static Simulator run(boolean testing, final long endTime,
      String graphFile,
      @Nullable Display display, @Nullable Monitor m, @Nullable Listener list) {

    View.Builder view = View.builder()
        .with(GraphRoadModelRenderer.builder())
        .with(RoadUserRenderer.builder()
            .withImageAssociation(
                TaxiBase.class, "/graphics/perspective/tall-building-64.png")
            .withImageAssociation(
                Taxi.class, "/graphics/flat/taxi-32.png")
            .withImageAssociation(
                Customer.class, "/graphics/flat/person-red-32.png"))
        .with(TaxiRenderer.builder(Language.ENGLISH))
        .withTitleAppendix("Taxi Demo");

    if (testing) {
      view = view.withAutoClose()
          .withAutoPlay()
          .withSimulatorEndTime(20 * 60 * 1000)
          .withSpeedUp(64);
    } else if (m != null && list != null && display != null) {
      view = view.withMonitor(m)
          .withSpeedUp(4)
          .withResolution(m.getClientArea().width, m.getClientArea().height)
          .withDisplay(display)
          .withCallback(list)
          .withAsync()
          .withAutoPlay()
          .withAutoClose();
    }

    // use map of leuven
    final Simulator simulator = Simulator.builder()
        .addModel(RoadModelBuilders.staticGraph(loadGraph(graphFile)))
        .addModel(DefaultPDPModel.builder())
        .addModel(view)
        .build();
    final RandomGenerator rng = simulator.getRandomGenerator();

    final RoadModel roadModel = simulator.getModelProvider().getModel(
        RoadModel.class);
    // add depots, taxis and parcels to simulator
    for (int i = 0; i < NUM_DEPOTS; i++) {
      simulator.register(new TaxiBase(roadModel.getRandomPosition(rng),
          DEPOT_CAPACITY));
    }
    for (int i = 0; i < NUM_TAXIS; i++) {
      simulator.register(new Taxi(roadModel.getRandomPosition(rng),
          TAXI_CAPACITY));
    }
    for (int i = 0; i < NUM_CUSTOMERS; i++) {
      simulator.register(new Customer(
          Parcel.builder(roadModel.getRandomPosition(rng),
              roadModel.getRandomPosition(rng))
              .serviceDuration(SERVICE_DURATION)
              .neededCapacity(1 + rng.nextInt(3))
              .buildDTO()));
    }

    simulator.addTickListener(new TickListener() {
      @Override
      public void tick(TimeLapse time) {
        if (time.getStartTime() > endTime) {
          simulator.stop();
        } else if (rng.nextDouble() < .007) {
          simulator.register(new Customer(
              Parcel
                  .builder(roadModel.getRandomPosition(rng),
                      roadModel.getRandomPosition(rng))
                  .serviceDuration(SERVICE_DURATION)
                  .neededCapacity(1 + rng.nextInt(3))
                  .buildDTO()));
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });
    simulator.start();

    return simulator;
  }

  /**
   * A customer with very permissive time windows.
   */
  static class Customer extends Parcel {
    Customer(ParcelDTO dto) {
      super(dto);
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}
  }

  // currently has no function
  static class TaxiBase extends Depot {
    TaxiBase(Point position, double capacity) {
      super(position);
      setCapacity(capacity);
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}
  }

  // load the graph file
  static Graph<MultiAttributeData> loadGraph(String name) {
    try {
      if (GRAPH_CACHE.containsKey(name)) {
        return GRAPH_CACHE.get(name);
      }
      final Graph<MultiAttributeData> g = DotGraphIO
          .getMultiAttributeGraphIO(
              Filters.selfCycleFilter())
          .read(
              TaxiExample.class.getResourceAsStream(name));

      GRAPH_CACHE.put(name, g);
      return g;
    } catch (final FileNotFoundException e) {
      throw new IllegalStateException(e);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
