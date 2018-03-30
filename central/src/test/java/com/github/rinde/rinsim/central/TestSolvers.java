/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.github.rinde.rinsim.central.GlobalStateObject.VehicleStateObject;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.google.common.collect.ImmutableList;

public final class TestSolvers {

  private TestSolvers() {}

  public static Solver asSolver(
      Iterable<ImmutableList<ImmutableList<Parcel>>> schedules) {
    final Iterator<ImmutableList<ImmutableList<Parcel>>> it = schedules
      .iterator();
    return SolverValidator.wrap(
      new Solver() {
        @Override
        public ImmutableList<ImmutableList<Parcel>> solve(
            GlobalStateObject state) {
          return it.next();
        }
      });
  }

  public static Solver lazyInsertion() {
    return Implementation.LAZY_INSERT;
  }

  enum Implementation implements Solver {
    LAZY_INSERT {

      @Override
      public ImmutableList<ImmutableList<Parcel>> solve(GlobalStateObject state)
          throws InterruptedException {

        final List<List<Parcel>> mutSchedule =
          new ArrayList<>(state.getVehicles().size());
        for (final VehicleStateObject vso : state.getVehicles()) {
          mutSchedule.add(new ArrayList<>(vso.getRoute().get()));
        }

        final Set<Parcel> unscheduled =
          GlobalStateObjects.unassignedParcels(state);

        for (final Parcel p : unscheduled) {
          int index = 0;
          int minJobs = mutSchedule.get(0).size();

          for (int i = 1; i < mutSchedule.size(); i++) {
            final int jobs = mutSchedule.get(i).size();
            if (jobs < minJobs) {
              index = i;
              minJobs = jobs;
            }
          }
          mutSchedule.get(index).add(0, p);
          mutSchedule.get(index).add(0, p);
        }

        final ImmutableList.Builder<ImmutableList<Parcel>> schedule =
          ImmutableList.builder();
        for (final List<Parcel> row : mutSchedule) {
          schedule.add(ImmutableList.copyOf(row));
        }
        return schedule.build();
      }

    }
  }

}
