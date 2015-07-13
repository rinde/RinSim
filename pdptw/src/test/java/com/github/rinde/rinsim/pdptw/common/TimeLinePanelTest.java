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
package com.github.rinde.rinsim.pdptw.common;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.RandomVehicle;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioController;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.testutil.GuiTests;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;

/**
 * Tests the {@link TimeLinePanel}.
 * @author Rinde van Lon
 */
@Category(GuiTests.class)
public class TimeLinePanelTest {

  /**
   * Test the gui.
   */
  @Test
  public void guiTest() {
    final Scenario testScenario = ScenarioTestUtil.createRandomScenario(1730);

    final Simulator sim = Simulator
        .builder()
        .addModel(
            ScenarioController
                .builder(testScenario)
                .withEventHandler(AddParcelEvent.class,
                    AddParcelEvent.defaultHandler())
                .withEventHandler(TimeOutEvent.class,
                    TimeOutEvent.ignoreHandler())
                .withEventHandler(AddVehicleEvent.class,
                    new TimedEventHandler<AddVehicleEvent>() {
                      @Override
                      public void handleTimedEvent(AddVehicleEvent event,
                          SimulatorAPI simulator) {
                        simulator
                            .register(new RandomVehicle(event.getVehicleDTO()));
                      }
                    }))
        .addModel(
            View.builder()
                .with(PlaneRoadModelRenderer.builder())
                .with(RoadUserRenderer.builder())
                .with(PDPModelRenderer.builder())
                .with(TimeLinePanel.builder())
                .withSpeedUp(200)
                .withAutoClose()
                .withSimulatorEndTime(60 * 60 * 1000)
                .withAutoPlay())
        .build();

    sim.start();
  }
}
