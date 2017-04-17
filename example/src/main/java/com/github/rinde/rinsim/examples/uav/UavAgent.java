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
package com.github.rinde.rinsim.examples.uav;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.rand.RandomUser;
import com.github.rinde.rinsim.core.model.road.CollisionPlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.MoveProgress;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

/**
 * @author Hoang Tung Dinh
 * @author Rinde van Lon
 */
public final class UavAgent
    implements MovingRoadUser, TickListener, RandomUser {

  static final int MAX_STUCK_TICK_COUNT = 10;

  private Optional<Point> destination = Optional.absent();
  private Optional<RandomGenerator> rng;
  private Optional<CollisionPlaneRoadModel> roadModel;
  private final Point initialPosition;
  private final String name;
  private final double speed;
  private int stuckTickCount;

  UavAgent(Point initPos, String nm, double uavSpeed) {
    initialPosition = initPos;
    name = nm;
    speed = uavSpeed;
    roadModel = Optional.absent();
    destination = Optional.absent();
    rng = Optional.absent();
  }

  @Override
  public void initRoadUser(RoadModel model) {
    roadModel = Optional.of((CollisionPlaneRoadModel) model);
    roadModel.get().addObjectAt(this, initialPosition);
  }

  @Override
  public double getSpeed() {
    return speed;
  }

  Optional<Point> getDestination() {
    return destination;
  }

  String getName() {
    return name;
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    if (!destination.isPresent()) {
      nextDestination();
    }

    final MoveProgress mp =
      roadModel.get().moveTo(this, destination.get(), timeLapse);

    if (roadModel.get().getPosition(this).equals(destination.get())
      || stuckTickCount >= MAX_STUCK_TICK_COUNT) {
      nextDestination();
      stuckTickCount = 0;
    } else if (mp.distance().getValue().doubleValue() == 0d) {
      stuckTickCount++;
    } else {
      stuckTickCount = 0;
    }
  }

  private void nextDestination() {
    destination = Optional.of(roadModel.get().getRandomPosition(rng.get()));
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}

  @Override
  public String toString() {
    return String.format("[%s %s]", getClass().getSimpleName(), name);
  }

  @Override
  public void setRandomGenerator(RandomProvider provider) {
    rng = Optional.of(provider.newInstance());
  }
}
