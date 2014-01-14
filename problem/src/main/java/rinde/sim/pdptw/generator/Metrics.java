/**
 * 
 */
package rinde.sim.pdptw.generator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.DynamicPDPTWScenario;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSortedMultiset;
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
   * @return A list of load values. The value at index <code>i</code> indicates
   *         the load at time <code>i</code>. All values are always
   *         <code> >= 0</code>. All time instances not included in the list are
   *         assumed to have load <code>0</code>.
   */
  public static ImmutableList<Double> measureLoad(Scenario s) {
    return measureLoad(s, 1);
  }

  static ImmutableList<Double> measureLoad(Scenario s, int numVehicles) {

    // FIXME should be possible to set the granularity of time. e.g. compute
    // load for every 1 second/minute/5minutes/hour/etc

    final double vehicleSpeed = getVehicleSpeed(s);
    final ImmutableList.Builder<LoadPart> loadParts = ImmutableList.builder();
    for (final TimedEvent te : s.asList()) {
      if (te instanceof AddParcelEvent) {
        loadParts.addAll(measureLoad((AddParcelEvent) te, vehicleSpeed));
      }
    }
    return sum(0, loadParts.build(), numVehicles);
  }

  public static ImmutableList<Double> measureRelativeLoad(Scenario s) {
    final int numVehicles = getEventTypeCounts(s).count(
        PDPScenarioEvent.ADD_VEHICLE);
    return measureLoad(s, numVehicles);
  }

  public static ImmutableList<LoadPart> measureLoad(AddParcelEvent event,
      double vehicleSpeed) {
    checkArgument(vehicleSpeed > 0d);
    checkArgument(
        event.parcelDTO.pickupTimeWindow.begin <= event.parcelDTO.deliveryTimeWindow.begin,
        "Delivery TW begin may not be before pickup TW begin.");
    checkArgument(
        event.parcelDTO.pickupTimeWindow.end <= event.parcelDTO.deliveryTimeWindow.end,
        "Delivery TW end may not be before pickup TW end.");

    // pickup lower bound,
    final long pickupLb = event.parcelDTO.pickupTimeWindow.begin;
    // pickup upper bound
    final long pickupUb = event.parcelDTO.pickupTimeWindow.end
        + event.parcelDTO.pickupDuration;
    final double pickupLoad = event.parcelDTO.pickupDuration
        / (double) (pickupUb - pickupLb);
    final LoadPart pickupPart = new LoadPart(pickupLb, pickupUb, pickupLoad);

    final long expectedTravelTime = travelTime(event.parcelDTO.pickupLocation,
        event.parcelDTO.destinationLocation, vehicleSpeed);
    // first possible departure time from pickup location
    final long travelLb = pickupLb + event.parcelDTO.pickupDuration;
    // latest possible arrival time at delivery location
    final long travelUb = Math.max(event.parcelDTO.deliveryTimeWindow.end,
        travelLb + expectedTravelTime);

    // checkArgument(travelUb - travelLb >= expectedTravelTime,
    // "The time windows should allow traveling from pickup to delivery.");

    final double travelLoad = expectedTravelTime
        / (double) (travelUb - travelLb);
    final LoadPart travelPart = new LoadPart(travelLb, travelUb, travelLoad);

    // delivery lower bound: the first possible time the delivery can start,
    // normally uses the start of the delivery TW, in case this is not
    // feasible we correct for the duration and travel time.
    final long deliveryLb = Math.max(event.parcelDTO.deliveryTimeWindow.begin,
        pickupLb + event.parcelDTO.pickupDuration + expectedTravelTime);
    // delivery upper bound: the latest possible time the delivery can end
    final long deliveryUb = Math.max(event.parcelDTO.deliveryTimeWindow.end,
        deliveryLb) + event.parcelDTO.deliveryDuration;
    final double deliveryLoad = event.parcelDTO.deliveryDuration
        / (double) (deliveryUb - deliveryLb);
    final LoadPart deliveryPart = new LoadPart(deliveryLb, deliveryUb,
        deliveryLoad);

    return ImmutableList.of(pickupPart, travelPart, deliveryPart);
  }

  static ImmutableList<Double> sum(long st, List<LoadPart> parts, int num) {
    checkArgument(num >= 1);
    final ImmutableList.Builder<Double> builder = ImmutableList.builder();
    long i = st;
    final Set<LoadPart> partSet = newLinkedHashSet(parts);
    while (!partSet.isEmpty()) {
      double currentLoadVal = 0d;
      final List<LoadPart> toRemove = newArrayList();
      for (final LoadPart lp : partSet) {
        if (lp.isIn(i)) {
          currentLoadVal += lp.get(i);
        }
        if (!lp.isBeforeEnd(i)) {
          toRemove.add(lp);
        }
      }
      partSet.removeAll(toRemove);

      if (!partSet.isEmpty()) {
        if (num > 1) {
          currentLoadVal /= num;
        }
        builder.add(currentLoadVal);
        i++;
      }
    }
    return builder.build();
  }

  /**
   * Checks whether the vehicles defined for the specified scenario have the
   * same speed. If the speed is the same it is returned, otherwise an exception
   * is thrown.
   * @param s The {@link Scenario} to get the speed from.
   * @return The vehicle speed if all vehicles have the same speed.
   * @throws IllegalArgumentException if either: not all vehicles have the same
   *           speed, or there are no vehicles.
   */
  public static double getVehicleSpeed(Scenario s) {
    double vehicleSpeed = -1d;
    for (final TimedEvent te : s.asList()) {
      if (te instanceof AddVehicleEvent) {
        if (vehicleSpeed == -1d) {
          vehicleSpeed = ((AddVehicleEvent) te).vehicleDTO.speed;
        } else {
          checkArgument(
              vehicleSpeed == ((AddVehicleEvent) te).vehicleDTO.speed,
              "All vehicles are expected to have the same speed.");
        }
      }
    }
    checkArgument(vehicleSpeed > 0, "There are no vehicles in the scenario.");
    return vehicleSpeed;
  }

  public static ImmutableMultiset<Enum<?>> getEventTypeCounts(Scenario s) {
    final ImmutableMultiset.Builder<Enum<?>> occurences = ImmutableMultiset
        .builder();
    for (final TimedEvent te : s.asList()) {
      occurences.add(te.getEventType());
    }
    return occurences.build();
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

    final long travelTime = travelTime(event.parcelDTO.pickupLocation,
        event.parcelDTO.destinationLocation, vehicleSpeed);

    checkArgument(
        event.parcelDTO.deliveryTimeWindow.begin >= firstDepartureTime
            + travelTime, "The begin of the delivery time window is too early.");
    checkArgument(
        latestDepartureTime + travelTime <= event.parcelDTO.deliveryTimeWindow.end,
        "The end of the pickup time window %s is too late.",
        event.parcelDTO.pickupTimeWindow.end);
  }

  public static ImmutableList<Long> getArrivalTimes(DynamicPDPTWScenario s) {
    final ImmutableList.Builder<Long> builder = ImmutableList.builder();
    for (final TimedEvent te : s.asList()) {
      if (te instanceof AddParcelEvent) {
        builder.add(te.time);
      }
    }
    return builder.build();
  }

  /**
   * Computes a histogram for the inputs. The result is a multiset with an entry
   * for each bin in ascending order, the count of each entry indicates the size
   * of the bin. A bin is indicated by its leftmost value, for example if the
   * <code>binSize</code> is <code>2</code> and the result contains
   * <code>4</code> with count <code>3</code> this means that there are
   * <code>3</code> values in range <code>2 &le; x &lt; 4</code>.
   * @param input The values to compute the histogram of.
   * @param binSize The size of the bins.
   * @return An {@link ImmutableSortedMultiset} representing the histogram.
   */
  public static ImmutableSortedMultiset<Double> computeHistogram(
      Iterable<Double> input, double binSize) {
    final ImmutableSortedMultiset.Builder<Double> builder = ImmutableSortedMultiset
        .naturalOrder();
    for (final double d : input) {
      checkArgument(!Double.isInfinite(d) && !Double.isNaN(d),
          "Only finite numbers are accepted, found %s.", d);
      builder.add(Math.floor(d / binSize) * binSize);
    }
    return builder.build();
  }

  // Best version as of January 13th, 2014
  public static double measureDynamismDistrNEW(Iterable<Double> arrivalTimes,
      double lengthOfDay) {
    final List<Double> times = newArrayList(arrivalTimes);
    checkArgument(times.size() >= 2,
        "At least two arrival times are required, found %s time(s).",
        times.size());

    for (final double time : times) {
      checkArgument(time >= 0 && time < lengthOfDay,
          "all specified times should be >= 0 and < %s. Found %s.",
          lengthOfDay, time);
    }
    Collections.sort(times);

    final int intervals = times.size();

    // this is the expected interarrival time
    final double expectedInterArrivalTime = lengthOfDay
        / intervals;
    // tolerance
    final double half = expectedInterArrivalTime / intervals;
    final double left = expectedInterArrivalTime - half;
    checkArgument(left >= 1d, "Num events %s, length of day %s, value %s.",
        times.size(), lengthOfDay, left);

    // deviation to expectedInterArrivalTime
    double sumDeviation = 0;
    double maxDeviation = 0;
    for (int i = 0; i < intervals - 1; i++) {
      // compute interarrival time
      final double delta = times.get(i + 1) - times.get(i);
      if (delta < left) {
        sumDeviation += Math.abs(left - delta);
        maxDeviation += left;
        // check for previous deviation
        if (i > 0 && times.get(i) - times.get(i - 1) < left) {
          sumDeviation += Math.abs(left - (times.get(i) - times.get(i - 1)));
          maxDeviation += left;
        }
      }
      else {
        maxDeviation += left;
      }
    }
    return 1d - (sumDeviation / maxDeviation);
  }

  // FIXME move this method to common? and make sure everybody uses the same
  // definition?
  // speed = km/h
  // points in km
  // return time in minutes
  public static long travelTime(Point p1, Point p2, double speed) {
    return DoubleMath.roundToLong((Point.distance(p1, p2) / speed) * 60d,
        RoundingMode.CEILING);
  }

  /**
   * Returns an {@link ImmutableList} containing all service points of
   * {@link Scenario}. The scenario must contain {@link AddParcelEvent}s.
   * @param s The scenario to extract the points from.
   * @return A list containing all service points in order of occurrence in the
   *         scenario event list.
   */
  public static ImmutableList<Point> getServicePoints(Scenario s) {
    final ImmutableList.Builder<Point> builder = ImmutableList.builder();
    for (final TimedEvent se : s.asList()) {
      if (se instanceof AddParcelEvent) {
        builder.add(((AddParcelEvent) se).parcelDTO.pickupLocation);
        builder.add(((AddParcelEvent) se).parcelDTO.destinationLocation);
      }
    }
    return builder.build();
  }

  // to use for parts of the timeline to avoid excessively long list with
  // mostly 0s.
  static class LoadPart extends TimeWindow {
    private final double load;

    public LoadPart(long st, long end, double value) {
      super(st, end);
      load = value;
    }

    public double get(long i) {
      if (isIn(i)) {
        return load;
      }
      return 0d;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper("LoadPart").add("begin", begin)
          .add("end", end).add("load", load).toString();
    }
  }
}
