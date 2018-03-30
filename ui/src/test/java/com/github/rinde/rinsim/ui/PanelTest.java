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
package com.github.rinde.rinsim.ui;

import org.eclipse.swt.SWT;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.testutil.GuiTests;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;

/**
 * @author Rinde van Lon
 *
 */
@Category(GuiTests.class)
public final class PanelTest {

  /**
   * Test of panel system.
   */
  @Test
  public void panelTest() {

    final Simulator sim = Simulator
      .builder()
      .addModel(RoadModelBuilders.plane()
        .withMinPoint(new Point(0, 0))
        .withMaxPoint(new Point(10, 10))
        .withMaxSpeed(10d))
      .addModel(
        View
          .builder()
          .with(PlaneRoadModelRenderer.builder())
          .with(RoadUserRenderer.builder())
          .with(
            TestPanelRenderer.builder("LEFT")
              .withPosition(SWT.LEFT)
              .withSize(200))
          .with(
            TestPanelRenderer.builder("RIHGT BOEEE YEAH")
              .withPosition(SWT.RIGHT)
              .withSize(300))
          .with(
            TestPanelRenderer.builder("RIHGT BOEEE YEAH")
              .withPosition(SWT.TOP)
              .withSize(100))
          .with(
            TestPanelRenderer.builder("TOP2")
              .withPosition(SWT.TOP)
              .withSize(100))
          .with(
            TestPanelRenderer.builder("LEFT2")
              .withPosition(SWT.LEFT)
              .withSize(100))
          .with(
            TestPanelRenderer.builder("LEFT3")
              .withPosition(SWT.LEFT)
              .withSize(150))
          .withAutoPlay()
          .withAutoClose()
          .withSimulatorEndTime(10000))
      .build();

    sim.start();
  }
}
