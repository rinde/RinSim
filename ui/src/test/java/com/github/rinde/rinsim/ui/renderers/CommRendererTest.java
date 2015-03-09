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

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View;
import com.google.common.base.Optional;

/**
 * @author Rinde van Lon
 *
 */
public class CommRendererTest {
  @Test
  public void test() {
    final RandomGenerator rng = new MersenneTwister(123L);
    final Simulator sim = Simulator.builder()
        .setRandomGenerator(rng)
        .addModel(CommModel.builder()
            .setRandomGenerator(rng)
            .build())
        .addModel(PlaneRoadModel.builder().build())
        .build();

    for (int i = 0; i < 20; i++) {
      sim.register(new CommAgent(rng, (i + 1) / 10d, 1 / (i + 1)));
    }

    View.create(sim)
        .with(CommRenderer.builder())
        .with(PlaneRoadModelRenderer.create())
        .enableAutoPlay()
        .enableAutoClose()
        .setSpeedUp(10)
        .stopSimulatorAtTime(1000 * 60 * 5)
        .show();
  }

  static class CommAgent implements MovingRoadUser, CommUser, TickListener {
    Optional<RoadModel> roadModel;
    Optional<CommDevice> device;
    Optional<Point> destination;
    private final double range;
    private final double reliability;
    private final RandomGenerator rng;

    CommAgent(RandomGenerator r, double ran, double rel) {
      System.out.println(ran);
      rng = r;
      range = ran;
      reliability = rel;
      device = Optional.absent();
      roadModel = Optional.absent();
      destination = Optional.absent();
    }

    @Override
    public Point getPosition() {
      return roadModel.get().getPosition(this);
    }

    @Override
    public void setCommDevice(CommDeviceBuilder builder) {
      device = Optional.of(builder
          .setMaxRange(range)
          .setReliability(reliability)
          .build());
    }

    @Override
    public void initRoadUser(RoadModel model) {
      roadModel = Optional.of(model);
      roadModel.get().addObjectAt(this, roadModel.get().getRandomPosition(rng));
    }

    @Override
    public void tick(TimeLapse timeLapse) {
      if (!destination.isPresent()) {
        destination = Optional.of(roadModel.get().getRandomPosition(rng));
      }
      roadModel.get().moveTo(this, destination.get(), timeLapse);

      if (roadModel.get().getPosition(this).equals(destination.get())) {
        destination = Optional.absent();
      }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

    @Override
    public double getSpeed() {
      return 50;
    }
  }
}
