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
package com.github.rinde.rinsim.ui.renderers;

import org.eclipse.swt.graphics.RGB;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.pdptw.DefaultDepot;
import com.github.rinde.rinsim.core.pdptw.DefaultVehicle;
import com.github.rinde.rinsim.core.pdptw.VehicleDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.testutil.GuiTests;
import com.github.rinde.rinsim.ui.View;

/**
 * @author Rinde van Lon
 *
 */
@Category(GuiTests.class)
public class PDPModelRendererTest {

  /**
   * Test for {@link PDPModelRenderer}.
   */
  @Test
  public void test() {
    final Simulator sim = Simulator.builder()
      .addModel(RoadModelBuilders.plane())
      .addModel(DefaultPDPModel.builder())
      .addModel(View.builder()
        .with(PlaneRoadModelRenderer.builder())
        .with(RoadUserRenderer.builder()
          .withColorAssociation(DefaultDepot.class, new RGB(255, 200, 0))
          .withCircleAroundObjects()
        )
        .with(PDPModelRenderer.builder()
          .withDestinationLines()
        )
        .withAutoPlay()
        .withAutoClose()
        .withSimulatorEndTime(10000)
      )
      .build();

    for (int i = 0; i < 10; i++) {
      if (i != 5) {
        sim.register(Parcel.builder(new Point(i, i + 1), new Point(5, 5))
          .build());
        sim.register(new Vehicle(new Point(i, 10 - i), new Point(i, i + 1)));
      }
    }
    sim.register(new DefaultDepot(new Point(5, 5)));

    sim.start();
  }

  static class Vehicle extends DefaultVehicle {

    final Point destination;

    public Vehicle(Point p, Point dest) {
      super(VehicleDTO.builder().startPosition(p).build());
      destination = dest;
    }

    @Override
    protected void tickImpl(TimeLapse time) {
      roadModel.get().moveTo(this, destination, time);
    }
  }
}
