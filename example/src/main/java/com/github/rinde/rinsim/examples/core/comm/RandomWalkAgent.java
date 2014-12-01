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
package com.github.rinde.rinsim.examples.core.comm;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.SimulatorUser;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.communication.CommunicationAPI;
import com.github.rinde.rinsim.core.model.communication.CommunicationUser;
import com.github.rinde.rinsim.core.model.communication.Mailbox;
import com.github.rinde.rinsim.core.model.communication.Message;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

/**
 * Example of the simple random agent with the use of simulation facilities.
 * @author Bartosz Michalik 
 */
class RandomWalkAgent implements TickListener, MovingRoadUser, SimulatorUser,
    CommunicationUser {
  private static final int MAX_MSG = 2000;
  // 10s
  private static final int COMMUNICATION_PERIOD = 10000;
  protected RoadModel roadModel;
  protected Optional<RoadUser> currentPackage;
  protected Queue<Point> path;
  protected RandomGenerator rnd;
  private SimulatorAPI simulator;
  private final double speed;
  private CommunicationAPI cm;
  private final int radius;

  private final Map<RandomWalkAgent, Long> lastCommunicationTime;

  private Set<RandomWalkAgent> communicatedWith;
  private final Mailbox mailbox;

  private final ReentrantLock lock;

  private int communications;

  private long lastCommunication;
  private final double reliability;

  /**
   * Create simple agent.
   * @param sp default speed of object in graph units per millisecond
   * @param rad in which it can communicate
   * @param rel of communication
   */
  @SuppressWarnings("null")
  public RandomWalkAgent(double sp, int rad, double rel) {
    speed = sp;
    radius = rad;
    reliability = rel;
    communicatedWith = newLinkedHashSet();
    lastCommunicationTime = newLinkedHashMap();
    mailbox = new Mailbox();
    lock = new ReentrantLock();
    communications = 0;
    path = newLinkedList();

    currentPackage = Optional.absent();
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    checkMsgs(timeLapse.getTime());
    refreshList(timeLapse.getTime());

    if (path.isEmpty()) {
      if (currentPackage.isPresent()
          && roadModel.containsObject(currentPackage.get())) {
        simulator.unregister(currentPackage.get());
      }
      if (communications > MAX_MSG) {
        simulator.unregister(this);
        return;
      }
      final Point destination = roadModel.getRandomPosition(rnd);
      currentPackage = Optional.<RoadUser> of(new ExamplePackage(
          "dummy package", destination));
      simulator.register(currentPackage);
      path = new LinkedList<Point>(roadModel.getShortestPathTo(this,
          destination));
    } else {
      roadModel.followPath(this, path, timeLapse);
    }

    sendMsgs(timeLapse.getStartTime());
  }

  private void refreshList(long currentTime) {
    if (lastCommunication + COMMUNICATION_PERIOD < currentTime) {
      lock.lock();
      communicatedWith = new HashSet<RandomWalkAgent>();
      for (final Entry<RandomWalkAgent, Long> e : lastCommunicationTime
          .entrySet()) {
        if (e.getValue() + COMMUNICATION_PERIOD * 100 >= currentTime) {
          communicatedWith.add(e.getKey());
        }
      }
      lock.unlock();
    }
  }

  private void sendMsgs(long currentTime) {
    if (lastCommunication + COMMUNICATION_PERIOD < currentTime) {
      lastCommunication = currentTime;
      cm.broadcast(new Message(this) {});
    }
  }

  private void checkMsgs(long currentTime) {
    final Queue<Message> messages = mailbox.getMessages();

    for (final Message m : messages) {
      lastCommunicationTime.put((RandomWalkAgent) m.getSender(), currentTime);
      communications++;
    }
  }

  public Set<RandomWalkAgent> getCommunicatedWith() {
    lock.lock();
    final Set<RandomWalkAgent> result = new HashSet<RandomWalkAgent>(
        communicatedWith);
    lock.unlock();
    return result;
  }

  @Override
  public double getSpeed() {
    return speed;
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}

  @Nullable
  @Override
  public Point getPosition() {
    return roadModel.containsObject(this) ? roadModel.getPosition(this) : null;
  }

  @Override
  public double getRadius() {
    return radius;
  }

  @Override
  public double getReliability() {
    return reliability;
  }

  @Override
  public void receive(Message message) {
    mailbox.receive(message);
  }

  public int getNoReceived() {
    return communications;
  }

  @Override
  public void setCommunicationAPI(CommunicationAPI commAPI) {
    cm = commAPI;
  }

  @Override
  public void initRoadUser(RoadModel model) {
    roadModel = model;
    final Point pos = roadModel.getRandomPosition(rnd);
    roadModel.addObjectAt(this, pos);
  }

  @Override
  public void setSimulator(SimulatorAPI api) {
    simulator = api;
    rnd = api.getRandomGenerator();
  }

}
