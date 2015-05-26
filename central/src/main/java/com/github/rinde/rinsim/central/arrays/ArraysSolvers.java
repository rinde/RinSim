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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newHashSet;

import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.converter.UnitConverter;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.central.GlobalStateObject;
import com.github.rinde.rinsim.central.GlobalStateObject.VehicleStateObject;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.math.DoubleMath;

/**
 * @author Rinde van Lon
 * 
 */
public final class ArraysSolvers {

  // TODO create a Converter class which uses the Units as constructor
  // parameters. All methods can then easily access them, making most of the
  // code much cleaner.

  private ArraysSolvers() {}

  /**
   * Converts the list of points on a plane into a travel time matrix. For
   * distance between two points the Euclidean distance is used, i.e. no
   * obstacles or graph structure are considered. See
   * {@link #toTravelTimeMatrix(List, Unit, Measure, Unit, RoundingMode)} for
   * more options.
   * @param points The set of points which will be converted to a travel time
   *          matrix.
   * @param speed the speed in m/s.
   * @param rm The rounding mode, see {@link RoundingMode}.
   * @return A <code>n x n</code> travel time matrix, where <code>n</code> is
   *         the size of the <code>points</code> list.
   */
  public static int[][] toTravelTimeMatrix(List<Point> points, double speed,
    RoundingMode rm) {
    return toTravelTimeMatrix(points, SI.METER,
      Measure.valueOf(speed, SI.METERS_PER_SECOND), SI.SECOND, rm);
  }

  /**
   * Converts the list of points on a plane into a travel time matrix. For
   * distance between two points the euclidean distance is used, i.e. no
   * obstacles or graph structure are considered.
   * @param points The set of points which will be converted to a travel time
   *          matrix.
   * @param distUnit The {@link Unit} that is used for distances (
   *          {@link Length}) between the specified points.
   * @param speed The travel speed specified as a {@link Measure} which includes
   *          its {@link Unit}.
   * @param outputTimeUnit The output time {@link Unit} to which all times are
   *          converted, e.g. if {@link SI#SECOND} is specified the travel times
   *          will be in seconds.
   * @param rm When computing the travel times they often need to be rounded.
   *          The rounding mode indicates how numbers are rounded, see
   *          {@link RoundingMode} for the available options.
   * @return A <code>n x n</code> travel time matrix, where <code>n</code> is
   *         the size of the <code>points</code> list.
   */
  public static int[][] toTravelTimeMatrix(List<Point> points,
    Unit<Length> distUnit, Measure<Double, Velocity> speed,
    Unit<Duration> outputTimeUnit, RoundingMode rm) {
    checkArgument(points.size() >= 2);
    final int[][] matrix = new int[points.size()][points.size()];
    for (int i = 0; i < points.size(); i++) {
      for (int j = 0; j < i; j++) {
        if (i != j) {
          // compute distance
          final Measure<Double, Length> dist = Measure.valueOf(
            Point.distance(points.get(i), points.get(j)), distUnit);
          // calculate duration in desired unit
          final double duration = RoadModels.computeTravelTime(speed, dist,
            outputTimeUnit);
          // round duration
          final int tt = DoubleMath.roundToInt(duration, rm);
          matrix[i][j] = tt;
          matrix[j][i] = tt;
        }
      }
    }
    return matrix;
  }

  /**
   * Converts the {@link GlobalStateObject} into an {@link ArraysObject} using
   * the specified output time unit.
   * @param state The state to convert.
   * @param outputTimeUnit The {@link Unit} to use as time in the resulting
   *          object.
   * @return An {@link ArraysObject} using the specified output time unit.
   */
  public static ArraysObject toSingleVehicleArrays(GlobalStateObject state,
    Unit<Duration> outputTimeUnit) {

    final UnitConverter timeConverter = state.timeUnit
      .getConverterTo(outputTimeUnit);

    final VehicleStateObject v = state.vehicles.iterator().next();

    // we check all vehicles in case this method is used in other contexts
    final ImmutableSet.Builder<Parcel> cargoBuilder = ImmutableSet.builder();
    for (final VehicleStateObject vs : state.vehicles) {
      cargoBuilder.addAll(vs.contents);
    }
    final Set<Parcel> inCargo = cargoBuilder.build();

    // there are always two locations: the current vehicle location and
    // the depot
    final int numLocations = 2 + state.availableParcels.size() * 2
      + inCargo.size();

    final int[] releaseDates = new int[numLocations];
    final int[] dueDates = new int[numLocations];
    final int[][] servicePairs = new int[state.availableParcels.size()][2];
    final int[] serviceTimes = new int[numLocations];

    // we need to create two mappings:
    // Parcel -> pickup index / deliver index
    // index -> Parcel
    final ImmutableMap.Builder<Parcel, ParcelIndexObj> parcel2indexBuilder = ImmutableMap
      .builder();
    final ImmutableMap.Builder<Integer, ParcelIndexObj> index2parcelBuilder = ImmutableMap
      .builder();

    // we wrap the points in PointWrapper to avoid problems with (possibly)
    // duplicates in the points
    final ImmutableList.Builder<Point> points = ImmutableList.builder();
    points.add(v.location);

    int index = 1;
    int spIndex = 0;
    for (final Parcel p : state.availableParcels) {
      serviceTimes[index] = DoubleMath.roundToInt(
        timeConverter.convert(p.getPickupDuration()), RoundingMode.CEILING);
      // add pickup location and time window
      points.add(p.getPickupLocation());
      final int deliveryIndex = index + state.availableParcels.size();
      final ParcelIndexObj pio = new ParcelIndexObj(p, index, deliveryIndex);
      parcel2indexBuilder.put(p, pio);
      index2parcelBuilder.put(index, pio);
      index2parcelBuilder.put(deliveryIndex, pio);

      final int[] tw = convertTW(p.getPickupTimeWindow(), state.time,
        timeConverter);
      releaseDates[index] = tw[0];
      dueDates[index] = tw[1];
      checkState(releaseDates[index] <= dueDates[index]);

      // link the pair with its delivery location (see next loop)
      servicePairs[spIndex++] = new int[] { index, deliveryIndex };

      index++;
    }
    checkState(spIndex == state.availableParcels.size(), "%s %s",
      state.availableParcels.size(), spIndex);

    final List<Parcel> deliveries = new ImmutableList.Builder<Parcel>()
      .addAll(state.availableParcels).addAll(inCargo).build();
    for (final Parcel p : deliveries) {
      serviceTimes[index] = DoubleMath.roundToInt(
        timeConverter.convert(p.getDeliveryDuration()), RoundingMode.CEILING);

      points.add(p.getDeliveryLocation());
      if (inCargo.contains(p)) {
        final ParcelIndexObj pio = new ParcelIndexObj(p, -1, index);
        parcel2indexBuilder.put(p, pio);
        index2parcelBuilder.put(index, pio);
      }

      final int[] tw = convertTW(p.getDeliveryTimeWindow(), state.time,
        timeConverter);
      releaseDates[index] = tw[0];
      dueDates[index] = tw[1];
      checkState(releaseDates[index] <= dueDates[index]);

      index++;
    }
    checkState(index == numLocations - 1);

    // the start position of the truck points to the depot location
    points.add(v.getDto().getStartPosition());

    // end of the day
    dueDates[index] = fixTWend(v.getDto().getAvailabilityTimeWindow().end,
      state.time,
      timeConverter);

    releaseDates[index] = Math.min(0, dueDates[index]);

    final Measure<Double, Velocity> speed = Measure.valueOf(v.getDto().getSpeed(),
      state.speedUnit);

    final ImmutableList<Point> pointList = points.build();
    final ImmutableMap<Parcel, ParcelIndexObj> parcel2indexMap = parcel2indexBuilder
      .build();
    final ImmutableMap<Integer, ParcelIndexObj> index2parcelMap = index2parcelBuilder
      .build();

    final int[][] travelTime = ArraysSolvers.toTravelTimeMatrix(pointList,
      state.distUnit, speed, outputTimeUnit, RoundingMode.CEILING);

    @Nullable
    SolutionObject[] sol = null;
    if (v.route.isPresent() && state.vehicles.size() == 1) {
      // the assumption is that if the current route of one vehicle is known,
      // the routes of all vehicles should be known.
      sol = toCurrentSolutions(state, parcel2indexMap, travelTime,
        releaseDates, dueDates, serviceTimes, new int[][] { travelTime[0] },
        new int[] { 0 });
    }
    return new ArraysObject(travelTime, releaseDates, dueDates, servicePairs,
      serviceTimes, sol, pointList, parcel2indexMap, index2parcelMap);
  }

  @Nullable
  static SolutionObject[] toCurrentSolutions(GlobalStateObject state,
    Map<Parcel, ParcelIndexObj> mapping, int[][] travelTime,
    int[] releaseDates, int[] dueDates, int[] serviceTimes,
    int[][] vehicleTravelTimes, int[] remainingServiceTimes) {
    final SolutionObject[] sols = new SolutionObject[state.vehicles.size()];
    for (int i = 0; i < state.vehicles.size(); i++) {
      sols[i] = convertRouteToSolutionObject(state, state.vehicles.get(i),
        mapping, travelTime, releaseDates, dueDates, serviceTimes,
        vehicleTravelTimes[i], remainingServiceTimes[i]);
    }
    return sols;
  }

  static SolutionObject convertRouteToSolutionObject(GlobalStateObject state,
    VehicleStateObject vso, Map<Parcel, ParcelIndexObj> mapping,
    int[][] travelTime, int[] releaseDates, int[] dueDates,
    int[] serviceTimes, int[] vehicleTravelTimes, int remainingServiceTime) {
    final int[] route = new int[vso.route.get().size() + 2];

    final Set<Parcel> seen = newHashSet();
    for (int i = 0; i < vso.route.get().size(); i++) {
      final Parcel dto = vso.route.get().get(i);

      if (vso.contents.contains(dto) || seen.contains(dto)) {
        // it is in cargo
        route[i + 1] = mapping.get(dto).deliveryIndex;
      } else {
        checkArgument(state.availableParcels.contains(dto),
          "This parcel should be available but is not: %s.", dto);
        // it is available
        route[i + 1] = mapping.get(dto).pickupIndex;
      }
      // TODO add error msg
      checkArgument(route[i + 1] > 0);
      seen.add(dto);
    }
    route[route.length - 1] = travelTime.length - 1;
    final int[] arrivalTimes = computeArrivalTimes(route, travelTime,
      remainingServiceTime, vehicleTravelTimes, serviceTimes, releaseDates);

    final int tardiness = computeRouteTardiness(route, arrivalTimes,
      serviceTimes, dueDates, remainingServiceTime);
    final int tt = computeTotalTravelTime(route, travelTime, vehicleTravelTimes);
    return new SolutionObject(route, arrivalTimes, tt + tardiness);
  }

  static int[] computeArrivalTimes(int[] route, int[][] travelTime,
    int remainingServiceTime, int[] vehicleTravelTimes, int[] serviceTimes,
    int[] releaseDates) {
    final int[] arrivalTimes = new int[route.length];

    checkArgument(route.length >= 2);
    checkArgument(route[0] == 0);

    arrivalTimes[0] = 0;

    for (int j = 1; j < route.length; j++) {
      final int prev = route[j - 1];
      final int cur = route[j];

      // we compute the travel time. If it is the first step in the
      // route, we use the time from vehicle location to the next
      // location in the route.
      final int tt = j == 1 ? vehicleTravelTimes[cur] : travelTime[prev][cur];

      if (j == 1 && remainingServiceTime > 0) {
        checkArgument(tt == 0, "%s", tt);
      }

      // service time is different in case we were already halfway with the
      // servicing (as defined by remainingServiceTime)
      final int st = j == 2 && remainingServiceTime > 0 ? remainingServiceTime
        : serviceTimes[prev];
      // we compute the first possible arrival time for the vehicle to
      // arrive at location i, given that it first visited location
      // i-1
      final int earliestArrivalTime = arrivalTimes[j - 1] + st + tt;

      // we also have to take into account the time window
      final int minArrivalTime = Math.max(earliestArrivalTime,
        releaseDates[cur]);
      arrivalTimes[j] = minArrivalTime;
    }
    return arrivalTimes;
  }

  /**
   * Converts the specified {@link GlobalStateObject} into an
   * {@link MVArraysObject} using the specified time unit.
   * @param state The state to convert.
   * @param outputTimeUnit The unit to use for time.
   * @return A {@link MVArraysObject} using the specified output time unit.
   */
  public static MVArraysObject toMultiVehicleArrays(GlobalStateObject state,
    Unit<Duration> outputTimeUnit) {
    final ArraysObject singleVehicleArrays = toSingleVehicleArrays(state,
      outputTimeUnit);
    checkArgument(!state.vehicles.isEmpty(), "We need at least one vehicle");

    final int[][] vehicleTravelTimes = toVehicleTravelTimes(state,
      singleVehicleArrays, outputTimeUnit);
    final int[][] inventories = toInventoriesArray(state, singleVehicleArrays);
    final int[] remainingServiceTimes = toRemainingServiceTimes(state,
      outputTimeUnit);

    final int[] currentDestinations = toVehicleDestinations(state,
      singleVehicleArrays);

    @Nullable
    SolutionObject[] sols = null;
    if (state.vehicles.iterator().next().route.isPresent()) {
      // the assumption is that if the current route of one vehicle is known,
      // the routes of all vehicles should be known.
      sols = toCurrentSolutions(state, singleVehicleArrays.parcel2index,
        singleVehicleArrays.travelTime, singleVehicleArrays.releaseDates,
        singleVehicleArrays.dueDates, singleVehicleArrays.serviceTimes,
        vehicleTravelTimes, remainingServiceTimes);
    }

    return new MVArraysObject(singleVehicleArrays, sols, vehicleTravelTimes,
      inventories, remainingServiceTimes, currentDestinations);
  }

  /**
   * Converts a {@link SolutionObject} into a list of {@link Parcel}s.
   * @param sol The solution to convert.
   * @param index2parcel Mapping of indices to {@link Parcel}s.
   * @return A list containing the route as specified by the
   *         {@link SolutionObject}.
   */
  public static ImmutableList<Parcel> convertSolutionObject(
    SolutionObject sol, Map<Integer, ParcelIndexObj> index2parcel) {
    final ImmutableList.Builder<Parcel> builder = ImmutableList.builder();
    // ignore first (current pos) and last (depot)
    for (int i = 1; i < sol.route.length - 1; i++) {
      builder.add(index2parcel.get(sol.route[i]).dto);
    }
    return builder.build();
  }

  static int[] toVehicleDestinations(GlobalStateObject state, ArraysObject sva) {
    final int v = state.vehicles.size();
    final UnmodifiableIterator<VehicleStateObject> iterator = state.vehicles
      .iterator();

    final int[] destinations = new int[v];
    for (int i = 0; i < v; i++) {
      final VehicleStateObject cur = iterator.next();
      final Parcel dest = cur.destination;
      if (dest != null) {
        checkArgument(sva.parcel2index.containsKey(dest));
        final boolean isInCargo = cur.contents.contains(dest);
        final ParcelIndexObj pio = sva.parcel2index.get(dest);
        final int index = isInCargo ? pio.deliveryIndex : pio.pickupIndex;
        destinations[i] = index;
      } else {
        destinations[i] = 0;
      }
      checkArgument(destinations[i] >= 0, "Invalid destination.", dest);
    }
    return destinations;
  }

  static int[][] toVehicleTravelTimes(GlobalStateObject state,
    ArraysObject sva, Unit<Duration> outputTimeUnit) {
    final int v = state.vehicles.size();
    final int n = sva.travelTime.length;
    // compute vehicle travel times
    final int[][] vehicleTravelTimes = new int[v][n];

    final UnmodifiableIterator<VehicleStateObject> iterator = state.vehicles
      .iterator();

    for (int i = 0; i < v; i++) {
      final VehicleStateObject cur = iterator.next();
      final Measure<Double, Velocity> speed = Measure.valueOf(
        cur.getDto().getSpeed(),
        state.speedUnit);

      final Parcel dest = cur.destination;
      if (dest != null) {
        // only add travel time for current dest
        for (int j = 1; j < n; j++) {
          vehicleTravelTimes[i][j] = Integer.MAX_VALUE;
        }
        final boolean isInCargo = cur.contents.contains(dest);
        final ParcelIndexObj pio = sva.parcel2index.get(dest);
        final int index = isInCargo ? pio.deliveryIndex : pio.pickupIndex;

        checkArgument(index > 0);
        vehicleTravelTimes[i][index] = computeRoundedTravelTime(speed,
          Measure.valueOf(
            Point.distance(cur.location, sva.location2index.get(index)),
            state.distUnit), outputTimeUnit);

      } else {
        // add travel time for every location
        for (int j = 1; j < n; j++) {
          vehicleTravelTimes[i][j] = computeRoundedTravelTime(speed,
            Measure.valueOf(
              Point.distance(cur.location, sva.location2index.get(j)),
              state.distUnit), outputTimeUnit);
        }
      }
    }
    return vehicleTravelTimes;
  }

  static int computeRoundedTravelTime(Measure<Double, Velocity> speed,
    Measure<Double, Length> dist, Unit<Duration> outputTimeUnit) {
    return DoubleMath.roundToInt(
      RoadModels.computeTravelTime(speed, dist, outputTimeUnit),
      RoundingMode.CEILING);
  }

  static int[][] toInventoriesArray(GlobalStateObject state, ArraysObject sva) {
    final UnmodifiableIterator<VehicleStateObject> iterator = state.vehicles
      .iterator();

    final ImmutableList.Builder<ImmutableList<Integer>> invPairBuilder = ImmutableList
      .builder();
    for (int i = 0; i < state.vehicles.size(); i++) {
      final VehicleStateObject cur = iterator.next();
      for (final Parcel dp : cur.contents) {
        invPairBuilder.add(ImmutableList.of(i,
          sva.parcel2index.get(dp).deliveryIndex));
      }
    }
    final ImmutableList<ImmutableList<Integer>> inventoryPairs = invPairBuilder
      .build();

    final int[][] inventories = new int[inventoryPairs.size()][2];
    for (int i = 0; i < inventoryPairs.size(); i++) {
      inventories[i][0] = inventoryPairs.get(i).get(0);
      inventories[i][1] = inventoryPairs.get(i).get(1);
    }
    return inventories;
  }

  static int[] toRemainingServiceTimes(GlobalStateObject state,
    Unit<Duration> outputTimeUnit) {
    final UnmodifiableIterator<VehicleStateObject> iterator = state.vehicles
      .iterator();
    final int[] remainingServiceTimes = new int[state.vehicles.size()];
    for (int i = 0; i < state.vehicles.size(); i++) {
      remainingServiceTimes[i] = DoubleMath.roundToInt(
        Measure.valueOf(iterator.next().remainingServiceTime, state.timeUnit)
          .doubleValue(outputTimeUnit), RoundingMode.CEILING);
    }
    return remainingServiceTimes;
  }

  /**
   * Computes the total travel time of the specified route.
   * @param route The route.
   * @param travelTime The travel time matrix.
   * @param vehicleTravelTimes The vehicle travel time for the vehicle that is
   *          driving the specified route.
   * @return The travel time of the specified route.
   */
  public static int computeTotalTravelTime(int[] route, int[][] travelTime,
    int[] vehicleTravelTimes) {
    int totalTravelTime = 0;
    for (int i = 1; i < route.length; i++) {
      if (i == 1) {
        totalTravelTime += vehicleTravelTimes[route[i]];
      } else {
        totalTravelTime += travelTime[route[i - 1]][route[i]];
      }
    }
    return totalTravelTime;
  }

  /**
   * Computes the total tardiness of the specified route with the specified
   * arrivalTimes.
   * @param route The route with length &ge; 2.
   * @param arrivalTimes The arrival times at every index of the route.
   * @param serviceTimes The full serviceTimes array containing all locations,
   *          using the original indices.
   * @param dueDates The full dueDates array containing all locations, using the
   *          original indices.
   * @param remainingServiceTime The remaining service time for the position at
   *          index 1, if any, 0 otherwise.
   * @return The sum tardiness.
   */
  public static int computeRouteTardiness(int[] route, int[] arrivalTimes,
    int[] serviceTimes, int[] dueDates, int remainingServiceTime) {
    int tardiness = 0;
    // start at index 1 since there can be no tardiness at start location
    for (int i = 1; i < route.length; i++) {
      int st;
      if (i == 1 && remainingServiceTime > 0) {
        st = remainingServiceTime;
      } else {
        st = serviceTimes[route[i]];
      }
      final int lateness = arrivalTimes[i] + st - dueDates[route[i]];
      if (lateness > 0) {
        tardiness += lateness;
      }
    }
    return tardiness;
  }

  /**
   * Sums the objective values of all provided {@link SolutionObject}s.
   * @param sols The {@link SolutionObject}s.
   * @return The sum objective value.
   */
  public static int computeTotalObjectiveValue(SolutionObject[] sols) {
    int obj = 0;
    for (final SolutionObject sol : sols) {
      obj += sol.objectiveValue;
    }
    return obj;
  }

  /**
   * Sums the objective values of all provided {@link SolutionObject}s. The
   * input values are treated as instances of the <code>inputUnit</code> and are
   * converted to the <code>outputUnit</code>.
   * @param sols The {@link SolutionObject}s.
   * @param inputUnit The time unit of the input values.
   * @param outputUnit The time unit to convert the values to.
   * @return The sum objective value in the <code>outputUnit</code>.
   */
  public static int computeTotalObjectiveValue(SolutionObject[] sols,
    Unit<Duration> inputUnit, Unit<Duration> outputUnit) {
    return Measure.valueOf(computeTotalObjectiveValue(sols), inputUnit)
      .intValue(outputUnit);
  }

  static int[] convertTW(TimeWindow tw, long time, UnitConverter timeConverter) {
    final int releaseDate = fixTWstart(tw.begin, time, timeConverter);
    final int dueDate = fixTWend(tw.end, time, timeConverter);
    if (releaseDate > dueDate) {
      // if this happens, we know this is the result of rounding behavior:
      // release is rounded up, due is rounded down. We also know that the
      // difference is only 1. Therefore we flip the values.
      checkArgument(Math.abs(dueDate - releaseDate) == 1);
      return new int[] { dueDate, releaseDate };
    }
    return new int[] { releaseDate, dueDate };
  }

  static int fixTWstart(long start, long time, UnitConverter timeConverter) {
    return DoubleMath.roundToInt(timeConverter.convert(start - time),
      RoundingMode.CEILING);
  }

  static int fixTWend(long end, long time, UnitConverter timeConverter) {
    return DoubleMath.roundToInt(timeConverter.convert(end - time),
      RoundingMode.FLOOR);
  }

  /**
   * Object which specifies the parameters of
   * {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
   * . Also includes additional information which is required to interpret the
   * resulting {@link SolutionObject}.
   * @author Rinde van Lon
   */
  public static class ArraysObject {
    /**
     * See
     * {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
     * .
     */
    public final int[][] travelTime;

    /**
     * See
     * {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
     * .
     */
    public final int[] releaseDates;

    /**
     * See
     * {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
     * .
     */
    public final int[] dueDates;

    /**
     * See
     * {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
     * .
     */
    public final int[][] servicePairs;

    /**
     * See
     * {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
     * .
     */
    public final int[] serviceTimes;

    /**
     * See
     * {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
     * .
     */
    @Nullable
    public final SolutionObject[] currentSolutions;

    /**
     * A bidirectional mapping between locations and their index.
     */
    public final ImmutableList<Point> location2index;

    /**
     * A mapping between parcels and their locations.
     */
    public final ImmutableMap<Parcel, ParcelIndexObj> parcel2index;

    /**
     * A mapping between indices and parcels/locations.
     */
    public final ImmutableMap<Integer, ParcelIndexObj> index2parcel;

    ArraysObject(int[][] travelTime, int[] releaseDates, int[] dueDates,
      int[][] servicePairs, int[] serviceTimes,
      @Nullable SolutionObject[] currentSolutions,
      ImmutableList<Point> locations,
      ImmutableMap<Parcel, ParcelIndexObj> parcel2index,
      ImmutableMap<Integer, ParcelIndexObj> index2parcel) {
      this.travelTime = travelTime;
      this.releaseDates = releaseDates;
      this.dueDates = dueDates;
      this.servicePairs = servicePairs;
      this.serviceTimes = serviceTimes;
      this.currentSolutions = currentSolutions;
      location2index = locations;
      this.parcel2index = parcel2index;
      this.index2parcel = index2parcel;
    }

    ArraysObject(int[][] travelTime, int[] releaseDates, int[] dueDates,
      int[][] servicePairs, int[] serviceTimes,
      @Nullable SolutionObject[] currentSolutions) {
      this(travelTime, releaseDates, dueDates, servicePairs, serviceTimes,
        currentSolutions, ImmutableList.<Point> of(), ImmutableMap
          .<Parcel, ParcelIndexObj> of(), ImmutableMap
          .<Integer, ParcelIndexObj> of());
    }
  }

  /**
   * Object which specifies the parameters of
   * {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
   * . Also includes additional information which is required to interpret the
   * resulting {@link SolutionObject}.
   * @author Rinde van Lon
   */
  public static class MVArraysObject extends ArraysObject {
    /**
     * See
     * {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
     * .
     */
    public final int[][] vehicleTravelTimes;

    /**
     * See
     * {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
     * .
     */
    public final int[][] inventories;

    /**
     * See
     * {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
     * .
     */
    public final int[] remainingServiceTimes;

    /**
     * See
     * {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[], int[], SolutionObject[])}
     * .
     */
    public final int[] currentDestinations;

    MVArraysObject(int[][] travelTime, int[] releaseDates, int[] dueDates,
      int[][] servicePairs, int[] serviceTimes,
      @Nullable SolutionObject[] currentSolutions,
      ImmutableList<Point> locations,
      ImmutableMap<Parcel, ParcelIndexObj> parcel2index,
      ImmutableMap<Integer, ParcelIndexObj> index2parcel,
      int[][] vehicleTravelTimes, int[][] inventories,
      int[] remainingServiceTimes, int[] currentDestinations) {
      super(travelTime, releaseDates, dueDates, servicePairs, serviceTimes,
        currentSolutions, locations, parcel2index, index2parcel);
      this.vehicleTravelTimes = Arrays.copyOf(vehicleTravelTimes,
        vehicleTravelTimes.length);
      this.inventories = Arrays.copyOf(inventories, inventories.length);
      this.remainingServiceTimes = Arrays.copyOf(remainingServiceTimes,
        remainingServiceTimes.length);
      this.currentDestinations = Arrays.copyOf(currentDestinations,
        currentDestinations.length);
    }

    MVArraysObject(ArraysObject ao,
      @Nullable SolutionObject[] currentSolutions,
      int[][] vehicleTravelTimes, int[][] inventories,
      int[] remainingServiceTimes, int[] currentDestinations) {
      this(ao.travelTime, ao.releaseDates, ao.dueDates, ao.servicePairs,
        ao.serviceTimes, currentSolutions, ao.location2index,
        ao.parcel2index, ao.index2parcel, vehicleTravelTimes, inventories,
        remainingServiceTimes, currentDestinations);
    }

    MVArraysObject(int[][] travelTime, int[] releaseDates, int[] dueDates,
      int[][] servicePairs, int[] serviceTimes, int[][] vehicleTravelTimes,
      int[][] inventories, int[] remainingServiceTimes,
      int[] currentDestinations, @Nullable SolutionObject[] curSolutions) {
      super(travelTime, releaseDates, dueDates, servicePairs, serviceTimes,
        curSolutions);
      this.vehicleTravelTimes = Arrays.copyOf(vehicleTravelTimes,
        vehicleTravelTimes.length);
      this.inventories = Arrays.copyOf(inventories, inventories.length);
      this.remainingServiceTimes = Arrays.copyOf(remainingServiceTimes,
        remainingServiceTimes.length);
      this.currentDestinations = Arrays.copyOf(currentDestinations,
        currentDestinations.length);
    }
  }

  static class ParcelIndexObj {
    final Parcel dto;
    final int pickupIndex;
    final int deliveryIndex;

    ParcelIndexObj(Parcel dto, int pickupIndex, int deliveryIndex) {
      this.dto = dto;
      this.pickupIndex = pickupIndex;
      this.deliveryIndex = deliveryIndex;
    }
  }
}
