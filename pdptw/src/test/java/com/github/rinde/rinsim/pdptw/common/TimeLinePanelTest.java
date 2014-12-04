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
package com.github.rinde.rinsim.pdptw.common;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests the {@link TimeLinePanel}.
 * @author Rinde van Lon 
 */
public class TimeLinePanelTest {

  /**
   * Test the gui.
   */
  @Test
  @Ignore
  public void guiTest() {
    // TODO enable this test again as soon as cyclic dependencies problem is
    // solved
    // Experiment
    // .build(TestObjectiveFunction.INSTANCE)
    // .addConfiguration(TestMASConfiguration.create("config"))
    // .addScenario(ScenarioTestUtil.create(1730))
    // .showGui(new UICreator() {
    //
    // @Override
    // public void createUI(Simulator sim) {
    // final UiSchema schema = new UiSchema(false);
    // schema.add(Vehicle.class, SWT.COLOR_RED);
    // schema.add(Depot.class, SWT.COLOR_CYAN);
    // schema.add(Parcel.class, SWT.COLOR_BLUE);
    // View.create(sim)
    // .with(new PlaneRoadModelRenderer())
    // .with(new RoadUserRenderer(schema, false))
    // .with(new PDPModelRenderer())
    // .with(new TimeLinePanel())
    // .setSpeedUp(200)
    // .enableAutoClose()
    // .enableAutoPlay()
    // .show();
    // }
    // })
    // .perform();
  }
}
