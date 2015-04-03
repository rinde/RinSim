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
import org.eclipse.swt.graphics.RGB;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.testutil.GuiTests;
import com.github.rinde.rinsim.ui.View;
import com.google.common.base.Optional;

/**
 * @author Rinde van Lon
 *
 */
@Category(GuiTests.class)
public class CommRendererTest {
  /**
   * Test for {@link CommRenderer}.
   */
  @Test
  public void testRenderer() {
    final RandomGenerator rng = new MersenneTwister(123L);
    final Simulator sim = Simulator.builder()
      .setRandomGenerator(rng)
      .addModel(CommModel.builder())
      .addModel(PlaneRoadModel.builder())
      .build();

    for (int i = 0; i < 20; i++) {
      sim.register(new CommAgent(rng, (i + 1) / 10d, i * (1d / 20d)));
    }

    sim.register(new CommAgent(rng, -1d, 1d));

    View.create(sim)
      .with(CommRenderer.builder()
        .showReliabilityColors(new RGB(0, 0, 255), new RGB(255, 255, 0))
        .showReliabilityPercentage()
        .showMessageCount()
      )
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
    long lastReceiveTime = 0;

    CommAgent(RandomGenerator r, double ran, double rel) {
      rng = r;
      range = ran;
      reliability = rel;
      device = Optional.absent();
      roadModel = Optional.absent();
      destination = Optional.absent();
    }

    @Override
    public Optional<Point> getPosition() {
      return Optional.of(roadModel.get().getPosition(this));
    }

    @Override
    public void setCommDevice(CommDeviceBuilder builder) {
      if (range >= 0) {
        builder.setMaxRange(range);
      }
      device = Optional.of(builder
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

      if (device.get().getUnreadCount() > 0) {
        lastReceiveTime = timeLapse.getStartTime();
        device.get().getUnreadMessages();
        device.get().broadcast(Messages.NICE_TO_MEET_YOU);
      } else if (device.get().getReceivedCount() == 0) {
        device.get().broadcast(Messages.HELLO_WORLD);
      } else if (lastReceiveTime > 10 * 1000) {
        device.get().broadcast(Messages.WHERE_IS_EVERYBODY);
      }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

    @Override
    public double getSpeed() {
      return 50;
    }

    enum Messages implements MessageContents {
      HELLO_WORLD, NICE_TO_MEET_YOU, WHERE_IS_EVERYBODY;
    }
  }
}
