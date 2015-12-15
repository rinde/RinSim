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
package com.github.rinde.rinsim.central;

import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.Set;

import com.github.rinde.rinsim.central.GlobalStateObject.VehicleStateObject;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.google.common.collect.ImmutableSet;

/**
 *
 * @author Rinde van Lon
 */
public final class GlobalStateObjects {

  private GlobalStateObjects() {}

  public static ImmutableSet<Parcel> unassignedParcels(
      GlobalStateObject state) {
    final Set<Parcel> set = newLinkedHashSet(state.getAvailableParcels());
    set.removeAll(assignedParcels(state));
    return ImmutableSet.copyOf(set);
  }

  public static ImmutableSet<Parcel> allParcels(GlobalStateObject state) {
    return ImmutableSet.<Parcel>builder()
        .addAll(state.getAvailableParcels())
        .addAll(assignedParcels(state))
        .build();
  }

  static ImmutableSet<Parcel> assignedParcels(GlobalStateObject state) {
    final ImmutableSet.Builder<Parcel> set = ImmutableSet.builder();
    for (final VehicleStateObject vso : state.getVehicles()) {
      if (vso.getRoute().isPresent()) {
        set.addAll(vso.getRoute().get());
      }
    }
    return set.build();
  }
}
