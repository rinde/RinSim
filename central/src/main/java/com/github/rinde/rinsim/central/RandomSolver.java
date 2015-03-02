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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.central.GlobalStateObject.VehicleStateObject;
import com.github.rinde.rinsim.core.pdptw.ParcelDTO;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;

/**
 * A {@link Solver} that constructs random solutions. For each order it first
 * randomly selects a vehicle. When all orders are assigned to a vehicle the
 * ordering of the pickups and deliveries is shuffled randomly.
 * 
 * @author Rinde van Lon 
 */
public class RandomSolver implements Solver {

  private final RandomGenerator randomGenerator;

  /**
   * Creates a new instance using the specified random generator.
   * @param rng The random generator to use for creating random solutions.
   */
  public RandomSolver(RandomGenerator rng) {
    randomGenerator = rng;
  }

  @Override
  public ImmutableList<ImmutableList<ParcelDTO>> solve(GlobalStateObject state) {
    final LinkedListMultimap<VehicleStateObject, ParcelDTO> map = LinkedListMultimap
        .create();

    final Set<ParcelDTO> available = newLinkedHashSet(state.availableParcels);
    final Set<ParcelDTO> destinations = newLinkedHashSet();
    for (final VehicleStateObject vso : state.vehicles) {
      if (vso.destination != null) {
        destinations.add(vso.destination);
      }
    }
    available.removeAll(destinations);

    // do random assignment of available parcels
    for (final ParcelDTO p : available) {
      final int index = randomGenerator.nextInt(state.vehicles.size());
      map.put(state.vehicles.get(index), p);
      map.put(state.vehicles.get(index), p);
    }

    final ImmutableList.Builder<ImmutableList<ParcelDTO>> builder = ImmutableList
        .builder();
    // insert contents, shuffle ordering, insert destination if applicable
    for (final VehicleStateObject vso : state.vehicles) {
      final List<ParcelDTO> assigned = newArrayList(map.get(vso));
      final List<ParcelDTO> conts = newArrayList(vso.contents);
      conts.remove(vso.destination);
      assigned.addAll(conts);
      if (vso.destination != null
          && state.availableParcels.contains(vso.destination)) {
        assigned.add(vso.destination);
      }
      Collections.shuffle(assigned, new RandomAdaptor(randomGenerator));
      if (vso.destination != null) {
        assigned.add(0, vso.destination);
      }
      builder.add(ImmutableList.copyOf(assigned));
    }
    return builder.build();
  }

  /**
   * @return A {@link StochasticSupplier} for {@link RandomSolver} instances.
   */
  public static StochasticSupplier<Solver> supplier() {
    return new StochasticSuppliers.AbstractStochasticSupplier<Solver>() {
      private static final long serialVersionUID = 992219257352250656L;

      @Override
      public Solver get(long seed) {
        return new RandomSolver(new MersenneTwister(seed));
      }
    };
  }
}
