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
package com.github.rinde.rinsim.central;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.github.rinde.rinsim.central.GlobalStateObject.VehicleStateObject;
import com.github.rinde.rinsim.core.pdptw.ParcelDTO;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Provides methods for validating input to and output from {@link Solver}s.
 * Also provides {@link #wrap(Solver)} method which decorates any solver such
 * that both inputs and outputs are validated every time it is is called.
 * @author Rinde van Lon 
 */
public final class SolverValidator {

  private SolverValidator() {}

  /**
   * Decorates the original {@link Solver} such that both the inputs to the
   * solver and the outputs from the solver are validated. When an invalid input
   * or output is detected a {@link IllegalArgumentException is thrown}.
   * @param delegate The {@link Solver} that will be used for the actual
   *          solving.
   * @return The wrapped solver.
   */
  public static Solver wrap(Solver delegate) {
    return new SolverValidator.ValidatedSolver(delegate);
  }

  /**
   * Decorates the original {@link StochasticSupplier} such that all generated
   * {@link Solver} instances are wrapped using {@link #wrap(Solver)}.
   * @param sup The supplier to wrap.
   * @return The wrapper supplier.
   */
  public static StochasticSupplier<Solver> wrap(
      StochasticSupplier<? extends Solver> sup) {
    return new ValidatedSupplier(sup);
  }

  /**
   * Validates the inputs for {@link Solver#solve(GlobalStateObject)} method. If
   * the input is not correct an {@link IllegalArgumentException} is thrown.
   * @param state The state to validate.
   * @return The state.
   */
  public static GlobalStateObject validateInputs(GlobalStateObject state) {
    checkArgument(state.time >= 0, "Time must be >= 0, is %s.", state.time);
    final Set<ParcelDTO> inventoryParcels = newHashSet();

    final boolean routeIsPresent = state.vehicles.get(0).route.isPresent();
    final Set<ParcelDTO> allParcels = newHashSet();
    for (final VehicleStateObject vs : state.vehicles) {
      checkArgument(
          vs.route.isPresent() == routeIsPresent,
          "Either a route should be present for all vehicles, or no route should be present for all vehicles.");
      if (vs.route.isPresent()) {
        for (final ParcelDTO p : vs.route.get()) {
          checkArgument(
              !allParcels.contains(p),
              "Found parcel which is already present in the route of another vehicle. Parcel %s.",
              p);
        }
        allParcels.addAll(vs.route.get());
      }
    }

    for (int i = 0; i < state.vehicles.size(); i++) {
      final VehicleStateObject vs = state.vehicles.get(i);

      checkArgument(vs.remainingServiceTime >= 0,
          "Remaining service time must be >= 0, is %s.",
          vs.remainingServiceTime);
      checkArgument(vs.getDto().speed > 0, "Speed must be positive, is %s.",
          vs.getDto().speed);
      final Set<ParcelDTO> intersection = Sets.intersection(
          state.availableParcels, vs.contents);
      checkArgument(
          intersection.isEmpty(),
          "Parcels can not be available AND in the inventory of a vehicle, found: %s.",
          intersection);
      final Set<ParcelDTO> inventoryIntersection = Sets.intersection(
          inventoryParcels, vs.contents);
      checkArgument(
          inventoryIntersection.isEmpty(),
          "Parcels can not be in the inventory of two vehicles at the same time, found: %s.",
          inventoryIntersection);
      inventoryParcels.addAll(vs.contents);

      // if the destination parcel is not available, it must be in the
      // cargo of the vehicle
      if (vs.destination != null) {
        final boolean isAvailable = state.availableParcels
            .contains(vs.destination);
        final boolean isInCargo = vs.contents.contains(vs.destination);
        checkArgument(
            isAvailable != isInCargo,
            "Destination must be either available (%s) or in the current vehicle's cargo (%s), but not both (i.e. XOR). Destination: %s, vehicle: %s (out of %s), remaining service time: %s.",
            isAvailable, isInCargo, vs.destination, i, state.vehicles.size(),
            vs.remainingServiceTime);
      }

      if (vs.route.isPresent()) {
        checkRoute(vs, i);
      }
    }
    return state;
  }

  /**
   * Validate the route of a vehicle.
   * @param vs The vehicle to check.
   * @param i The index of the vehicle, only used to generate nice error
   *          messages.
   * @throws IllegalArgumentException if the route is not correct.
   */
  public static void checkRoute(VehicleStateObject vs, int i) {
    checkArgument(vs.route.isPresent());
    checkArgument(
        vs.route.get().containsAll(vs.contents),
        "Vehicle %s's route doesn't contain all locations it has in cargo. Route: %s, cargo: %s.",
        i, vs.route.get(), vs.contents);
    if (vs.destination != null) {
      checkArgument(!vs.route.get().isEmpty()
          && vs.route.get().get(0) == vs.destination,
          "First location in route must equal destination (%s), route is: %s.",
          vs.destination, vs.route.get());
    }

    for (final ParcelDTO dp : vs.route.get()) {
      final int freq = Collections.frequency(vs.route.get(), dp);
      if (vs.contents.contains(dp)) {
        checkArgument(
            freq == 1,
            "A parcel already in cargo should occur once in the route, found %s instance(s). Parcel: %s, route: %s.",
            freq, dp, vs.route.get());
      } else {
        checkArgument(
            freq == 2,
            "A parcel that is still available should occur twice in the route, found %s instance(s). Parcel: %s, route: %s.",
            freq, dp, vs.route.get(), vs.remainingServiceTime);
      }
    }
  }

  /**
   * Validates the routes that are produced by a {@link Solver}. If one of the
   * routes is infeasible an {@link IllegalArgumentException} is thrown.
   * @param routes The routes that are validated.
   * @param state Parameter as specified by
   *          {@link Solver#solve(GlobalStateObject)}.
   * @return The routes.
   */
  public static ImmutableList<ImmutableList<ParcelDTO>> validateOutputs(
      ImmutableList<ImmutableList<ParcelDTO>> routes, GlobalStateObject state) {

    checkArgument(
        routes.size() == state.vehicles.size(),
        "There must be exactly one route for every vehicle, found %s routes with %s vehicles.",
        routes.size(), state.vehicles.size());

    final Set<ParcelDTO> inputParcels = newHashSet(state.availableParcels);
    final Set<ParcelDTO> outputParcels = newHashSet();
    for (int i = 0; i < routes.size(); i++) {
      final List<ParcelDTO> route = routes.get(i);
      final Set<ParcelDTO> routeSet = ImmutableSet.copyOf(route);
      checkArgument(routeSet.containsAll(state.vehicles.get(i).contents),
          "The route of vehicle %s doesn't visit all parcels in its cargo.", i);
      inputParcels.addAll(state.vehicles.get(i).contents);

      if (state.vehicles.get(i).destination != null) {
        checkArgument(
            route.get(0) == state.vehicles.get(i).destination,
            "The route of vehicle %s should start with its current destination: %s.",
            i, state.vehicles.get(i).destination);
      }

      for (final ParcelDTO p : route) {
        checkArgument(!outputParcels.contains(p),
            "Found a parcel which is already in another route: %s.", p);
        final int frequency = Collections.frequency(route, p);
        if (state.availableParcels.contains(p)) {
          // if the parcel is available, it needs to occur twice in
          // the route (once for pickup, once for delivery).
          checkArgument(
              frequency == 2,
              "Route %s: a parcel that is picked up needs to be delivered as well, so it should occur twice in the route, found %s occurence(s) of parcel %s.",
              i, frequency, p);
        } else {
          checkArgument(
              state.vehicles.get(i).contents.contains(p),
              "The parcel in this route is not available, which means it should be in the contents of this vehicle. Parcel: %s.",
              p);
          checkArgument(
              frequency == 1,
              "A parcel that is already in cargo should occur once in the route, found %s occurences of parcel %s.",
              frequency, p);
        }
      }
      outputParcels.addAll(route);
    }
    checkArgument(
        inputParcels.equals(outputParcels),
        "The number of distinct parcels in the routes should equal the number of parcels in the input, parcels that should be added in routes: %s, parcels that should be removed from routes: %s.",
        Sets.difference(inputParcels, outputParcels),
        Sets.difference(outputParcels, inputParcels));
    return routes;
  }

  private static class ValidatedSolver implements Solver {
    private final Solver delegateSolver;

    ValidatedSolver(Solver delegate) {
      delegateSolver = delegate;
    }

    @Override
    public ImmutableList<ImmutableList<ParcelDTO>> solve(GlobalStateObject state) {
      return validateOutputs(delegateSolver.solve(validateInputs(state)), state);
    }
  }

  private static final class ValidatedSupplier extends
      StochasticSuppliers.AbstractStochasticSupplier<Solver> {
    private static final long serialVersionUID = -2408333654270668182L;
    private final StochasticSupplier<? extends Solver> supplier;

    ValidatedSupplier(StochasticSupplier<? extends Solver> sup) {
      supplier = sup;
    }

    @Override
    public Solver get(long seed) {
      return SolverValidator.wrap(supplier.get(seed));
    }
  }
}
