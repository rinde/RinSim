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
package com.github.rinde.rinsim.central.rt;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.rinde.rinsim.central.GlobalStateObject;
import com.github.rinde.rinsim.central.GlobalStateObject.VehicleStateObject;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

/**
 *
 * @author Rinde van Lon
 */
public final class ScheduleUtil {

  private ScheduleUtil() {}

  // fixes routes.
  static List<List<Parcel>> fixSchedule(
      ImmutableList<ImmutableList<Parcel>> schedule,
      GlobalStateObject state) {

    checkArgument(schedule.size() == state.getVehicles().size(),
      "The number of routes (%s) and the number of vehicles (%s) must "
          + "be equal.",
      schedule.size(), state.getVehicles().size());
    checkArgument(!state.getVehicles().get(0).getRoute().isPresent(),
      "A state object without routes is expected.");

    // only parcels in this set may occur in the schedule
    final Set<Parcel> undeliveredParcels = new HashSet<>();
    undeliveredParcels.addAll(state.getAvailableParcels());

    // for each vehicle, we create a multiset that is a representation of the
    // number of times the occurrence of a parcel is REQUIRED to be in the
    // route of the vehicle
    final List<Multiset<Parcel>> expectedRoutes = new ArrayList<>();
    for (int i = 0; i < state.getVehicles().size(); i++) {
      expectedRoutes.add(HashMultiset.<Parcel>create());
      final VehicleStateObject vehicle = state.getVehicles().get(i);
      expectedRoutes.get(i).addAll(vehicle.getContents());
      if (vehicle.getDestination().isPresent() && !vehicle.getContents()
          .contains(vehicle.getDestination().get())) {
        expectedRoutes.get(i).add(vehicle.getDestination().get(), 2);
      }
      undeliveredParcels.addAll(vehicle.getContents());
    }

    // create map of parcel -> vehicle index
    final Multimap<Parcel, Integer> parcelOwner = LinkedHashMultimap.create();
    for (int i = 0; i < schedule.size(); i++) {
      final List<Parcel> route = schedule.get(i);
      final Set<Parcel> routeSet = ImmutableSet.copyOf(route);
      for (final Parcel p : routeSet) {
        parcelOwner.put(p, i);
      }
    }

    // copy schedule into a modifiable structure
    final List<List<Parcel>> newSchedule = new ArrayList<>();
    for (final ImmutableList<Parcel> route : schedule) {
      newSchedule.add(new ArrayList<>(route));
    }
    // compare with current vehicle cargo
    for (int i = 0; i < state.getVehicles().size(); i++) {
      final VehicleStateObject vehicle = state.getVehicles().get(i);
      final Multiset<Parcel> routeSet =
        ImmutableMultiset.copyOf(schedule.get(i));

      final Set<Parcel> test =
        Sets.union(routeSet.elementSet(), expectedRoutes.get(i).elementSet());

      for (final Parcel p : test) {
        final int actualOccurences = routeSet.count(p);
        checkState(actualOccurences <= 2);
        final int expectedOccurrences = expectedRoutes.get(i).count(p);

        if (!undeliveredParcels.contains(p)) {
          // it is already delivered, remove all occurrences
          newSchedule.get(i).removeAll(Collections.singleton(p));
        } else if (actualOccurences != expectedOccurrences
            && expectedOccurrences > 0) {
          if (expectedOccurrences == 1 && actualOccurences == 2) {
            newSchedule.get(i).remove(p);
          } else {
            // expected occurr = 1 or 2
            final boolean destinationIsCurrent =
              vehicle.getDestination().asSet().contains(p);

            int toAdd = expectedOccurrences - actualOccurences;

            // add it once at the front of the route
            if (destinationIsCurrent) {
              newSchedule.get(i).add(0, p);
              toAdd--;
            }

            // add it once to the end of the route
            if (toAdd > 0) {
              newSchedule.get(i).add(p);
            }
          }
        }

        // if the parcel is expected in the current vehicle, but it also appears
        // in (an) other vehicle(s), we have to remove it there
        if (expectedOccurrences > 0 && parcelOwner.containsKey(p)) {
          for (final Integer v : parcelOwner.get(p)) {
            if (!v.equals(i)) {
              newSchedule.get(v).removeAll(Collections.singleton(p));
            }
          }
        }
      }

      if (vehicle.getDestination().isPresent()
          && !newSchedule.get(i).get(0)
              .equals(vehicle.getDestination().get())) {
        newSchedule.get(i).remove(vehicle.getDestination().get());
        newSchedule.get(i).add(0, vehicle.getDestination().get());
      }
    }
    return newSchedule;
  }

}
