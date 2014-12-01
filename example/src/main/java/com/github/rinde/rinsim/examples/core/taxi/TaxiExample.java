/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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
import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.examples.core.taxi.TaxiRenderer.Language;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.serializers.DotGraphSerializer;
import com.github.rinde.rinsim.serializers.SelfCycleFilter;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.ui.renderers.UiSchema;
import com.github.rinde.rinsim.util.TimeWindow;

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
  private static final Map<String, Graph<MultiAttributeData>> GRAPH_CACHE = newHashMap();

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
   * @param endTime
   * @param graphFile
   * @param display
   * @param m
   * @param list
   * @return
   */
  public static Simulator run(boolean testing, final long endTime,
      String graphFile,
      @Nullable Display display, @Nullable Monitor m, @Nullable Listener list) {

    // create a new simulator
    final RandomGenerator rng = new MersenneTwister(123);
    final Simulator simulator = new Simulator(rng, Measure.valueOf(1000L,
        SI.MILLI(SI.SECOND)));

    // use map of leuven
    final RoadModel roadModel = new GraphRoadModel(loadGraph(graphFile));
    final DefaultPDPModel pdpModel = new DefaultPDPModel();

    // configure simulator with models
    simulator.register(roadModel);
    simulator.register(pdpModel);
    simulator.configure();

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
      simulator.register(new Customer(roadModel.getRandomPosition(rng),
          roadModel.getRandomPosition(rng), SERVICE_DURATION, SERVICE_DURATION,
          1 + rng.nextInt(3)));
    }

    simulator.addTickListener(new TickListener() {
      @Override
      public void tick(TimeLapse time) {
        if (time.getStartTime() > endTime) {
          simulator.stop();
        } else if (rng.nextDouble() < .007) {
          simulator.register(new Customer(
              roadModel.getRandomPosition(rng), roadModel
                  .getRandomPosition(rng), SERVICE_DURATION, SERVICE_DURATION,
              1 + rng.nextInt(3)));
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });

    final UiSchema uis = new UiSchema();
    uis.add(TaxiBase.class, "/graphics/perspective/tall-building-64.png");
    uis.add(Taxi.class, "/graphics/flat/taxi-32.png");
    uis.add(Customer.class, "/graphics/flat/person-red-32.png");
    final View.Builder view = View.create(simulator)
        .with(new GraphRoadModelRenderer())
        .with(new RoadUserRenderer(uis, false))
        .with(new TaxiRenderer(Language.ENGLISH))
        .setTitleAppendix("Taxi Demo");

    if (testing) {
      view.enableAutoClose()
          .enableAutoPlay()
          .stopSimulatorAtTime(60 * 60 * 1000)
          .setSpeedUp(64);
    }
    else if (m != null && list != null) {
      view.displayOnMonitor(m)
          .setSpeedUp(4)
          .setResolution(m.getClientArea().width, m.getClientArea().height)
          .setDisplay(display)
          .setCallback(list)
          .setAsync()
          .enableAutoPlay()
          .enableAutoClose();
    }

    view.show();
    return simulator;
  }

  /**
   * A customer with very permissive time windows.
   */
  static class Customer extends Parcel {
    Customer(Point startPosition, Point pDestination,
        long pLoadingDuration, long pUnloadingDuration, double pMagnitude) {
      super(pDestination, pLoadingDuration, TimeWindow.ALWAYS,
          pUnloadingDuration, TimeWindow.ALWAYS, pMagnitude);
      setStartPosition(startPosition);
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}
  }

  // currently has no function
  static class TaxiBase extends Depot {
    TaxiBase(Point position, double capacity) {
      setStartPosition(position);
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
      final Graph<MultiAttributeData> g = DotGraphSerializer
          .getMultiAttributeGraphSerializer(
              new SelfCycleFilter()).read(
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
