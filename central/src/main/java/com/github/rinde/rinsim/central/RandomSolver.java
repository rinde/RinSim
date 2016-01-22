/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.central.GlobalStateObject.VehicleStateObject;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;

/**
 * A {@link Solver} that constructs random solutions. For each order it first
 * randomly selects a vehicle. When all orders are assigned to a vehicle the
 * ordering of the pickups and deliveries is shuffled randomly.
 *
 * @author Rinde van Lon
 */
public final class RandomSolver implements Solver {
  private final RandomGenerator randomGenerator;

  RandomSolver(RandomGenerator rng) {
    randomGenerator = rng;
  }

  @Override
  public ImmutableList<ImmutableList<Parcel>> solve(GlobalStateObject state) {
    checkArgument(!state.getVehicles().isEmpty(), "Need at least one vehicle.");
    final LinkedListMultimap<VehicleStateObject, Parcel> map =
      LinkedListMultimap.create();

    final Set<Parcel> available = newLinkedHashSet(state.getAvailableParcels());
    final Set<Parcel> destinations = newLinkedHashSet();
    for (final VehicleStateObject vso : state.getVehicles()) {
      destinations.addAll(vso.getDestination().asSet());
    }
    available.removeAll(destinations);

    // do random assignment of available parcels
    for (final Parcel p : available) {
      final int index = randomGenerator.nextInt(state.getVehicles().size());
      map.put(state.getVehicles().get(index), p);
      map.put(state.getVehicles().get(index), p);
    }

    final ImmutableList.Builder<ImmutableList<Parcel>> builder = ImmutableList
        .builder();
    // insert contents, shuffle ordering, insert destination if applicable
    for (final VehicleStateObject vso : state.getVehicles()) {
      final List<Parcel> assigned = newArrayList(map.get(vso));
      final List<Parcel> conts = newArrayList(vso.getContents());
      conts.removeAll(vso.getDestination().asSet());
      assigned.addAll(conts);
      if (vso.getDestination().isPresent()
          && state.getAvailableParcels().contains(vso.getDestination().get())) {
        assigned.add(vso.getDestination().get());
      }
      Collections.shuffle(assigned, new RandomAdaptor(randomGenerator));
      if (vso.getDestination().isPresent()) {
        assigned.add(0, vso.getDestination().get());
      }
      builder.add(ImmutableList.copyOf(assigned));
    }
    return builder.build();
  }

  /**
   * Creates a new {@link RandomSolver} with the specified random seed.
   * @param seed The random seed.
   * @return A new random solver instance.
   */
  public static Solver create(long seed) {
    return new RandomSolver(new MersenneTwister(seed));
  }

  /**
   * @return A {@link StochasticSupplier} for {@link RandomSolver} instances.
   */
  public static StochasticSupplier<Solver> supplier() {
    return RandomSolverSupplier.INSTANCE;
  }

  enum RandomSolverSupplier implements StochasticSupplier<Solver> {
    INSTANCE {
      @Override
      public Solver get(long seed) {
        return create(seed);
      }

      @Override
      public String toString() {
        return RandomSolver.class.getSimpleName() + ".supplier()";
      }
    }
  }
}
