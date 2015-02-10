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
package com.github.rinde.rinsim.ui.renderers;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.road.GridRoadModel;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;

public class GridRoadModelRendererTest {
  @Test
  public void test() {
    final Simulator sim = new Simulator(new MersenneTwister(123L),
        Measure.valueOf(1000L, SI.MILLI(SI.SECOND)));

    sim.register(GridRoadModel.builder()
        .setNumXCells(5)
        .build());
    sim.configure();

    sim.register(new Agent());

    // View.create(sim)
    // .with(new GridRoadModelRenderer())
    // .show();
  }

  static class Agent implements MovingRoadUser, TickListener {

    @Override
    public void tick(TimeLapse timeLapse) {
      // TODO Auto-generated method stub

    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
      // TODO Auto-generated method stub

    }

    @Override
    public void initRoadUser(RoadModel model) {
      model.addObjectAt(this, new Point(0, 0));
    }

    @Override
    public double getSpeed() {
      return 1;
    }

  }
}
