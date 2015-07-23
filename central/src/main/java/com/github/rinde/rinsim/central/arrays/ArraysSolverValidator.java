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
package com.github.rinde.rinsim.central.arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

/**
 * Provides methods for validating input to and output from
 * {@link SingleVehicleArraysSolver}s and {@link MultiVehicleArraysSolver}. Also
 * provides <code>wrap(..)</code> methods which decorates any solver such that
 * both inputs and outputs are validated every time <code>solve(..)</code> is
 * called.
 * @author Rinde van Lon
 */
public final class ArraysSolverValidator {

  private ArraysSolverValidator() {}

  /**
   * Decorates the original {@link SingleVehicleArraysSolver} such that both the
   * inputs to the solver and the outputs from the solver are validated. When an
   * invalid input or output is detected a {@link IllegalArgumentException is
   * thrown}.
   * @param delegate The {@link SingleVehicleArraysSolver} that will be used for
   *          the actual solving.
   * @return The wrapped solver.
   */
  public static SingleVehicleArraysSolver wrap(
      SingleVehicleArraysSolver delegate) {
    return new SingleValidator(delegate);
  }

  /**
   * Decorates the original {@link MultiVehicleArraysSolver} such that both the
   * inputs to the solver and the outputs from the solver are validated. When an
   * invalid input or output is detected an {@link IllegalArgumentException} is
   * thrown.
   * @param delegate The {@link MultiVehicleArraysSolver} that will be used for
   *          the actual solving.
   * @return The wrapped solver.
   */
  public static MultiVehicleArraysSolver wrap(
      MultiVehicleArraysSolver delegate) {
    return new MultiValidator(delegate);
  }

  /**
   * Validates the inputs for the {@link SingleVehicleArraysSolver}. This method
   * checks all properties as defined in
   * {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
   * . If the inputs are not correct an {@link IllegalArgumentException} is
   * thrown.
   * @param travelTime Parameter as specified by
   *          {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
   *          .
   * @param releaseDates Parameter as specified by
   *          {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
   *          .
   * @param dueDates Parameter as specified by
   *          {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
   *          .
   * @param servicePairs Parameter as specified by
   *          {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
   *          .
   * @param serviceTimes Parameter as specified by
   *          {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
   *          .
   */
  public static void validateInputs(int[][] travelTime, int[] releaseDates,
      int[] dueDates, int[][] servicePairs, int[] serviceTimes) {

    final int n = travelTime.length;
    checkArgument(n > 0, "Travel time matrix cannot be empty");
    // check that matrix is n x n
    for (int i = 0; i < n; i++) {
      checkArgument(travelTime[i].length == n, "row %s has invalid length %s",
          i, travelTime[i].length);
    }
    checkArgument(releaseDates.length == n,
        "ReleaseDates array has incorrect length (%s) should be %s",
        releaseDates.length, n);
    checkArgument(dueDates.length == n,
        "dueDates array has incorrect length (%s) should be %s",
        dueDates.length, n);
    checkArgument(serviceTimes.length == n,
        "serviceTimes has incorrect length (%s) should be %s",
        serviceTimes.length, n);
    for (int i = 0; i < n; i++) {
      if (i == 0 || i == n - 1) {
        checkArgument(serviceTimes[i] == 0,
            "first and last index in serviceTimes should be 0");
      } else {
        checkArgument(serviceTimes[i] >= 0, "serviceTimes should be >= 0");
      }
    }

    // check time windows validity
    for (int i = 0; i < n; i++) {
      checkArgument(
          releaseDates[i] <= dueDates[i],
          "Index %s, release date (%s) should always be before the due date "
              + "(%s)",
          i, releaseDates[i], dueDates[i]);
    }

    checkArgument(releaseDates[0] == 0 && dueDates[0] == 0,
        "Start location should have release date and due date 0");
        // checkArgument(releaseDates[n - 1] == 0,
        // "Depot should have release date 0");

    // check that every pair consists of valid ids and that a location is in
    // only one pair
    final Set<Integer> set = newHashSet();
    for (int i = 0; i < servicePairs.length; i++) {
      checkArgument(servicePairs[i].length == 2,
          "Each pair entry should consist of exactly two locations.");
      for (int j = 0; j < 2; j++) {
        checkArgument(
            servicePairs[i][j] > 0 && servicePairs[i][j] < n - 1,
            "Pair consists of an invalid location (start location and depot are"
                + " not allowed), index is %s, location is %s",
            i, servicePairs[i][j]);
        checkArgument(
            !set.contains(servicePairs[i][j]),
            "Location can be part of only one pair, duplicate location: %s "
                + "(index %s,%s)",
            servicePairs[i][j], i, j);
        set.add(servicePairs[i][j]);
      }
    }
  }

  /**
   * Validates the inputs for the {@link MultiVehicleArraysSolver}. This method
   * checks all properties as defined in
   * {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   * . If the inputs are not correct an {@link IllegalArgumentException} is
   * thrown.
   * @param travelTime Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @param releaseDates Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @param dueDates Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @param servicePairs Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @param serviceTimes Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @param vehicleTravelTimes Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @param inventories Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @param remainingServiceTimes Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @param currentDestinations Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @param currentSolutions Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   */
  public static void validateInputs(int[][] travelTime, int[] releaseDates,
      int[] dueDates, int[][] servicePairs, int[] serviceTimes,
      int[][] vehicleTravelTimes, int[][] inventories,
      int[] remainingServiceTimes, int[] currentDestinations,
      @Nullable SolutionObject[] currentSolutions) {

    validateInputs(travelTime, releaseDates, dueDates, servicePairs,
        serviceTimes);

    // number of vehicles v
    final int v = vehicleTravelTimes.length;
    final int n = travelTime.length;
    checkArgument(v > 0, "At least one vehicle is required.");

    checkArgument(
        v == remainingServiceTimes.length,
        "Expected a remainingServiceTimes array of size %s, but found one with "
            + "size %s.",
        v, remainingServiceTimes.length);
    checkArgument(currentDestinations.length == v,
        "The currentDestinations array should be of length v=%s, it is %s.", v,
        currentDestinations.length);

    validateVehicleTravelTimes(v, n, vehicleTravelTimes, currentDestinations);

    final ImmutableSet.Builder<Integer> b = ImmutableSet.builder();
    for (int i = 0; i < servicePairs.length; i++) {
      b.add(servicePairs[i][0]);
      b.add(servicePairs[i][1]);
    }
    final Set<Integer> availLocs = b.build();

    final int m = n - 2 - servicePairs.length * 2;
    checkArgument(
        inventories.length == m,
        "Invalid number of inventory entries, must be equal to number of "
            + "delivery locations: %s, found: %s.",
        m, servicePairs.length);

    final Multimap<Integer, Integer> inventoriesMap = HashMultimap.create();
    final Set<Integer> parcelsInInventory = newHashSet();
    for (int i = 0; i < m; i++) {
      checkArgument(
          2 == inventories[i].length,
          "We expected inventories matrix of size m x 2, but we found m x %s "
              + "at index %s.",
          inventories[i].length, i);
      checkArgument(
          inventories[i][0] >= 0 && inventories[i][0] < v,
          "Found a reference to a non-existing vehicle (%s) in inventories at "
              + "row %s.",
          inventories[i][0], i);
      checkArgument(
          inventories[i][1] >= 1 && inventories[i][1] < n - 1,
          "Found a reference to a non-existing location (%s) in inventories at"
              + " row %s.",
          inventories[i][1], i);
      checkArgument(
          !availLocs.contains(inventories[i][1]),
          "Found a reference to a location (%s) in inventories at row %s which "
              + "is available, as such, it can not be in the inventory.",
          inventories[i][1], i);
      checkArgument(!parcelsInInventory.contains(inventories[i][1]),
          "Found a duplicate inventory entry, first duplicate at row %s.", i);
      parcelsInInventory.add(inventories[i][1]);
      inventoriesMap.put(inventories[i][0], inventories[i][1]);
    }

    for (int i = 0; i < v; i++) {
      checkArgument(remainingServiceTimes[i] >= 0,
          "Remaining service time must be >= 0, found %s.",
          remainingServiceTimes[i]);
    }

    final ImmutableBiMap.Builder<Integer, Integer> servicePairsBuilder =
        ImmutableBiMap
            .builder();
    for (int i = 0; i < servicePairs.length; i++) {
      servicePairsBuilder.put(servicePairs[i][0], servicePairs[i][1]);
    }
    final ImmutableBiMap<Integer, Integer> servicePairsMap = servicePairsBuilder
        .build();

    for (int i = 0; i < v; i++) {
      if (remainingServiceTimes[i] != 0) {
        checkArgument(currentDestinations[i] != 0);
      }

      if (currentDestinations[i] != 0) {
        final int dest = currentDestinations[i];
        checkArgument(
            dest >= 1 && dest < n - 1,
            "The destination must be a valid location, it can not be the "
                + "depot. It is %s.",
            dest);

        final boolean isAvailablePickupLoc = servicePairsMap.keySet().contains(
            dest);
        final boolean isInInventory = inventoriesMap.containsValue(dest);
        checkArgument(
            isAvailablePickupLoc != isInInventory,
            "The destination location %s must be an available pickup location "
                + "OR a delivery location which is in the inventory, available "
                + "pickup loc: %s, in inventory: %s.",
            dest, isAvailablePickupLoc, isInInventory);

        if (parcelsInInventory.contains(dest)) {
          checkArgument(
              inventoriesMap.get(i).contains(dest),
              "When a vehicle is moving towards a destination which is a "
                  + "delivery location, it must contain this parcel in its "
                  + "cargo. Vehicle %s, destination %s.",
              i, dest);
        }
      }
    }

    if (currentSolutions != null) {
      validateCurrentSolutions(v, n, currentSolutions, currentDestinations,
          inventoriesMap, servicePairsMap);
    }
  }

  private static void validateCurrentSolutions(int v, int n,
      SolutionObject[] currentSolutions,
      int[] currentDestinations,
      Multimap<Integer, Integer> inventoriesMap,
      ImmutableBiMap<Integer, Integer> servicePairsMap) {
    checkArgument(
        currentSolutions.length == v,
        "The number of currentSolutions (%s) should equal the number of "
            + "vehicles (%s).",
        currentSolutions.length, v);

    for (int i = 0; i < currentSolutions.length; i++) {
      final List<Integer> route = ImmutableList.copyOf(Ints
          .asList(currentSolutions[i].route));

      checkArgument(route.get(0) == 0,
          "First item in route should always be 0, it is %s.", route.get(0));
      checkArgument(route.get(route.size() - 1) == n - 1,
          "Last item in route should always be depot (%s), it is %s.", n - 1,
          route.get(route.size() - 1));

      if (currentDestinations[i] > 0) {
        // there is a current destination
        checkArgument(
            currentDestinations[i] == route.get(1),
            "The vehicle has a current destination (%s) but it is not the "
                + "first item in its route: %s.",
            currentDestinations[i], route);
      }
      final Collection<Integer> inventory = inventoriesMap.get(i);
      checkArgument(
          ImmutableSet.copyOf(route).containsAll(inventory),
          "The route should contain all locations in its inventory. Vehicle "
              + "%s, route: %s, inventory: %s.",
          i, route, inventory);

      for (int j = 1; j < route.size() - 1; j++) {
        final Integer item = route.get(j);
        final int freq = Collections.frequency(route, item);
        checkArgument(
            freq == 1,
            "Vehicle %s: each location should occur only once, found %s "
                + "instances of location %s. Route: %s.",
            i, freq, item, route);
        if (!inventoriesMap.containsEntry(i, item)) {
          // not in cargo, so the pair should appear in the route
          if (servicePairsMap.containsKey(item)) {
            checkArgument(route.contains(servicePairsMap.get(item)),
                "Couldn't find %s in regular mapping.", item);
          } else {
            checkArgument(
                route.contains(servicePairsMap.inverse().get(item)),
                "Couldn't find %s in inverse mapping.", item);
          }
        }
      }
    }
  }

  private static void validateVehicleTravelTimes(int v, int n,
      int[][] vehicleTravelTimes, int[] currentDestinations) {
    for (int i = 0; i < v; i++) {
      checkArgument(
          n == vehicleTravelTimes[i].length,
          "We expected vehicleTravelTimes matrix of size v x %s, but we found "
              + "v x %s at index %s.",
          n, vehicleTravelTimes[i].length, i);

      for (int j = 0; j < n; j++) {
        checkArgument(
            vehicleTravelTimes[i][j] >= 0,
            "Found an invalid vehicle travel time (%s) at position %s,%s. All "
                + "times must be >= 0.",
            vehicleTravelTimes[i][j], i, j);

        if (j == 0) {
          checkArgument(0 == vehicleTravelTimes[i][j],
              "vehicleTravelTimes[%s][%s] == %s, but must be 0.", i, j,
              vehicleTravelTimes[i][j]);
        } else if (currentDestinations[i] != 0 && j != currentDestinations[i]) {
          checkArgument(
              vehicleTravelTimes[i][j] == Integer.MAX_VALUE,
              "vehicleTravelTimes[%s][%s] == %s, but must be "
                  + "Integer.MAX_VALUE.",
              i, j, vehicleTravelTimes[i][j]);
        } else {
          checkArgument(
              vehicleTravelTimes[i][j] != Integer.MAX_VALUE,
              "vehicleTravelTimes[%s][%s] == Integer.MAX_VALUE, but must be "
                  + "normal value.",
              i, j);
        }
      }
    }
  }

  /**
   * Validates the {@link SolutionObject} that is produced by a
   * {@link SingleVehicleArraysSolver} . If the {@link SolutionObject} is
   * infeasible, an {@link IllegalArgumentException} is thrown.
   * @param sol The {@link SolutionObject} that is validated.
   * @param travelTime Parameter as specified by
   *          {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
   *          .
   * @param releaseDates Parameter as specified by
   *          {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
   *          .
   * @param dueDates Parameter as specified by
   *          {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
   *          .
   * @param servicePairs Parameter as specified by
   *          {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
   *          .
   * @param serviceTimes Parameter as specified by
   *          {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
   *          .
   * @param currentSolution Parameter as specified by
   *          {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
   *          .
   * @return The solution as is supplied, used for method chaining.
   */
  public static SolutionObject validateOutputs(SolutionObject sol,
      int[][] travelTime, int[] releaseDates, int[] dueDates,
      int[][] servicePairs, int[] serviceTimes,
      @Nullable SolutionObject currentSolution) {
    // convert single vehicle version to multi vehicle version for checking
    // of inputs
    final int n = travelTime.length;
    final int[][] vehicleTravelTimes = new int[1][n];
    // copy first row
    for (int i = 0; i < n; i++) {
      vehicleTravelTimes[0][i] = travelTime[0][i];
    }
    final Set<Integer> locationSet = newHashSet(ContiguousSet.create(
        Range.closedOpen(1, n - 1), DiscreteDomain.integers()));
    for (int i = 0; i < servicePairs.length; i++) {
      locationSet.remove(servicePairs[i][0]);
      locationSet.remove(servicePairs[i][1]);
    }

    final int[][] inventories = new int[locationSet.size()][2];
    final Iterator<Integer> locationSetIterator = locationSet.iterator();
    for (int i = 0; i < locationSet.size(); i++) {
      inventories[i][0] = 0;
      inventories[i][1] = locationSetIterator.next();
    }

    final int[] remainingServiceTimes = new int[] {0};
    final int[] currentDestinations = new int[] {0};

    @Nullable
    final SolutionObject[] currentSolutions = currentSolution == null ? null
        : new SolutionObject[] {currentSolution};

    // check inputs again since we just modified them
    validateInputs(travelTime, releaseDates, dueDates, servicePairs,
        serviceTimes, vehicleTravelTimes, inventories, remainingServiceTimes,
        currentDestinations, currentSolutions);

    final SolutionObject[] sols = new SolutionObject[] {sol};
    validateOutputs(sols, travelTime, releaseDates, dueDates, servicePairs,
        serviceTimes, vehicleTravelTimes, inventories, remainingServiceTimes,
        currentDestinations);
    return sol;
  }

  /**
   * Validates the {@link SolutionObject}s that are produced by a
   * {@link MultiVehicleArraysSolver}. If any of the {@link SolutionObject}s is
   * infeasible, an {@link IllegalArgumentException} is thrown.
   * @param sols The {@link SolutionObject}s that are validated.
   * @param travelTime Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @param releaseDates Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @param dueDates Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @param servicePairs Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @param serviceTimes Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @param vehicleTravelTimes Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @param inventories Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @param remainingServiceTimes Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @param currentDestinations Parameter as specified by
   *          {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   *          .
   * @return The solution as is supplied, used for method chaining.
   */
  public static SolutionObject[] validateOutputs(SolutionObject[] sols,
      int[][] travelTime, int[] releaseDates, int[] dueDates,
      int[][] servicePairs, int[] serviceTimes, int[][] vehicleTravelTimes,
      int[][] inventories, int[] remainingServiceTimes,
      int[] currentDestinations) {

    final int n = travelTime.length;

    final ImmutableSet.Builder<Integer> routeSetBuilder = ImmutableSet
        .builder();
    int visitedLocations = 0;
    for (final SolutionObject sol : sols) {
      routeSetBuilder.addAll(toSet(sol.route));
      visitedLocations += sol.route.length - 2;
    }
    final Set<Integer> routeSet = routeSetBuilder.build();
    final Set<Integer> locationSet = ContiguousSet.create(
        Range.closedOpen(0, travelTime.length), DiscreteDomain.integers());

    checkArgument(
        visitedLocations == n - 2,
        "The number of visits in routes should equal the number of locations, "
            + "expected: %s, observed: %s.",
        n - 2, visitedLocations);

    // checks duplicates and missing locations
    checkArgument(
        routeSet.size() == n,
        "Every location should appear exactly once in one route. Missing "
            + "location: %s.",
        Sets.difference(locationSet, routeSet));
    // checks for completeness of tour
    checkArgument(
        routeSet.equals(locationSet),
        "Not all locations are serviced, there is probably a non-existing "
            + "location in the route. Set difference: %s.",
        Sets.difference(routeSet, locationSet));

    final ImmutableMultimap.Builder<Integer, Integer> inventoryBuilder =
        ImmutableMultimap
            .builder();
    for (int i = 0; i < inventories.length; i++) {
      inventoryBuilder.put(inventories[i][0], inventories[i][1]);
    }
    final Multimap<Integer, Integer> inventoryMap = inventoryBuilder.build();

    for (int v = 0; v < sols.length; v++) {
      final SolutionObject sol = sols[v];

      /*
       * CHECK SERVICE SEQUENCE
       */
      checkArgument(
          sol.route[0] == 0,
          "The route should always start with the vehicle start location: 0, "
              + "actual:%s.",
          sol.route[0]);
      checkArgument(sol.route[sol.route.length - 1] == n - 1,
          "The route should always finish with the depot.");

      if (currentDestinations[v] != 0) {
        checkArgument(
            sol.route[1] == currentDestinations[v],
            "Vehicle %s has a current destination %s, as such this must be the "
                + "first point to visit (at index 1). The route: %s.",
            v, currentDestinations[v], Arrays.toString(sol.route));
      }

      final Set<Integer> locs = ImmutableSet.copyOf(Ints.asList(sol.route));
      final Collection<Integer> inventory = inventoryMap.get(v);
      for (final Integer i : inventory) {
        checkArgument(
            locs.contains(i),
            "Every location in the inventory of a vehicle should occur in its "
                + "route, route for vehicle %s does not contain location %s.",
            v, i);
      }

      // check service pairs ordering, pickups should be visited before
      // their corresponding delivery location
      final Map<Integer, Integer> pairs = newHashMap();
      for (int i = 0; i < servicePairs.length; i++) {
        pairs.put(servicePairs[i][0], servicePairs[i][1]);
      }
      final Set<Integer> seen = newHashSet();
      final Set<Integer> set = newHashSet(Ints.asList(sol.route));
      for (int i = 1; i < sol.route.length - 1; i++) {
        if (pairs.containsKey(sol.route[i])) {
          checkArgument(
              !seen.contains(pairs.get(sol.route[i])),
              "Pickups should be visited before their corresponding deliveries."
                  + " Location %s should be visited after location %s.",
              pairs.get(sol.route[i]), sol.route[i]);

          checkArgument(
              set.contains(pairs.get(sol.route[i])),
              "Vehicle %s: this route should contain both the pickup and "
                  + "delivery location, found %s, didn't find %s.",
              v, sol.route[i], pairs.get(sol.route[i]));
        }
        seen.add(sol.route[i]);
      }

      /*
       * CHECK ARRIVAL TIMES
       */
      checkArgument(sol.arrivalTimes.length == sol.route.length,
          "Number of arrival times should equal number of locations.");
      checkArgument(sol.arrivalTimes[0] == 0,
          "The first arrival time should be 0, was %s.", sol.arrivalTimes[0]);

      // check feasibility
      final int[] minArrivalTimes = ArraysSolvers.computeArrivalTimes(
          sol.route, travelTime, remainingServiceTimes[v],
          vehicleTravelTimes[v], serviceTimes, releaseDates);
      for (int i = 1; i < sol.route.length; i++) {
        checkArgument(
            sol.arrivalTimes[i] >= minArrivalTimes[i],
            "Vehicle %s, route index %s, arrivalTime (%s) needs to be greater "
                + "or equal to minArrivalTime (%s).",
            v, i, sol.arrivalTimes[i], minArrivalTimes[i]);
      }

      /*
       * CHECK OBJECTIVE VALUE
       */

      // sum travel time
      final int totalTravelTime = ArraysSolvers.computeTotalTravelTime(
          sol.route, travelTime, vehicleTravelTimes[v]);

      // sum tardiness
      final int tardiness = ArraysSolvers.computeRouteTardiness(sol.route,
          sol.arrivalTimes, serviceTimes, dueDates, remainingServiceTimes[v]);

      checkArgument(
          sol.objectiveValue == totalTravelTime + tardiness,
          "Vehicle %s: incorrect objective value (%s), it should be travel "
              + "time + tardiness = %s + %s = %s.",
          v, sol.objectiveValue, totalTravelTime, tardiness, totalTravelTime
              + tardiness);

    }

    return sols;
  }

  static Set<Integer> toSet(int[] arr) {
    final Set<Integer> set = newHashSet();
    for (int i = 0; i < arr.length; i++) {
      set.add(arr[i]);
    }
    return set;
  }

  private static class SingleValidator implements SingleVehicleArraysSolver {
    private final SingleVehicleArraysSolver delegateSolver;

    SingleValidator(SingleVehicleArraysSolver delegate) {
      delegateSolver = delegate;
    }

    @Override
    public SolutionObject solve(int[][] travelTime, int[] releaseDates,
        int[] dueDates, int[][] servicePairs, int[] serviceTimes,
        @Nullable SolutionObject currentSolution) {
      // first check inputs
      validateInputs(travelTime, releaseDates, dueDates, servicePairs,
          serviceTimes);
      // execute solver
      final SolutionObject output = delegateSolver.solve(travelTime,
          releaseDates, dueDates, servicePairs, serviceTimes, currentSolution);
      // check outputs
      return validateOutputs(output, travelTime, releaseDates, dueDates,
          servicePairs, serviceTimes, currentSolution);
    }
  }

  private static class MultiValidator implements MultiVehicleArraysSolver {
    private final MultiVehicleArraysSolver delegateSolver;

    MultiValidator(MultiVehicleArraysSolver delegate) {
      delegateSolver = delegate;
    }

    @Override
    public SolutionObject[] solve(int[][] travelTime, int[] releaseDates,
        int[] dueDates, int[][] servicePairs, int[] serviceTimes,
        int[][] vehicleTravelTimes, int[][] inventories,
        int[] remainingServiceTimes, int[] currentDestinations,
        @Nullable SolutionObject[] currentSolutions) {
      validateInputs(travelTime, releaseDates, dueDates, servicePairs,
          serviceTimes, vehicleTravelTimes, inventories, remainingServiceTimes,
          currentDestinations, currentSolutions);
      final SolutionObject[] output = delegateSolver.solve(travelTime,
          releaseDates, dueDates, servicePairs, serviceTimes,
          vehicleTravelTimes, inventories, remainingServiceTimes,
          currentDestinations, currentSolutions);
      return validateOutputs(output, travelTime, releaseDates, dueDates,
          servicePairs, serviceTimes, vehicleTravelTimes, inventories,
          remainingServiceTimes, currentDestinations);
    }
  }
}
