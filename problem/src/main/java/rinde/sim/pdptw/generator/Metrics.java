/**
 * 
 */
package rinde.sim.pdptw.generator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.primitives.Ints.checkedCast;
import static java.util.Collections.nCopies;

import java.math.RoundingMode;
import java.util.List;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.TimedEvent;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public final class Metrics {

    private Metrics() {}

    /**
     * Computes the absolute load at every time instance of the specified
     * scenario. Load is a measure of expected vehicle utilization.
     * @param s The {@link Scenario} to measure.
     * @return A list of load values. The value at index <code>i</code>
     *         indicates the load at time <code>i</code>. All values are always
     *         <code> >= 0</code>. All time instances not included in the list
     *         are assumed to have load <code>0</code>.
     */
    public static ImmutableList<Double> measureLoad(Scenario s) {
        final double vehicleSpeed = getVehicleSpeed(s);
        final ImmutableList.Builder<LoadPart> loadParts = ImmutableList
                .builder();
        for (final TimedEvent te : s.asList()) {
            if (te instanceof AddParcelEvent) {
                loadParts
                        .addAll(measureLoad((AddParcelEvent) te, vehicleSpeed));
            }
        }
        return sum(0, loadParts.build()).load;
    }

    public static ImmutableList<LoadPart> measureLoad(AddParcelEvent event,
            double vehicleSpeed) {
        checkArgument(vehicleSpeed > 0d);
        checkArgument(event.parcelDTO.pickupTimeWindow.begin <= event.parcelDTO.deliveryTimeWindow.begin, "Delivery TW begin may not be before pickup TW begin.");
        checkArgument(event.parcelDTO.pickupTimeWindow.end <= event.parcelDTO.deliveryTimeWindow.end, "Delivery TW end may not be before pickup TW end.");

        // pickup lower bound,
        final long pickupLb = event.parcelDTO.pickupTimeWindow.begin;
        // pickup upper bound
        final long pickupUb = event.parcelDTO.pickupTimeWindow.end
                + event.parcelDTO.pickupDuration;
        final double pickupLoad = event.parcelDTO.pickupDuration
                / (double) (pickupUb - pickupLb);
        final LoadPart pickupPart = new LoadPart(pickupLb,
                nCopies(checkedCast(pickupUb - pickupLb), pickupLoad));

        final long expectedTravelTime = travelTime(event.parcelDTO.pickupLocation, event.parcelDTO.destinationLocation, vehicleSpeed);
        // first possible departure time from pickup location
        final long travelLb = pickupLb + event.parcelDTO.pickupDuration;
        // latest possible arrival time at delivery location
        final long travelUb = Math
                .max(event.parcelDTO.deliveryTimeWindow.end, travelLb
                        + expectedTravelTime);

        // checkArgument(travelUb - travelLb >= expectedTravelTime,
        // "The time windows should allow travelling from pickup to delivery.");

        final double travelLoad = expectedTravelTime
                / (double) (travelUb - travelLb);
        final LoadPart travelPart = new LoadPart(travelLb,
                nCopies(checkedCast(travelUb - travelLb), travelLoad));

        // delivery lower bound: the first possible time the delivery can start,
        // normally uses the start of the delivery TW, in case this is not
        // feasible we correct for the duration and travel time.
        final long deliveryLb = Math
                .max(event.parcelDTO.deliveryTimeWindow.begin, pickupLb
                        + event.parcelDTO.pickupDuration + expectedTravelTime);
        // delivery upper bound: the latest possible time the delivery can end
        final long deliveryUb = Math
                .max(event.parcelDTO.deliveryTimeWindow.end, deliveryLb)
                + event.parcelDTO.deliveryDuration;
        final double deliveryLoad = event.parcelDTO.deliveryDuration
                / (double) (deliveryUb - deliveryLb);
        final LoadPart deliveryPart = new LoadPart(deliveryLb,
                nCopies(checkedCast(deliveryUb - deliveryLb), deliveryLoad));

        return ImmutableList.of(pickupPart, travelPart, deliveryPart);
    }

    static LoadPart sum(long st, List<LoadPart> parts) {
        final ImmutableList.Builder<Double> builder = ImmutableList.builder();
        long i = st;
        boolean withinBounds = true;
        while (withinBounds) {
            double currentLoadVal = 0d;
            withinBounds = false;
            for (final LoadPart lp : parts) {
                final long relativeTime = i - lp.startTime;
                if (relativeTime >= 0 && relativeTime < lp.load.size()) {
                    currentLoadVal += lp.load.get((int) relativeTime);
                }
                if (relativeTime < lp.load.size()) {
                    // as long as one of the LoadParts is still within bounds,
                    // we continue
                    withinBounds = true;
                }
            }
            if (withinBounds) {
                builder.add(currentLoadVal);
                i++;
            }
        }
        return new LoadPart(st, builder.build());
    }

    /**
     * Checks whether the vehicles defined for the specified scenario have the
     * same speed.
     * @param s The {@link Scenario} to get the speed from.
     * @return The vehicle speed if all vehicles have the same speed.
     * @throws IllegalArgumentException if either: not all vehicles have the
     *             same speed, or there are no vehicles.
     */
    public static double getVehicleSpeed(Scenario s) {
        double vehicleSpeed = -1d;
        for (final TimedEvent te : s.asList()) {
            if (te instanceof AddVehicleEvent) {
                if (vehicleSpeed == -1d) {
                    vehicleSpeed = ((AddVehicleEvent) te).vehicleDTO.speed;
                } else {
                    checkArgument(vehicleSpeed == ((AddVehicleEvent) te).vehicleDTO.speed, "All vehicles are expected to have the same speed.");
                }
            }
        }
        checkArgument(vehicleSpeed > 0, "There are no vehicles in the scenario.");
        return vehicleSpeed;
    }

    public static void checkTimeWindowStrictness(Scenario s) {
        final double speed = getVehicleSpeed(s);
        for (final TimedEvent te : s.asList()) {
            if (te instanceof AddParcelEvent) {
                Metrics.checkParcelTWStrictness((AddParcelEvent) te, speed);
            }
        }
    }

    /**
     * Checks whether the TWs are not unnecessarily big.
     * @param event
     * @param vehicleSpeed
     */
    public static void checkParcelTWStrictness(AddParcelEvent event,
            double vehicleSpeed) {
        final long firstDepartureTime = event.parcelDTO.pickupTimeWindow.begin
                + event.parcelDTO.pickupDuration;
        final long latestDepartureTime = event.parcelDTO.pickupTimeWindow.end
                + event.parcelDTO.pickupDuration;

        final long travelTime = travelTime(event.parcelDTO.pickupLocation, event.parcelDTO.destinationLocation, vehicleSpeed);

        checkArgument(event.parcelDTO.deliveryTimeWindow.begin >= firstDepartureTime
                + travelTime, "The begin of the delivery time window is too early.");
        checkArgument(latestDepartureTime + travelTime <= event.parcelDTO.deliveryTimeWindow.end, "The end of the pickup time window %s is too late.", event.parcelDTO.pickupTimeWindow.end);
    }

    // to use for parts of the timeline to avoid excessively long list with
    // mostly 0s.
    static class LoadPart {
        final long startTime;
        final ImmutableList<Double> load;

        public LoadPart(long st, List<Double> loadList) {
            startTime = st;
            load = ImmutableList.copyOf(loadList);
        }

        @Override
        public String toString() {
            return Objects.toStringHelper("LoadPart")
                    .add("startTime", startTime).add("load", load).toString();
        }
    }

    // TODO
    public static void computeStress() {}

    public static double measureDynamism(Scenario s) {
        final List<TimedEvent> list = s.asList();
        final ImmutableList.Builder<Long> times = ImmutableList.builder();
        for (final TimedEvent te : list) {
            if (te instanceof AddParcelEvent) {
                times.add(te.time);
            }
        }
        return measureDynamism(times.build());
    }

    public static double measureDynamism(List<Long> arrivalTimes) {
        // announcements are distinct arrival times
        final int announcements = newHashSet(arrivalTimes).size();
        final int orders = arrivalTimes.size();
        return announcements / (double) orders;
    }

    // speed = km/h
    // points in km
    // return time in minutes
    public static long travelTime(Point p1, Point p2, double speed) {
        return DoubleMath
                .roundToLong((Point.distance(p1, p2) / speed) * 60d, RoundingMode.CEILING);
    }

}
