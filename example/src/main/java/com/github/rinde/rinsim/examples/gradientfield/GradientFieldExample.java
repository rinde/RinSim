/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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
package com.github.rinde.rinsim.examples.gradientfield;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.RouteRenderer;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Parser;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Scenario;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;

/**
 * Example of a gradient field MAS for the Gendreau et al. (2006) dataset.
 * <p>
 * If this class is run on MacOS it might be necessary to use
 * -XstartOnFirstThread as a VM argument.
 * @author David Merckx
 * @author Rinde van Lon
 */
public final class GradientFieldExample {
  static final long RANDOM_SEED = 123L;

  static final int TEST_SPEED_UP = 64;
  static final long TEST_END_TIME = 20 * 60 * 1000;

  private GradientFieldExample() {}

  /**
   * Runs the example.
   * @param args Ignored.
   */
  public static void main(String[] args) {
    run(false);
  }

  /**
   * Runs the example.
   * @param testing If <code>true</code> the example is run in testing mode,
   *          this means that it will automatically start and stop itself.
   */
  public static void run(final boolean testing) {
    View.Builder viewBuilder = View.builder()
      .withTitleAppendix("Gradient field example")
      .with(PlaneRoadModelRenderer.builder())
      .with(RoadUserRenderer.builder()
        .withImageAssociation(
          Truck.class, "/graphics/perspective/bus-44.png")
        .withImageAssociation(
          Depot.class, "/graphics/flat/warehouse-32.png")
        .withImageAssociation(
          GFParcel.class, "/graphics/flat/hailing-cab-32.png"))
      .with(GradientFieldRenderer.builder())
      .with(RouteRenderer.builder())
      .with(PDPModelRenderer.builder());

    if (testing) {
      viewBuilder = viewBuilder.withAutoClose()
        .withAutoPlay()
        .withSpeedUp(TEST_SPEED_UP)
        .withSimulatorEndTime(TEST_END_TIME);
    }

    final Gendreau06Scenario scenario = Gendreau06Parser
      .parser().addFile(GradientFieldExample.class
        .getResourceAsStream("/data/gendreau06/req_rapide_1_240_24"),
        "req_rapide_1_240_24")
      .allowDiversion()
      .parse().get(0);

    Experiment.builder()
      .withRandomSeed(RANDOM_SEED)
      .withThreads(1)
      .addConfiguration(MASConfiguration.pdptwBuilder()
        .setName("GradientFieldConfiguration")
        .addEventHandler(AddVehicleEvent.class, VehicleHandler.INSTANCE)
        .addEventHandler(AddParcelEvent.class, ParcelHandler.INSTANCE)
        .addModel(GradientModel.builder())
        .build())
      .addScenario(scenario)
      .showGui(viewBuilder)
      .repeat(1)
      .perform();
  }

  enum VehicleHandler implements TimedEventHandler<AddVehicleEvent> {
    INSTANCE {
      @Override
      public void handleTimedEvent(AddVehicleEvent event, SimulatorAPI sim) {
        sim.register(new Truck(event.getVehicleDTO()));
      }
    }
  }

  enum ParcelHandler implements TimedEventHandler<AddParcelEvent> {
    INSTANCE {
      @Override
      public void handleTimedEvent(AddParcelEvent event, SimulatorAPI sim) {
        // all parcels are accepted by default
        sim.register(new GFParcel(event.getParcelDTO()));
      }
    }
  }
}
