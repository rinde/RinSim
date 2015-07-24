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
package com.github.rinde.rinsim.examples.fabrirecht.simple;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.google.common.base.Predicate;

/**
 * This truck implementation only picks parcels up, it does not deliver them.
 *
 * @author Rinde van Lon
 */
class Truck extends Vehicle {

  Truck(VehicleDTO pDto) {
    super(pDto);
  }

  @Override
  protected void tickImpl(TimeLapse time) {
    final RoadModel rm = getRoadModel();
    final PDPModel pm = getPDPModel();
    // we always go to the closest available parcel
    final Parcel closest = (Parcel) RoadModels
        .findClosestObject(rm.getPosition(this), rm, new Predicate<RoadUser>() {
          @Override
          public boolean apply(@Nullable RoadUser input) {
            return input instanceof Parcel
                && pm.getParcelState((Parcel) input) == ParcelState.AVAILABLE;
          }
        });

    if (closest != null) {
      rm.moveTo(this, closest, time);
      if (rm.equalPosition(closest, this)
          && pm
              .getTimeWindowPolicy()
              .canPickup(closest.getPickupTimeWindow(), time.getTime(),
                  closest.getPickupDuration())) {
        pm.pickup(this, closest, time);
      }
    }
  }
}
