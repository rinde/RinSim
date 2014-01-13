/**
 * 
 */
package rinde.sim.pdptw.generator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.stat.inference.TestUtils;

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
import com.google.common.primitives.Longs;

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

  public static double measureDynamism(DynamicPDPTWScenario s, long granularity) {
    checkArgument(s.getTimeWindow().begin == 0);
    return measureDynamism(getArrivalTimes(s), s.getTimeWindow().end,
        granularity);
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

  public static double measureDynamismDistr3(Iterable<Long> arrivalTimes,
      long lengthOfDay) {

    final List<Long> ts = newArrayList(arrivalTimes);
    Collections.sort(ts);

    final List<Long> input = ImmutableList.<Long> builder()
        // .add(0L)
        .addAll(ts)
        // .add(lengthOfDay)
        .build();

    final int intervals = input.size();
    // interarrival time for the 100% dynamism case
    final double period = lengthOfDay / (double) intervals;

    final List<Double> squaredDiffs = newArrayList();
    for (int i = 0; i < intervals - 1; i++) {
      final double interArrivalTime = (double) input.get(i + 1) - input.get(i);
      // if (interArrivalTime - period < 0) {
      squaredDiffs.add(Math.pow(interArrivalTime - period, 2d));
      // }
    }
    // System.out.println("period: " + period);
    // System.out.println(squaredDiffs);
    final double var = squaredDiffs.isEmpty() ? 0 : DoubleMath
        .mean(squaredDiffs);

    final double range = input.get(input.size() - 1) - input.get(0);

    // var for 100% dynamism is 0
    // var for 0% dynamism is:
    final double staticVar = Math.max(period * period, range * range);
    // System.out.printf("var: %1.3f static var: %1.3f\n", var, staticVar);

    // normalise
    return 1d - (var / staticVar);
  }

  public static double measureDynamismDistr2(Iterable<Long> arrivalTimes,
      long lengthOfDay) {

    final List<Long> ts = newArrayList(arrivalTimes);
    Collections.sort(ts);

    final List<Long> input = ImmutableList.<Long> builder()
        // .add(0L)
        .addAll(ts)
        // .add(lengthOfDay)
        .build();

    final int intervals = input.size();
    final double period = lengthOfDay / (double) intervals;

    double sumDeviation = 0;
    double maxDeviation = 0;
    for (int i = 0; i < intervals; i++) {
      final double left = i * period;
      final double right = Math.abs((left + period) - lengthOfDay) < .0000001 ? lengthOfDay
          : left + period;
      final double cur = input.get(i);

      checkArgument(left >= 0 && right <= lengthOfDay, "[%s,%s)", left, right,
          lengthOfDay);
      maxDeviation += left;

      if (cur < left) {
        sumDeviation += Math.abs(left - cur);
      } else if (cur > right) {
        sumDeviation += Math.abs(right - cur);
      }
      // System.out.println(sumDeviation / i);
    }

    // maxDeviation /= input.size() - 1;
    // System.out.printf("sum %1.3f max %1.3f \n", sumDeviation, maxDeviation);
    return 1d - (sumDeviation / maxDeviation);
  }

  public static List<Long> cumulativeInformationGraph(List<Long> times,
      long lengthOfDay) {
    final List<Long> list = newArrayList();
    int index = 0;
    for (int i = 0; i < lengthOfDay; i++) {
      while (index < times.size() && times.get(index) == i) {
        index++;
      }
      list.add((long) index);
    }
    return list;
  }

  public static double measureDynamism2ndDerivative(
      Iterable<Long> arrivalTimes, long lengthOfDay) {
    System.out.println("=======================");
    System.out.println(lengthOfDay + " " + arrivalTimes);
    final List<Long> ts = newArrayList(arrivalTimes);
    checkArgument(!ts.isEmpty());
    Collections.sort(ts);

    final double expectedIncrease = ts.size() / (double) lengthOfDay;
    // System.out.println("expected increase: " + expectedIncrease);

    final List<Long> cumulInfo = cumulativeInformationGraph(ts, lengthOfDay);
    // System.out.println(cumulInfo);
    // final List<Long> times = ImmutableList.<Long> builder()
    // .add(0L)
    // .addAll(ts)
    // .add(lengthOfDay)
    // .build();

    // TODO add a tolerance?

    double error = 0;
    final double minError = 0;
    int numVals = 0;
    for (int i = 0; i < cumulInfo.size() - 1; i++) {
      final double expectedEvents = (i + 1) * expectedIncrease;
      final double observedEvents = cumulInfo.get(i);

      // final double curError = observedEvents - expectedEvents;
      // final double curMaxError = curError > 0 ? ts.size() - expectedEvents
      // : expectedEvents;
      // final double curMinError = Math.abs(expectedEvents
      // - (curError > 0 ? Math.ceil(expectedEvents) : Math
      // .floor(expectedEvents)));

      final double max;
      final double min;
      if (observedEvents > expectedEvents) {
        min = Math.ceil(expectedEvents) - expectedEvents;
        max = ts.size() - expectedEvents;
      }
      else if (observedEvents < expectedEvents) {
        min = expectedEvents - Math.floor(expectedEvents);
        max = expectedEvents;
      }
      else {
        min = 0d;
        max = 0d;
      }

      final double cur;
      boolean ignore = false;
      if ((max - min) > 0d) {
        cur = (Math.abs(expectedEvents - observedEvents) - min)
            / (max - min);
        if (cur == 0d) {
          ignore = true;
        }
      } else {
        ignore = true;
        cur = 0d;
      }

      // System.out
      // .printf("min error %1.3f max %1.3f\n", curMinError, curMaxError);
      // final double relError = (Math.abs(curError) - minError)
      // / (curMaxError - curMinError);

      // System.out
      // .printf(
      // "%d expected: %1.2f observed: %1.2f error: %1.3f min: %1.3f max: %1.3f\n",
      // i, expectedEvents, observedEvents, cur, min, max);
      if (!ignore) {
        numVals++;
        error += cur;// Math.abs(relError);
      }
      // System.out.printf("error %1.2f\n", error);

      // minError += Math.abs(expectedEvents - Math.round(expectedEvents));

    }

    if (numVals > 0) {
      error /= numVals;
    }

    // final double maxError = (((double) ts.size() * (lengthOfDay - 1)) / 2d);
    // System.out
    // .printf("error: %1.3f min: %1.3f max: %1.3f\n", error, minError,
    // maxError);
    return 1d - error;// ((error - minError) / (maxError - minError));

    // final List<Long> firstDerivative = derivative(cumulInfo);
    // final List<Long> secondDerivative = derivative(firstDerivative);
    //
    // System.out.println("  " + firstDerivative);
    // System.out.println("   " + secondDerivative);
    //
    // long sumDeviation = 0;
    // for (final Long l : secondDerivative) {
    // sumDeviation += Math.abs(l);
    // }
    // System.out.println("deviation: " + sumDeviation);
    // return 1d - (sumDeviation / (double) lengthOfDay);
  }

  public static double measureDynamism2ndDerivative2(
      Iterable<Long> arrivalTimes, long lengthOfDay) {
    // System.out.println("=======================");
    // System.out.println(lengthOfDay + " " + arrivalTimes);
    final List<Long> ts = newArrayList(arrivalTimes);
    checkArgument(!ts.isEmpty());
    Collections.sort(ts);

    final double expectedIncrease = ts.size() / (double) lengthOfDay;
    final List<Long> cumulInfo = cumulativeInformationGraph(ts, lengthOfDay);

    double error = 0;
    int numVals = 0;
    for (final long i : ts) {
      checkArgument(i < lengthOfDay);
      final double expectedEvents = (i + 1) * expectedIncrease;
      final double observedEvents = cumulInfo.get((int) i);

      final double max;
      final double min;
      if (observedEvents > expectedEvents) {
        min = Math.ceil(expectedEvents) - expectedEvents;
        max = ts.size() - expectedEvents;
      }
      else if (observedEvents < expectedEvents) {
        min = expectedEvents - Math.floor(expectedEvents);
        max = expectedEvents;
      }
      else {
        min = 0d;
        max = 0d;
      }

      final double cur;
      boolean ignore = false;
      if ((max - min) > 0d) {
        cur = Math.sqrt(Math.abs(expectedEvents - observedEvents) - min)
            / Math.sqrt((max - min));
        if (cur == 0d) {
          ignore = true;
        }
      } else {
        ignore = true;
        cur = 0d;
      }

      if (!ignore) {
        numVals++;
        error += cur;// Math.abs(relError);
      }
    }

    if (numVals > 0) {
      error /= numVals;
    }
    return 1d - error;// ((error - minError) / (maxError - minError));
  }

  static ImmutableList<Long> derivative(List<Long> list) {
    final ImmutableList.Builder<Long> builder = ImmutableList.builder();
    for (int i = 0; i < list.size() - 1; i++) {
      builder.add(list.get(i + 1) - list.get(i));
    }
    return builder.build();
  }

  public static double chi(Iterable<Long> arrivalTimes, long lengthOfDay) {
    final List<Long> ts = newArrayList(arrivalTimes);
    checkArgument(!ts.isEmpty());
    Collections.sort(ts);
    final List<Long> cumulInfo = cumulativeInformationGraph(ts, lengthOfDay);
    final long[] observed = Longs.toArray(cumulInfo);

    final double[] expected = new double[cumulInfo.size()];
    final double expectedIncrease = ts.size() / (double) lengthOfDay;
    for (int i = 0; i < cumulInfo.size(); i++) {
      expected[i] = (i + 1d) * expectedIncrease;
      checkState(expected[i] > 0d);
    }

    return TestUtils.chiSquareTest(expected, observed);
  }

  public static double measureDynDeviationCount(Iterable<Long> arrivalTimes,
      long lengthOfDay) {

    final List<Long> ts = newArrayList(arrivalTimes);
    checkArgument(!ts.isEmpty());
    Collections.sort(ts);

    final double expectedIncrease = ts.size() / (double) lengthOfDay;

    final List<Long> cumulInfo = cumulativeInformationGraph(ts, lengthOfDay);

    int relevantMoments = 0;
    int nonDeviations = 0;
    for (int i = 0; i < cumulInfo.size() - 1; i++) {
      final double expectedEvents = (i + 1) * expectedIncrease;
      final double observedEvents = cumulInfo.get(i);

      final double max;
      final double min;
      if (observedEvents > expectedEvents) {
        min = Math.ceil(expectedEvents) - expectedEvents;
        max = ts.size() - expectedEvents;
      }
      else if (observedEvents < expectedEvents) {
        min = expectedEvents - Math.floor(expectedEvents);
        max = expectedEvents;
      }
      else {
        min = 0d;
        max = 0d;
      }

      final double cur;
      boolean ignore = false;
      if ((max - min) > 0d) {
        cur = (Math.abs(expectedEvents - observedEvents) - min)
            / (max - min);
        if (cur == 0d) {
          // ignore = true;
          nonDeviations++;
        }
      } else {
        ignore = true;
        cur = 0d;
      }

      if (!ignore) {
        relevantMoments++;
      }
    }

    final double score = (relevantMoments - nonDeviations)
        / (double) relevantMoments;

    return 1d - (score);

  }

  // This is the FINAL version as established on December 20th, 2013.
  public static double measureDynamismDistr(Iterable<Long> arrivalTimes,
      long lengthOfDay) {
    final List<Long> ts = newArrayList(arrivalTimes);
    checkArgument(!ts.isEmpty());
    Collections.sort(ts);
    // TODO sort?
    final List<Long> times = ImmutableList.<Long> builder()
        // .add(0L)
        .addAll(ts)
        // .add(lengthOfDay)
        .build();

    // final double length = times.get(times.size() - 1) - times.get(0);

    final int intervals = times.size();
    final double range = times.get(times.size() - 1) - times.get(0);

    // this is the expected interarrival time
    final double expectedInterArrivalTime = (double) lengthOfDay
        / (double) intervals;
    // tolerance
    final double half = expectedInterArrivalTime / intervals;
    final double left = expectedInterArrivalTime - half;
    final double right = expectedInterArrivalTime + half;

    // deviation to expectedInterArrivalTime
    double sumDeviation = 0;
    for (int i = 0; i < intervals - 1; i++) {
      // compute interarrival time
      final long delta = times.get(i + 1) - times.get(i);
      if (delta < left) {
        sumDeviation += Math.abs(left - delta);
        // } else if (delta > right) {
        // sumDeviation += Math.abs(right - delta);
      }
    }
    final double maxDeviation =
        // Math.max(0, range
        // - (expectedInterArrivalTime + half)) +
        (intervals - 1) * left;

    // System.out.printf(
    // "expectedInterArrivalTime: %1.3f intervals: %1d left: %1.3f\n",
    // expectedInterArrivalTime, (intervals - 1), left);
    // System.out.printf("sum %1.3f max %1.3f range %1.3f\n", sumDeviation,
    // maxDeviation, range);
    return 1d - (sumDeviation / maxDeviation);
  }

  public static double measureDynamismDistrNEW(Iterable<Long> arrivalTimes,
      long lengthOfDay) {
    final List<Long> ts = newArrayList(arrivalTimes);
    checkArgument(!ts.isEmpty());
    Collections.sort(ts);
    // TODO sort?
    final List<Long> times = ImmutableList.<Long> builder()
        // .add(0L)
        .addAll(ts)
        // .add(lengthOfDay)
        .build();

    // final double length = times.get(times.size() - 1) - times.get(0);

    final int intervals = times.size();

    // this is the expected interarrival time
    final double expectedInterArrivalTime = (double) lengthOfDay
        / (double) intervals;
    // tolerance
    final double half = expectedInterArrivalTime / intervals;
    final double left = expectedInterArrivalTime - half;

    // deviation to expectedInterArrivalTime
    double sumDeviation = 0;
    double maxDeviation = 0;
    for (int i = 0; i < intervals - 1; i++) {

      final double curExp = ((double) lengthOfDay - times.get(i))
          / (intervals - i);
      final double curHalf = curExp / (intervals - i);
      final double curLeft = curExp - curHalf;
      final double maxx = Math.max(left, curLeft);

      // compute interarrival time
      final long delta = times.get(i + 1) - times.get(i);
      // System.out.println(delta + " " + left + " " + curLeft);
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
    // System.out.println("max " + maxDeviation);
    // final double maxDeviation =
    // // Math.max(0, range
    // // - (expectedInterArrivalTime + half)) +
    // (intervals - 1) * left;

    // System.out.printf(
    // "expectedInterArrivalTime: %1.3f intervals: %1d left: %1.3f\n",
    // expectedInterArrivalTime, (intervals - 1), left);
    // System.out.printf("sum %1.3f max %1.3f range %1.3f\n", sumDeviation,
    // maxDeviation, range);
    return 1d - (sumDeviation / maxDeviation);
  }

  // [0,lengthOfDay)
  /**
   * Measure degree of dynamism using the specified times. Degree of dynamism is
   * defined as the ratio of the number of intervals with changes to the total
   * number intervals.
   * @param arrivalTimes The arrival times for which to complete dynamism.
   * @param lengthOfDay Length of the scenario.
   * @param granularity Length of an interval.
   * @return The dynamism.
   */
  public static double measureDynamism(Iterable<Long> arrivalTimes,
      long lengthOfDay, long granularity) {
    checkArgument(lengthOfDay > 0, "Length of day must be positive.");
    checkArgument(granularity <= lengthOfDay,
        "Granularity must be <= lengthOfDay.");
    checkArgument(
        lengthOfDay % granularity == 0,
        "lengthOfDay (%s) % granularity (%s) must equal 0.",
        lengthOfDay, granularity);

    final Set<Long> intervals = newHashSet();
    int size = 0;
    for (final long time : arrivalTimes) {
      checkArgument(time >= 0 && time < lengthOfDay,
          "all specified times should be >= 0 and < %s. Found %s.",
          lengthOfDay, time);
      intervals.add(DoubleMath.roundToLong(time / (double) granularity,
          RoundingMode.FLOOR));
      size++;
    }
    final int totalIntervals = DoubleMath.roundToInt(lengthOfDay
        / (double) granularity, RoundingMode.UNNECESSARY);
    return intervals.size()
        / (double) Math.min(totalIntervals, size);
  }

  @Deprecated
  public static double measureDynamismOld(Scenario s) {
    final List<TimedEvent> list = s.asList();
    final ImmutableList.Builder<Long> times = ImmutableList.builder();
    for (final TimedEvent te : list) {
      if (te instanceof AddParcelEvent) {
        times.add(te.time);
      }
    }
    return measureDynamismOld(times.build());
  }

  @Deprecated
  public static double measureDynamismOld(List<Long> arrivalTimes) {
    // announcements are distinct arrival times
    // this indicates the number of changes of the problem
    final int announcements = newHashSet(arrivalTimes).size();
    final int orders = arrivalTimes.size();
    return announcements / (double) orders;
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
