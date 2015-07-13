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

import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

class AGV extends Vehicle {
  private Optional<Box> destination;
  private Optional<AgvModel> agvModel;

  AGV(Point startPos) {
    super(VehicleDTO.builder()
        .capacity(1)
        .startPosition(startPos)
        .speed(FactoryExample.AGV_SPEED)
        .build());
    destination = Optional.absent();
    agvModel = Optional.absent();
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
      if (getRoadModel().equalPosition(this, destination.get())) {
        getPDPModel().pickup(this, destination.get(), time);
      } else if (getPDPModel().getContents(this).contains(destination.get())) {
        if (getRoadModel().getPosition(this)
            .equals(destination.get().getDeliveryLocation())) {
          getPDPModel().deliver(this, destination.get(), time);
          destination = Optional.absent();
        } else {
          getRoadModel()
              .moveTo(this, destination.get().getDeliveryLocation(), time);
        }
      } else {
        if (getRoadModel().containsObject(destination.get())) {
          getRoadModel().moveTo(this, destination.get(), time);
        } else {
          destination = Optional.absent();
        }
      }
    }
  }

  /**
   * Injection of AvgModel.
   * @param model Model to inject.
   */
  public void registerAgvModel(AgvModel model) {
    agvModel = Optional.of(model);
  }
}
