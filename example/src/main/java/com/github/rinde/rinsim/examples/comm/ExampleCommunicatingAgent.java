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
package com.github.rinde.rinsim.examples.comm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

class ExampleCommunicatingAgent
    implements MovingRoadUser, CommUser, TickListener {
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
  private final String name;
  private final Set<CommUser> knownAgents;

  ExampleCommunicatingAgent(RandomGenerator r, String nm) {
    rng = r;
    device = Optional.absent();
    roadModel = Optional.absent();
    destination = Optional.absent();
    name = nm;
    knownAgents = new HashSet<>();
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

    // check if we have messages
    if (device.get().getUnreadCount() > 0) {
      lastReceiveTime = timeLapse.getStartTime();
      final List<Message> messages = device.get().getUnreadMessages();

      // reading all unread messages
      for (final Message message : messages) {

        if (message.getContents() == Messages.WHO_ARE_YOU) {
          // if someone asks us who we are, we give an polite answer
          device.get().send(new MyNameIs(name), message.getSender());
        } else if (message.getContents() instanceof MyNameIs) {
          // if someone tells us their name, we remember who they are.
          // cast the message to read the name
          final String nameOfOther =
            ((MyNameIs) message.getContents()).getName();
          System.out.println("I, " + name + ", just met " + nameOfOther);
          knownAgents.add(message.getSender());
        }

        if (!knownAgents.contains(message.getSender())) {
          // if we don't know the sender, we ask who they are
          device.get().send(Messages.WHO_ARE_YOU, message.getSender());
        }
      }
      // when we have non-zero unread messages, we always broadcast "nice to
      // meet you" to everyone within range.
      device.get().broadcast(Messages.NICE_TO_MEET_YOU);
    } else if (device.get().getReceivedCount() == 0) {
      // our first message
      device.get().broadcast(Messages.HELLO_WORLD);
    } else if (timeLapse.getStartTime()
      - lastReceiveTime > LONELINESS_THRESHOLD) {
      // when we haven't received anything for a while, we become anxious :(
      device.get().broadcast(Messages.WHERE_IS_EVERYBODY);
    }
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}

  @Override
  public double getSpeed() {
    return VEHICLE_SPEED_KMH;
  }

  @Override
  public String toString() {
    return name;
  }

  enum Messages implements MessageContents {
    HELLO_WORLD, NICE_TO_MEET_YOU, WHERE_IS_EVERYBODY, WHO_ARE_YOU;
  }

  static class MyNameIs implements MessageContents {
    private final String name;

    MyNameIs(String nm) {
      name = nm;
    }

    String getName() {
      return name;
    }
  }
}
