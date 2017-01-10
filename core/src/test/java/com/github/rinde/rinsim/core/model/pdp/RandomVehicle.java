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
package com.github.rinde.rinsim.core.model.pdp;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.rand.RandomUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

public class RandomVehicle extends Vehicle implements RandomUser {
  private Optional<RoadModel> rm;
  private Optional<PDPModel> pm;
  private Optional<RandomGenerator> rng;
  private Optional<Parcel> target;

  public RandomVehicle(VehicleDTO dto) {
    super(dto);
    rng = Optional.absent();
    rm = Optional.absent();
    pm = Optional.absent();
    target = Optional.absent();
  }

  @Override
  protected void tickImpl(TimeLapse time) {
    if (!time.hasTimeLeft()) {
      return;
    }
    if (!target.isPresent()) {
      target = findTarget();
    }

    if (target.isPresent()) {
      if (pm.get().containerContains(this, target.get())) {
        if (rm.get().getPosition(this)
          .equals(target.get().getDeliveryLocation())) {
          pm.get().deliver(this, target.get(), time);
        } else {
          rm.get().moveTo(this, target.get().getDeliveryLocation(), time);
        }
      } else {
        if (pm.get().getParcelState(target.get()) != ParcelState.AVAILABLE) {
          // somebody got there first
          target = Optional.absent();
        } else if (rm.get().equalPosition(this, target.get())) {
          pm.get().pickup(this, target.get(), time);
        } else {
          rm.get().moveTo(this, target.get(), time);
        }
      }
    } else {
      final Set<Depot> depots = rm.get().getObjectsOfType(Depot.class);
      if (!depots.isEmpty()) {
        rm.get().moveTo(this, depots.iterator().next(), time);
      }
    }
  }

  Optional<Parcel> findTarget() {
    final Collection<Parcel> available = pm.get().getParcels(
      ParcelState.AVAILABLE);
    final ImmutableSet<Parcel> contents = pm.get().getContents(this);
    if (available.isEmpty() && contents.isEmpty()) {
      return Optional.absent();
    }
    boolean pickup;
    if (!available.isEmpty() && !contents.isEmpty()) {
      pickup = rng.get().nextBoolean();
    } else {
      pickup = !available.isEmpty();
    }
    if (pickup) {
      return Optional.of(newArrayList(available).get(
        rng.get().nextInt(available.size())));
    }
    return Optional.of(contents.asList().get(
      rng.get().nextInt(contents.size())));
  }

  @Override
  public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
    rm = Optional.of(pRoadModel);
    pm = Optional.of(pPdpModel);
  }

  @Override
  public void setRandomGenerator(RandomProvider provider) {
    rng = Optional.of(provider.newInstance());
  }
}
