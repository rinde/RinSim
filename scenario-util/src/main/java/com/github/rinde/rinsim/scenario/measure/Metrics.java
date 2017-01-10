/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.scenario.measure;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.github.rinde.rinsim.core.model.pdp.IParcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.generator.ScenarioGenerator;
import com.github.rinde.rinsim.scenario.generator.ScenarioGenerator.TravelTimes;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

/**
 * @author Rinde van Lon
 */
public final class Metrics {

  private Metrics() {}

  /**
   * Computes the absolute load at every time instance of the specified
   * scenario. Load is a measure of expected vehicle utilization.
   * @param s The {@link Scenario} to measure.
   * @return A list of load values. The value at index <code>i</code> indicates
   *         the load at time <code>i</code>. All values are always
   *         <code> &ge; 0</code>. All time instances not included in the list
   *         are assumed to have load <code>0</code>.
   */
  static ImmutableList<Double> measureLoad(Scenario s) {
    return measureLoad(s, 1);
  }

  static ImmutableList<Double> measureLoad(Scenario s, int numVehicles) {
    final TravelTimes tt = ScenarioGenerator.createTravelTimes(s);
    final ImmutableList.Builder<LoadPart> loadParts = ImmutableList.builder();
    for (final TimedEvent te : s.getEvents()) {
      if (te instanceof AddParcelEvent) {
        loadParts.addAll(measureLoad((AddParcelEvent) te, tt));
      }
    }
    return sum(0, loadParts.build(), numVehicles);
  }

  static ImmutableList<Double> measureRelativeLoad(Scenario s) {
    final int numVehicles = getEventTypeCounts(s).count(AddVehicleEvent.class);
    return measureLoad(s, numVehicles);
  }

  static ImmutableList<LoadPart> measureLoad(AddParcelEvent event,
      TravelTimes tt) {
    checkArgument(
      event.getParcelDTO().getPickupTimeWindow().begin() <= event.getParcelDTO()
        .getDeliveryTimeWindow().begin(),
      "Delivery TW begin may not be before pickup TW begin.");
    checkArgument(
      event.getParcelDTO().getPickupTimeWindow().end() <= event.getParcelDTO()
        .getDeliveryTimeWindow().end(),
      "Delivery TW end may not be before pickup TW end.");

    // pickup lower bound,
    final long pickupLb = event.getParcelDTO().getPickupTimeWindow().begin();
    // pickup upper bound
    final long pickupUb = event.getParcelDTO().getPickupTimeWindow().end()
      + event.getParcelDTO().getPickupDuration();
    final double pickupLoad = event.getParcelDTO().getPickupDuration()
      / (double) (pickupUb - pickupLb);
    final LoadPart pickupPart = new LoadPart(pickupLb, pickupUb, pickupLoad);

    final long expectedTravelTime = tt.getShortestTravelTime(
      event.getParcelDTO().getPickupLocation(),
      event.getParcelDTO().getDeliveryLocation());
    // first possible departure time from pickup location
    final long travelLb = pickupLb + event.getParcelDTO().getPickupDuration();
    // latest possible arrival time at delivery location
    final long travelUb = Math.max(
      event.getParcelDTO().getDeliveryTimeWindow().end(),
      travelLb + expectedTravelTime);

    final double travelLoad = expectedTravelTime
      / (double) (travelUb - travelLb);
    final LoadPart travelPart = new LoadPart(travelLb, travelUb, travelLoad);

    // delivery lower bound: the first possible time the delivery can start,
    // normally uses the start of the delivery TW, in case this is not
    // feasible we correct for the duration and travel time.
    final long deliveryLb = Math.max(
      event.getParcelDTO().getDeliveryTimeWindow().begin(),
      pickupLb + event.getParcelDTO().getPickupDuration()
        + expectedTravelTime);
    // delivery upper bound: the latest possible time the delivery can end
    final long deliveryUb = Math.max(
      event.getParcelDTO().getDeliveryTimeWindow().end(),
      deliveryLb) + event.getParcelDTO().getDeliveryDuration();
    final double deliveryLoad = event.getParcelDTO().getDeliveryDuration()
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
    for (final TimedEvent te : s.getEvents()) {
      if (te instanceof AddVehicleEvent) {
        if (vehicleSpeed == -1d) {
          vehicleSpeed = ((AddVehicleEvent) te).getVehicleDTO().getSpeed();
        } else {
          checkArgument(
            vehicleSpeed == ((AddVehicleEvent) te).getVehicleDTO().getSpeed(),
            "All vehicles are expected to have the same speed.");
        }
      }
    }
    checkArgument(vehicleSpeed > 0, "There are no vehicles in the scenario.");
    return vehicleSpeed;
  }

  /**
   * Computes the number of occurrences of each event type in the specified
   * {@link Scenario}.
   * @param s The scenario to check.
   * @return A {@link ImmutableMultiset} of event types.
   */
  public static ImmutableMultiset<Class<?>> getEventTypeCounts(Scenario s) {
    final Multiset<Class<?>> set = LinkedHashMultiset.create();
    for (final TimedEvent te : s.getEvents()) {
      set.add(te.getClass());
    }
    final List<Class<?>> toMove = new ArrayList<>();
    for (final Class<?> c : set.elementSet()) {
      if (!Modifier.isPublic(c.getModifiers())
        && TimedEvent.class.isAssignableFrom(c.getSuperclass())
        && !set.contains(c.getSuperclass())) {
        toMove.add(c);
      }
    }
    for (final Class<?> c : toMove) {
      set.add(c.getSuperclass(), set.count(c));
      set.remove(c, set.count(c));
    }
    return ImmutableMultiset.copyOf(set);
  }

  /**
   * Checks the time window strictness of the specified {@link Scenario}. A time
   * window is strict if:
   * <ul>
   * <li><code>dtw.begin &ge; ptw.begin + pd + travelTime</code></li>
   * <li><code>ptw.end + pd + travelTime &le; dtw.end</code></li>
   * </ul>
   * Where:
   * <ul>
   * <li><code>dtw</code> is {@link ParcelDTO#getDeliveryTimeWindow()}</li>
   * <li><code>ptw</code> is {@link ParcelDTO#getPickupTimeWindow()}</li>
   * <li><code>pd</code> is {@link ParcelDTO#getPickupDuration()}</li>
   * <li><code>travelTime</code> is the time to travel the shortest path from
   * {@link ParcelDTO#getPickupLocation()} to
   * {@link ParcelDTO#getDeliveryLocation()}</li>
   * </ul>
   *
   * @param s The scenario to check.
   */
  public static void checkTimeWindowStrictness(Scenario s) {
    final TravelTimes tt = ScenarioGenerator.createTravelTimes(s);
    for (final TimedEvent te : s.getEvents()) {
      if (te instanceof AddParcelEvent) {
        checkParcelTWStrictness((AddParcelEvent) te, tt);
      }
    }
  }

  /*
   * Checks whether the TWs are not unnecessarily big.
   */
  static void checkParcelTWStrictness(AddParcelEvent event,
      TravelTimes travelTimes) {
    final long firstDepartureTime = event.getParcelDTO()
      .getPickupTimeWindow().begin()
      + event.getParcelDTO().getPickupDuration();
    final long latestDepartureTime = event.getParcelDTO()
      .getPickupTimeWindow().end()
      + event.getParcelDTO().getPickupDuration();

    final double travelTime = travelTimes.getShortestTravelTime(
      event.getParcelDTO().getPickupLocation(),
      event.getParcelDTO().getDeliveryLocation());

    checkArgument(
      event.getParcelDTO().getDeliveryTimeWindow().begin() >= firstDepartureTime
        + travelTime,
      "The begin of the delivery time window (%s) is too early, "
        + "should be >= %s.",
      event.getParcelDTO().getDeliveryTimeWindow(), firstDepartureTime
        + travelTime);
    checkArgument(
      latestDepartureTime + travelTime <= event.getParcelDTO()
        .getDeliveryTimeWindow().end(),
      "The end of the pickup time window %s is too late, or end of delivery "
        + "is too early.",
      event.getParcelDTO().getPickupTimeWindow().end());
  }

  /**
   * Collects all event arrival times of the specified {@link Scenario}.
   * @param s The scenario.
   * @return {@link ImmutableList} containing event arrival times.
   */
  public static ImmutableList<Long> getArrivalTimes(Scenario s) {
    final ImmutableList.Builder<Long> builder = ImmutableList.builder();
    for (final TimedEvent te : s.getEvents()) {
      if (te instanceof AddParcelEvent) {
        builder.add(te.getTime());
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
    final ImmutableSortedMultiset.Builder<Double> builder =
      ImmutableSortedMultiset
        .naturalOrder();
    for (final double d : input) {
      checkArgument(!Double.isInfinite(d) && !Double.isNaN(d),
        "Only finite numbers are accepted, found %s.", d);
      builder.add(Math.floor(d / binSize) * binSize);
    }
    return builder.build();
  }

  static long pickupUrgency(AddParcelEvent event) {
    return pickupUrgency(event.getParcelDTO());
  }

  static long pickupUrgency(IParcel dto) {
    return dto.getPickupTimeWindow().end() - dto.getOrderAnnounceTime();
  }

  enum Urgency implements Function<AddParcelEvent, Long> {
    PICKUP {
      @Override
      @Nullable
      public Long apply(@Nullable AddParcelEvent input) {
        return pickupUrgency(verifyNotNull(input));
      }
    }
  }

  static StatisticalSummary toStatisticalSummary(
      Iterable<? extends Number> values) {
    final SummaryStatistics ss = new SummaryStatistics();
    for (final Number n : values) {
      ss.addValue(n.doubleValue());
    }
    return ss.getSummary();
  }

  /**
   * Computes a {@link StatisticalSummary} object for all urgency values of a
   * {@link Scenario}.
   * @param s The scenario to measure.
   * @return A statistical summary of the urgency values in the specified
   *         scenario.
   */
  public static StatisticalSummary measureUrgency(Scenario s) {
    final List<Long> urgencyValues = FluentIterable.from(s.getEvents())
      .filter(AddParcelEvent.class)
      .transform(Urgency.PICKUP)
      .toList();
    return toStatisticalSummary(urgencyValues);
  }

  /**
   * Measures the degree of dynamism of the specified scenario.
   * @param s The scenario to measure.
   * @return A double in range [0,1].
   */
  public static double measureDynamism(Scenario s) {
    return measureDynamism(s, s.getTimeWindow().end());
  }

  /**
   * Measures the degree of dynamism of the specified scenario using the
   * specified length of day.
   * @param s The scenario to measure.
   * @param lengthOfDay The length of the day.
   * @return A double in range [0,1].
   */
  public static double measureDynamism(Scenario s, long lengthOfDay) {
    return measureDynamism(convert(getOrderArrivalTimes(s)),
      lengthOfDay);
  }

  // Best version as of March 6th, 2014
  /**
   * Computes degree of dynamism of the specified arrival times and length of
   * day.
   * @param arrivalTimes A list of event arrival times.
   * @param lengthOfDay The length of the day.
   * @return A number in range [0, 1].
   */
  public static double measureDynamism(Iterable<Double> arrivalTimes,
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

    final int numEvents = times.size();

    // this is the expected interarrival time
    final double expectedInterArrivalTime = lengthOfDay
      / numEvents;

    // deviation to expectedInterArrivalTime
    double sumDeviation = 0;
    double maxDeviation = (numEvents - 1) * expectedInterArrivalTime;
    double prevDeviation = 0;
    for (int i = 0; i < numEvents - 1; i++) {
      // compute interarrival time
      final double delta = times.get(i + 1) - times.get(i);
      if (delta < expectedInterArrivalTime) {
        final double diff = expectedInterArrivalTime - delta;
        final double scaledPrev = diff / expectedInterArrivalTime
          * prevDeviation;
        final double cur = diff + scaledPrev;
        sumDeviation += cur;
        maxDeviation += scaledPrev;
        prevDeviation = cur;
      } else {
        prevDeviation = 0;
      }
    }
    return 1d - sumDeviation / maxDeviation;
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
    for (final TimedEvent se : s.getEvents()) {
      if (se instanceof AddParcelEvent) {
        builder.add(((AddParcelEvent) se).getParcelDTO().getPickupLocation());
        builder.add(((AddParcelEvent) se).getParcelDTO().getDeliveryLocation());
      }
    }
    return builder.build();
  }

  static ImmutableList<Long> getOrderArrivalTimes(Scenario s) {
    final ImmutableList.Builder<Long> builder = ImmutableList.builder();
    for (final TimedEvent se : s.getEvents()) {
      if (se instanceof AddParcelEvent) {
        builder
          .add(((AddParcelEvent) se).getParcelDTO().getOrderAnnounceTime());
      }
    }
    return builder.build();
  }

  static ImmutableList<Double> convert(List<Long> in) {
    final ImmutableList.Builder<Double> builder = ImmutableList.builder();
    for (final Long l : in) {
      builder.add(new Double(l));
    }
    return builder.build();
  }

  // to use for parts of the timeline to avoid excessively long list with
  // mostly 0s.
  static class LoadPart {
    private final double load;
    private final TimeWindow tw;

    LoadPart(long st, long end, double value) {
      tw = TimeWindow.create(st, end);
      load = value;
    }

    boolean isBeforeEnd(long i) {
      return tw.isBeforeEnd(i);
    }

    boolean isIn(long i) {
      return tw.isIn(i);
    }

    long begin() {
      return tw.begin();
    }

    long end() {
      return tw.end();
    }

    long length() {
      return tw.length();
    }

    double get(long i) {
      if (tw.isIn(i)) {
        return load;
      }
      return 0d;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper("LoadPart").add("begin", tw.begin())
        .add("end", tw.end()).add("load", load).toString();
    }
  }
}
