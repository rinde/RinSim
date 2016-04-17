/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.testutil.GuiTests;
import com.github.rinde.rinsim.ui.View;

/**
 * Test of {@link RoadUserRenderer}.
 * @author Rinde van Lon
 */
@Category(GuiTests.class)
public class RoadUserRendererTest {

  /**
   * Test of {@link RoadUserRenderer}.
   */
  @Test
  public void test() {
    final Simulator sim = Simulator.builder()
      .addModel(RoadModelBuilders.plane())
      .addModel(View.builder()
        .with(PlaneRoadModelRenderer.builder())
        .with(RoadUserRenderer.builder()
          .withColorAssociation(Agent1.class, new RGB(255, 0, 255))
          .withColorAssociation(Agent2.class, new RGB(0, 0, 255))
          .withColorAssociation(Agent3.class, new RGB(255, 200, 0))
          .withCircleAroundObjects())
        .withAutoPlay()
        .withAutoClose()
        .withSimulatorEndTime(10000))
      .build();

    for (int i = 0; i < 11; i++) {
      if (i != 5) {
        sim.register(new Agent1(new Point(i, i)));
        sim.register(new Agent3(new Point(i, 10 - i)));
      }
    }
    sim.register(new Agent2(new Point(5, 5)));

    sim.start();

  }

  static class Agent1 extends Agent {
    Agent1(Point p) {
      super(p);
    }
  }

  static class Agent2 extends Agent {
    Agent2(Point p) {
      super(p);
    }
  }

  static class Agent3 extends Agent {
    Agent3(Point p) {
      super(p);
    }
  }

  static class Agent implements RoadUser {
    final Point pos;

    Agent(Point p) {
      pos = p;
    }

    @Override
    public void initRoadUser(RoadModel model) {
      model.addObjectAt(this, pos);
    }

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }
}
