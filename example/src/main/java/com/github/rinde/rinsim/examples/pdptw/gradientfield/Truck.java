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
package com.github.rinde.rinsim.examples.pdptw.gradientfield;

import java.util.Map;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.VehicleState;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.pdptw.DefaultParcel;
import com.github.rinde.rinsim.core.pdptw.DefaultVehicle;
import com.github.rinde.rinsim.core.pdptw.VehicleDTO;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Predicate;

class Truck extends DefaultVehicle implements FieldEmitter {
  private GradientModel gradientModel;

  public Truck(VehicleDTO pDto) {
    super(pDto);
  }

  @Override
  protected void tickImpl(TimeLapse time) {
    // Check if we can deliver nearby
    final Parcel delivery = getDelivery(time, 5);

    final RoadModel rm = roadModel.get();
    final PDPModel pm = pdpModel.get();

    if (delivery != null) {
      if (delivery.getDestination().equals(getPosition())
          && pm.getVehicleState(this) == VehicleState.IDLE) {
        pm.deliver(this, delivery, time);
      } else {
        rm.moveTo(this, delivery.getDestination(), time);
      }
      return;
    }

    // Otherwise, Check if we can pickup nearby
    final DefaultParcel closest = (DefaultParcel) RoadModels.findClosestObject(
        rm.getPosition(this), rm, new Predicate<RoadUser>() {
          @Override
          public boolean apply(RoadUser input) {
            return input instanceof DefaultParcel
                && pm.getParcelState(((DefaultParcel) input)) == ParcelState.AVAILABLE;
          }
        });

    if (closest != null
        && Point.distance(rm.getPosition(closest), getPosition()) < 10) {
      if (rm.equalPosition(closest, this)
          && pm.getTimeWindowPolicy().canPickup(closest.getPickupTimeWindow(),
              time.getTime(), closest.getPickupDuration())) {
        final double newSize = getPDPModel().getContentsSize(this)
            + closest.getMagnitude();

        if (newSize <= getCapacity()) {
          pm.pickup(this, closest, time);
        }
      } else {
        rm.moveTo(this, rm.getPosition(closest), time);
      }
      return;
    }

    if (rm.getObjectsOfType(Parcel.class).isEmpty()) {
      rm.moveTo(this, dto.startPosition, time);
      return;
    }

    // If none of the above, let the gradient field guide us!
    @Nullable
    final Point p = gradientModel.getTargetFor(this);
    if (p != null) {
      rm.moveTo(this, p, time);
    }
  }

  @Nullable
  public Parcel getDelivery(TimeLapse time, int distance) {
    Parcel target = null;
    double closest = distance;
    final PDPModel pm = pdpModel.get();
    for (final Parcel p : pm.getContents(this)) {

      final double dist = Point.distance(roadModel.get().getPosition(this),
          p.getDestination());
      if (dist < closest
          && pm.getTimeWindowPolicy().canDeliver(p.getDeliveryTimeWindow(),
              time.getTime(), p.getPickupDuration())) {
        closest = dist;
        target = p;
      }
    }

    return target;
  }

  @Override
  public void setModel(GradientModel model) {
    gradientModel = model;
  }

  @Override
  public Point getPosition() {
    return roadModel.get().getPosition(this);
  }

  @Override
  public float getStrength() {
    return -1;
  }

  public Map<Point, Float> getFields() {
    return gradientModel.getFields(this);
  }
}
