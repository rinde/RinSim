/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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
package com.github.rinde.rinsim.scenario.gendreau06;

import java.io.Serializable;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;

/**
 * Objective function for Gendreau et al. (2006) problem instances.
 * @author Rinde van Lon
 */
public final class Gendreau06ObjectiveFunction
    implements ObjectiveFunction, Serializable {
  private static final long serialVersionUID = 6069190376442772396L;
  private static final Gendreau06ObjectiveFunction INSTANCE =
    new Gendreau06ObjectiveFunction(30d, 1d, 1d, 1d);

  private final double vehicleSpeed;
  private final double ttFactor;
  private final double tdFactor;
  private final double otFactor;

  private Gendreau06ObjectiveFunction(double speed, double tt, double td,
      double ot) {
    vehicleSpeed = speed;
    ttFactor = tt;
    tdFactor = td;
    otFactor = ot;
  }

  /**
   * All parcels need to be delivered, all vehicles need to be back at the
   * depot.
   * @param stats The statistics object to check for validity.
   * @return <code>true</code> when the statistics object represents a valid
   *         simulation run, <code>false</code> otherwise.
   */
  @Override
  public boolean isValidResult(StatisticsDTO stats) {
    return stats.totalParcels == stats.acceptedParcels
      && stats.totalParcels == stats.totalPickups
      && stats.totalParcels == stats.totalDeliveries
      && stats.simFinish
      && stats.totalVehicles == stats.vehiclesAtDepot
      && stats.distanceUnit.equals(SI.KILOMETER)
      && stats.speedUnit.equals(NonSI.KILOMETERS_PER_HOUR)
      && stats.timeUnit.equals(SI.MILLI(SI.SECOND));
  }

  /**
   * Computes the cost according to the definition of the paper: <i>the cost
   * function used throughout this work is to minimize a weighted sum of three
   * different criteria: total travel time, sum of lateness over all pick-up and
   * delivery locations and sum of overtime over all vehicles</i>. The function
   * is defined as:
   * <code>sum(Tk) + alpha sum(max(0,tv-lv)) + beta sum(max(0,tk-l0))</code>
   * Where: Tk is the total travel time on route Rk, alpha and beta are
   * weighting parameters which were set to 1 in the paper. The definition of
   * lateness: <code>max(0,lateness)</code> is commonly referred to as
   * <i>tardiness</i>. All times are expressed in minutes.
   * @param stats The statistics object to compute the cost for.
   * @return The cost.
   */
  @Override
  public double computeCost(StatisticsDTO stats) {
    final double totalTravelTime = travelTime(stats);
    final double sumTardiness = tardiness(stats);
    final double overTime = overTime(stats);
    return ttFactor * totalTravelTime
      + tdFactor * sumTardiness
      + otFactor * overTime;
  }

  @Override
  public String printHumanReadableFormat(StatisticsDTO stats) {
    return new StringBuilder().append("Travel time: ")
      .append(travelTime(stats)).append("\nTardiness: ")
      .append(tardiness(stats)).append("\nOvertime: ")
      .append(overTime(stats)).append("\nTotal: ").append(computeCost(stats))
      .toString();
  }

  /**
   * Computes the travel time based on the {@link StatisticsDTO}.
   * @param stats The statistics.
   * @return The travel time in minutes.
   */
  public double travelTime(StatisticsDTO stats) {
    return stats.timeUnit.getConverterTo(NonSI.MINUTE)
      .convert(stats.totalTravelTime);
  }

  /**
   * Computes the tardiness based on the {@link StatisticsDTO}.
   * @param stats The statistics.
   * @return The tardiness in minutes.
   */
  public double tardiness(StatisticsDTO stats) {
    return stats.timeUnit.getConverterTo(NonSI.MINUTE)
      .convert(stats.pickupTardiness + stats.deliveryTardiness);
  }

  /**
   * Computes the over time based on the {@link StatisticsDTO}.
   * @param stats The statistics.
   * @return The over time in minutes.
   */
  public double overTime(StatisticsDTO stats) {
    return stats.timeUnit.getConverterTo(NonSI.MINUTE).convert(stats.overTime);
  }

  public double getVehicleSpeed() {
    return vehicleSpeed;
  }

  @Override
  public String toString() {
    return "GendrOF(" + vehicleSpeed + ")";
  }

  /**
   * @return The default instance with a vehicle speed of 30 km/h.
   */
  public static Gendreau06ObjectiveFunction instance() {
    return INSTANCE;
  }

  public static Gendreau06ObjectiveFunction instance(double vehicleSpeedKmh) {
    return new Gendreau06ObjectiveFunction(vehicleSpeedKmh, 1d, 1d, 1d);
  }

  public static Gendreau06ObjectiveFunction instance(double vehicleSpeedKmh,
      double ttFactor, double tdFactor, double otFactor) {
    return new Gendreau06ObjectiveFunction(vehicleSpeedKmh, ttFactor, tdFactor,
      otFactor);
  }
}
