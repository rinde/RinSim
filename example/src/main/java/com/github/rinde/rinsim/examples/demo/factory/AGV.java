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
package com.github.rinde.rinsim.examples.demo.factory;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.google.common.base.Optional;

class AGV extends Vehicle {
  private Optional<RoadModel> roadModel;
  private Optional<PDPModel> pdpModel;
  private Optional<Box> destination;
  private final RandomGenerator randomGenerator;
  private Optional<AgvModel> agvModel;

  AGV(RandomGenerator rng) {
    roadModel = Optional.absent();
    destination = Optional.absent();
    agvModel = Optional.absent();
    randomGenerator = rng;
    setCapacity(1);
  }

  @Override
  public double getSpeed() {
    return FactoryExample.AGV_SPEED;
  }

  @Override
  protected void tickImpl(TimeLapse time) {
    if (!time.hasTimeLeft()) {
      return;
    }
    if (!destination.isPresent()) {
      final Box closest = agvModel.get().nextDestination();
      if (closest != null) {
        destination = Optional.of(closest);
      }
    }

    if (destination.isPresent()) {
      if (roadModel.get().equalPosition(this, destination.get())) {
        pdpModel.get().pickup(this, destination.get(), time);
      } else if (pdpModel.get().getContents(this).contains(destination.get())) {
        if (roadModel.get().getPosition(this)
            .equals(destination.get().getDestination())) {
          pdpModel.get().deliver(this, destination.get(), time);
          destination = Optional.absent();
        } else {
          roadModel.get()
              .moveTo(this, destination.get().getDestination(), time);
        }
      } else {
        if (roadModel.get().containsObject(destination.get())) {
          roadModel.get().moveTo(this, destination.get(), time);
        } else {
          destination = Optional.absent();
        }
      }
    }
  }

  @Override
  public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
    roadModel = Optional.of(pRoadModel);
    pdpModel = Optional.of(pPdpModel);
    pRoadModel.addObjectAt(this,
        roadModel.get().getRandomPosition(randomGenerator));
  }

  /**
   * Injection of AvgModel;
   * @param model Model to inject.
   */
  public void registerAgvModel(AgvModel model) {
    agvModel = Optional.of(model);
  }
}
