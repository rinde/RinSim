/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.examples.comm;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

class RandomBroadcastAgent implements MovingRoadUser, CommUser, TickListener {
  static final double MIN_RANGE = .2;
  static final double MAX_RANGE = 1.5;
  static final long LONELINESS_THRESHOLD = 10 * 1000;
  static final double VEHICLE_SPEED_KMH = 50;

  Optional<RoadModel> roadModel;
  Optional<CommDevice> device;
  Optional<Point> destination;
  long lastReceiveTime;
  private final double range;
  private final double reliability;
  private final RandomGenerator rng;

  RandomBroadcastAgent(RandomGenerator r) {
    rng = r;
    device = Optional.absent();
    roadModel = Optional.absent();
    destination = Optional.absent();

    range = MIN_RANGE + rng.nextDouble() * (MAX_RANGE - MIN_RANGE);
    reliability = rng.nextDouble();
  }

  @Override
  public Optional<Point> getPosition() {
    if (roadModel.get().containsObject(this)) {
      return Optional.of(roadModel.get().getPosition(this));
    }
    return Optional.absent();
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
    } else if (timeLapse.getStartTime()
      - lastReceiveTime > LONELINESS_THRESHOLD) {
      device.get().broadcast(Messages.WHERE_IS_EVERYBODY);
    }
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}

  @Override
  public double getSpeed() {
    return VEHICLE_SPEED_KMH;
  }

  enum Messages implements MessageContents {
    HELLO_WORLD, NICE_TO_MEET_YOU, WHERE_IS_EVERYBODY;
  }
}
