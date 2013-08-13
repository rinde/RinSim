/**
 * 
 */
package rinde.sim.pdptw.central.solver;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Arrays.asList;

import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.measure.Measure;
import javax.measure.converter.UnitConverter;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.central.GlobalStateObject;
import rinde.sim.pdptw.central.GlobalStateObject.VehicleState;
import rinde.sim.problem.common.DefaultParcel;
import rinde.sim.problem.common.ParcelDTO;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.math.DoubleMath;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public final class ArraysSolvers {

    private ArraysSolvers() {}

    /**
     * Converts the list of points on a plane into a travel time matrix. For
     * distance between two points the euclidean distance is used, i.e. no
     * obstacles or graph structure are considered. See
     * {@link #toTravelTimeMatrix(List, Unit, Measure, Unit, RoundingMode)} for
     * more options.
     * @param points The set of points which will be converted to a travel time
     *            matrix.
     * @param speed the speed in m/s.
     * @param rm The rounding mode, see {@link RoundingMode}.
     * @return A <code>n x n</code> travel time matrix, where <code>n</code> is
     *         the size of the <code>points</code> list.
     */
    public static int[][] toTravelTimeMatrix(List<Point> points, double speed,
            RoundingMode rm) {
        return toTravelTimeMatrix(points, SI.METER, Measure.valueOf(speed, SI.METERS_PER_SECOND), SI.SECOND, rm);
    }

    /**
     * Converts the list of points on a plane into a travel time matrix. For
     * distance between two points the euclidean distance is used, i.e. no
     * obstacles or graph structure are considered.
     * @param points The set of points which will be converted to a travel time
     *            matrix.
     * @param distUnit The {@link Unit} that is used for distances (
     *            {@link Length}) between the specified points.
     * @param speed The travel speed specified as a {@link Measure} which
     *            includes its {@link Unit}.
     * @param outputTimeUnit The output time {@link Unit} to which all times are
     *            converted, e.g. if {@link SI#SECOND} is specified the travel
     *            times will be in seconds.
     * @param rm When computing the travel times they often need to be rounded.
     *            The rounding mode indicates how numbers are rounded, see
     *            {@link RoundingMode} for the available options.
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
                    final Measure<Double, Length> dist = Measure.valueOf(Point
                            .distance(points.get(i), points.get(j)), distUnit);
                    // calculate duration in desired unit
                    final double duration = computeTravelTime(speed, dist, outputTimeUnit);
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
     *            object.
     * @return An {@link ArraysObject} using the specified output time unit.
     */
    public static ArraysObject toSingleVehicleArrays(GlobalStateObject state,
            Unit<Duration> outputTimeUnit) {

        final UnitConverter timeConverter = state.timeUnit
                .getConverterTo(outputTimeUnit);

        final VehicleState v = state.vehicles.iterator().next();
        final Collection<ParcelDTO> inCargo = v.contents;
        // there are always two locations: the current vehicle location and
        // the depot
        final int numLocations = 2 + (state.availableParcels.size() * 2)
                + inCargo.size();

        final int[] releaseDates = new int[numLocations];
        final int[] dueDates = new int[numLocations];
        final int[][] servicePairs = new int[state.availableParcels.size()][2];
        final int[] serviceTimes = new int[numLocations];

        final Map<Point, ParcelDTO> point2dto = newHashMap();
        final Point[] locations = new Point[numLocations];
        locations[0] = v.location;

        int index = 1;
        int spIndex = 0;
        for (final ParcelDTO p : state.availableParcels) {
            serviceTimes[index] = DoubleMath.roundToInt(timeConverter
                    .convert(p.pickupDuration), RoundingMode.CEILING);
            // add pickup location and time window
            locations[index] = p.pickupLocation;
            point2dto.put(locations[index], p);
            releaseDates[index] = fixTWstart(p.pickupTimeWindow.begin, state.time, timeConverter);
            dueDates[index] = fixTWend(p.pickupTimeWindow.end, state.time, timeConverter);

            // link the pair with its delivery location (see next loop)
            servicePairs[spIndex++] = new int[] { index,
                    index + state.availableParcels.size() };

            index++;
        }
        checkState(spIndex == state.availableParcels.size(), "%s %s", state.availableParcels
                .size(), spIndex);

        final List<ParcelDTO> deliveries = newArrayListWithCapacity(state.availableParcels
                .size() + inCargo.size());
        deliveries.addAll(state.availableParcels);
        deliveries.addAll(inCargo);
        for (final ParcelDTO p : deliveries) {
            serviceTimes[index] = DoubleMath.roundToInt(timeConverter
                    .convert(p.deliveryDuration), RoundingMode.CEILING);

            locations[index] = p.destinationLocation;
            point2dto.put(locations[index], p);
            releaseDates[index] = fixTWstart(p.deliveryTimeWindow.begin, state.time, timeConverter);
            dueDates[index] = fixTWend(p.deliveryTimeWindow.end, state.time, timeConverter);
            index++;
        }
        checkState(index == numLocations - 1);

        // the start position of the truck is the depot
        locations[index] = v.startPosition;
        // end of the day
        dueDates[index] = fixTWend(v.availabilityTimeWindow.end, state.time, timeConverter);

        final Measure<Double, Velocity> speed = Measure
                .valueOf(v.speed, state.speedUnit);

        final int[][] travelTime = ArraysSolvers
                .toTravelTimeMatrix(asList(locations), state.distUnit, speed, outputTimeUnit, RoundingMode.CEILING);

        return new ArraysObject(travelTime, releaseDates, dueDates,
                servicePairs, serviceTimes, locations, point2dto);
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
        final ArraysObject singleVehicleArrays = toSingleVehicleArrays(state, outputTimeUnit);
        checkArgument(state.vehicles.size() > 0, "We need at least one vehicle");

        final int[][] vehicleTravelTimes = toVehicleTravelTimes(state, singleVehicleArrays, outputTimeUnit);
        final int[][] inventories = toInventoriesArray(state, singleVehicleArrays);
        final int[] remainingServiceTimes = toRemainingServiceTimes(state, outputTimeUnit);
        return new MVArraysObject(singleVehicleArrays, vehicleTravelTimes,
                inventories, remainingServiceTimes);
    }

    /**
     * Converts a {@link SolutionObject} into a {@link Queue} which conforms to
     * the return value of {@link Solver#solve(GlobalStateObject)}.
     * @param sol The solution to convert.
     * @param point2dto A mapping of locations to parcels.
     * @param locations A list of locations which is used to lookup the
     *            positions in the {@link SolutionObject#route}.
     * @return A queue containing the route as specified by the
     *         {@link SolutionObject}.
     */
    public static ImmutableList<ParcelDTO> convertSolutionObject(
            SolutionObject sol, Map<Point, ParcelDTO> point2dto,
            List<Point> locations) {
        final ImmutableList.Builder<ParcelDTO> builder = ImmutableList
                .builder();
        // ignore first (current pos) and last (depot)
        for (int i = 1; i < sol.route.length - 1; i++) {
            builder.add(point2dto.get(locations.get(sol.route[i])));
        }
        return builder.build();
    }

    static int[][] toVehicleTravelTimes(GlobalStateObject state,
            ArraysObject sva, Unit<Duration> outputTimeUnit) {
        final int v = state.vehicles.size();
        final int n = sva.travelTime.length;
        // compute vehicle travel times
        final int[][] vehicleTravelTimes = new int[v][n];

        final UnmodifiableIterator<VehicleState> iterator = state.vehicles
                .iterator();

        for (int i = 0; i < v; i++) {
            final VehicleState cur = iterator.next();
            for (int j = 1; j < n; j++) {
                final double dist = Point.distance(cur.location, sva.locations
                        .get(j));

                final Measure<Double, Velocity> speed = Measure
                        .valueOf(cur.speed, state.speedUnit);
                final double duration = computeTravelTime(speed, Measure.valueOf(dist, state.distUnit), outputTimeUnit);
                vehicleTravelTimes[i][j] = DoubleMath
                        .roundToInt(duration, RoundingMode.CEILING);
            }
        }
        return vehicleTravelTimes;
    }

    static int[][] toInventoriesArray(GlobalStateObject state, ArraysObject sva) {
        final ImmutableMap.Builder<Point, Integer> point2indexBuilder = ImmutableMap
                .builder();
        for (int i = 0; i < sva.locations.size(); i++) {
            point2indexBuilder.put(sva.locations.get(i), i);
        }
        final Map<Point, Integer> point2index = point2indexBuilder.build();
        final UnmodifiableIterator<VehicleState> iterator = state.vehicles
                .iterator();
        final List<List<Integer>> inventoryPairs = newArrayList();
        for (int i = 0; i < state.vehicles.size(); i++) {
            final VehicleState cur = iterator.next();
            for (final ParcelDTO dp : cur.contents) {
                inventoryPairs.add(newArrayList(i, point2index
                        .get(dp.destinationLocation)));
            }
        }

        final int[][] inventories = new int[inventoryPairs.size()][2];
        for (int i = 0; i < inventoryPairs.size(); i++) {
            inventories[i][0] = inventoryPairs.get(i).get(0);
            inventories[i][1] = inventoryPairs.get(i).get(1);
        }
        return inventories;
    }

    static int[] toRemainingServiceTimes(GlobalStateObject state,
            Unit<Duration> outputTimeUnit) {
        final UnmodifiableIterator<VehicleState> iterator = state.vehicles
                .iterator();
        final int[] remainingServiceTimes = new int[state.vehicles.size()];
        for (int i = 0; i < state.vehicles.size(); i++) {
            remainingServiceTimes[i] = DoubleMath
                    .roundToInt(Measure
                            .valueOf(iterator.next().remainingServiceTime, state.timeUnit)
                            .doubleValue(outputTimeUnit), RoundingMode.CEILING);
        }
        return remainingServiceTimes;
    }

    /**
     * Computes the duration which is required to travel the specified distance
     * with the given velocity. Note: although time is normally as a long, we
     * use double here instead. Converting it to long in this method would
     * introduce rounding in a too early stage.
     * @param speed
     * @param distance
     * @param outputTimeUnit
     * @return
     */
    static double computeTravelTime(Measure<Double, Velocity> speed,
            Measure<Double, Length> distance, Unit<Duration> outputTimeUnit) {
        return Measure.valueOf(distance.doubleValue(SI.METER)// meters
                / speed.doubleValue(SI.METERS_PER_SECOND), // divided by m/s
        SI.SECOND) // gives seconds
                .doubleValue(outputTimeUnit); // convert to desired unit
    }

    /**
     * Object which specifies the parameters of
     * {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[])}
     * . Also includes additional information which is required to interpret the
     * resulting {@link SolutionObject}.
     * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
     */
    public static class ArraysObject {
        /**
         * See
         * {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[])}
         * .
         */
        public final int[][] travelTime;

        /**
         * See
         * {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[])}
         * .
         */
        public final int[] releaseDates;

        /**
         * See
         * {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[])}
         * .
         */
        public final int[] dueDates;

        /**
         * See
         * {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[])}
         * .
         */
        public final int[][] servicePairs;

        /**
         * See
         * {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[])}
         * .
         */
        public final int[] serviceTimes;

        /**
         * A list of the actual locations, in the same order as the
         * {@link #travelTime} matrix.
         */
        public final ImmutableList<Point> locations;

        /**
         * A mapping of location to {@link DefaultParcel}.
         */
        public final ImmutableMap<Point, ParcelDTO> point2dto;

        ArraysObject(int[][] travelTime, int[] releaseDates, int[] dueDates,
                int[][] servicePairs, int[] serviceTimes, Point[] locations,
                Map<Point, ParcelDTO> point2dto) {
            this.travelTime = travelTime;
            this.releaseDates = releaseDates;
            this.dueDates = dueDates;
            this.servicePairs = servicePairs;
            this.serviceTimes = serviceTimes;
            this.locations = ImmutableList.copyOf(asList(locations));
            this.point2dto = ImmutableMap.copyOf(point2dto);
        }
    }

    /**
     * Object which specifies the parameters of
     * {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[])}
     * . Also includes additional information which is required to interpret the
     * resulting {@link SolutionObject}.
     * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
     */
    public static class MVArraysObject extends ArraysObject {
        /**
         * See
         * {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[])}
         * .
         */
        public final int[][] vehicleTravelTimes;

        /**
         * See
         * {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[])}
         * .
         */
        public final int[][] inventories;

        /**
         * See
         * {@link MultiVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], int[][], int[][], int[])}
         * .
         */
        public final int[] remainingServiceTimes;

        MVArraysObject(int[][] travelTime, int[] releaseDates, int[] dueDates,
                int[][] servicePairs, int[] serviceTimes, Point[] locations,
                Map<Point, ParcelDTO> point2dto, int[][] vehicleTravelTimes,
                int[][] inventories, int[] remainingServiceTimes) {
            super(travelTime, releaseDates, dueDates, servicePairs,
                    serviceTimes, locations, point2dto);
            this.vehicleTravelTimes = vehicleTravelTimes;
            this.inventories = inventories;
            this.remainingServiceTimes = remainingServiceTimes;
        }

        MVArraysObject(ArraysObject ao, int[][] vehicleTravelTimes,
                int[][] inventories, int[] remainingServiceTimes) {
            this(ao.travelTime, ao.releaseDates, ao.dueDates, ao.servicePairs,
                    ao.serviceTimes, ao.locations
                            .toArray(new Point[ao.locations.size()]),
                    ao.point2dto, vehicleTravelTimes, inventories,
                    remainingServiceTimes);
        }
    }

    static int fixTWstart(long start, long time, UnitConverter timeConverter) {
        return Math.max((DoubleMath.roundToInt(timeConverter.convert(start
                - time), RoundingMode.CEILING)), 0);
    }

    static int fixTWend(long end, long time, UnitConverter timeConverter) {
        return Math.max((DoubleMath.roundToInt(timeConverter
                .convert(end - time), RoundingMode.FLOOR)), 0);
    }
}
