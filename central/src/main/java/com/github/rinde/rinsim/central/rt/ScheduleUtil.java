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
    checkArgument(!state.getVehicles().get(0).getRoute().isPresent());

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

        // final ParcelState parcelState = pdpModel.getParcelState(p);
        //
        // if (parcelState == ParcelState.PICKING_UP) {
        // // PICKING_UP -> needs to be in correct vehicle
        // if (!(vehicleState == VehicleState.PICKING_UP
        // && pdpModel.getVehicleActionInfo(vehicle).getParcel()
        // .equals(p))) {
        // // it is not being picked up by the current vehicle, remove from
        // // route
        // newSchedule.get(i).removeAll(Collections.singleton(p));
        // }
        // } else if (parcelState == ParcelState.IN_CARGO) {
        // // IN_CARGO -> may appear max once in schedule
        // if (vehicleContents.contains(p)) {
        // if (routeSet.count(p) == 2) {
        // // remove first occurrence of parcel, as it can only be visited
        // // once (for delivery)
        // newSchedule.get(i).remove(p);
        // }
        // } else {
        // // parcel is owned by someone else
        // newSchedule.get(i).removeAll(Collections.singleton(p));
        // }
        // } else if (parcelState == ParcelState.DELIVERING) {
        // // DELIVERING -> may appear max once in schedule
        // if (vehicleContents.contains(p)) {
        // if (routeSet.count(p) == 2) {
        // // removes the last occurrence
        // newSchedule.get(i).remove(newSchedule.get(i).lastIndexOf(p));
        // }
        // } else {
        // newSchedule.get(i).removeAll(Collections.singleton(p));
        // }
        // } else if (parcelState == ParcelState.DELIVERED) {
        // // DELIVERED -> may not appear in schedule
        // newSchedule.get(i).removeAll(Collections.singleton(p));
        // }
        // else: good: ANNOUNCED, AVAILABLE
      }

      // for all parcels in the vehicle we check if they exist in the
      // schedule, if not we add them to the end of the route
      // for (final Parcel p : vehicleContents) {
      // // check if the schedule agrees about parcel ownership
      // if (parcelOwner.get(p).intValue() != i) {
      // // if not, we move the parcel to the owner
      // newSchedule.get(i).add(p);
      // // TODO remove old
      // }
      // }

      // if current vehicle is picking up a parcel, make sure it appears in
      // the route once (if it doesn't we add it in the end).
      // if (vehicleState == VehicleState.PICKING_UP && parcelOwner
      // .get(pdpModel.getVehicleActionInfo(vehicle).getParcel())
      // .intValue() != i) {
      // newSchedule.get(i)
      // .add(pdpModel.getVehicleActionInfo(vehicle).getParcel());
      // }
    }
    return newSchedule;
  }

}
