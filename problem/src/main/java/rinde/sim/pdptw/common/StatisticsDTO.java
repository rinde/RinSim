package rinde.sim.pdptw.common;

import java.io.Serializable;

import javax.annotation.Nullable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.base.Objects;

/**
 * This is an immutable value object containing statistics about a simulation
 * run.
 * <p>
 * Two statistics objects are equal when all fields, EXCEPT computation time,
 * are equal.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class StatisticsDTO implements Serializable {
  private static final long serialVersionUID = 1968951252238291733L;
  /**
   * The cumulative distance all vehicle have traveled.
   */
  public final double totalDistance;
  /**
   * The total number of parcels that are picked up.
   */
  public final int totalPickups;
  /**
   * The total number of parcels that are delivered.
   */
  public final int totalDeliveries;
  /**
   * The total number of parcels in the scenario.
   */
  public final int totalParcels;
  /**
   * The total number of parcels that were actually added in the model.
   */
  public final int acceptedParcels;
  /**
   * The cumulative pickup tardiness of all parcels.
   */
  public final long pickupTardiness;
  /**
   * The cumulative delivery tardiness of all parcels.
   */
  public final long deliveryTardiness;
  /**
   * The time (ms) it took to compute the simulation.
   */
  public final long computationTime;
  /**
   * The time that has elapsed in the simulation (this is in the unit which is
   * used in the simulation).
   */
  public final long simulationTime;
  /**
   * Indicates whether the scenario has finished.
   */
  public final boolean simFinish;
  /**
   * The number of vehicles that are back at the depot.
   */
  public final int vehiclesAtDepot;
  /**
   * The cumulative tardiness of vehicle arrival at depot.
   */
  public final long overTime;
  /**
   * Total number of vehicles available.
   */
  public final int totalVehicles;
  /**
   * Number of vehicles that have been used, 'used' means has moved.
   */
  public final int movedVehicles;

  /**
   * Create a new statistics object.
   * @param dist {@link #totalDistance}.
   * @param pick {@link #totalPickups}.
   * @param del {@link #totalDeliveries}.
   * @param parc {@link #totalParcels}.
   * @param accP {@link #acceptedParcels}.
   * @param pickTar {@link #pickupTardiness}.
   * @param delTar {@link #deliveryTardiness}.
   * @param compT {@link #computationTime}.
   * @param simT {@link #simulationTime}.
   * @param finish {@link #simFinish}.
   * @param atDepot {@link #vehiclesAtDepot}.
   * @param overT {@link #overTime}.
   * @param total {@link #totalVehicles}.
   * @param moved {@link #movedVehicles}.
   */
  public StatisticsDTO(double dist, int pick, int del, int parc, int accP,
      long pickTar, long delTar, long compT, long simT, boolean finish,
      int atDepot, long overT, int total, int moved) {
    totalDistance = dist;
    totalPickups = pick;
    totalDeliveries = del;
    totalParcels = parc;
    acceptedParcels = accP;
    pickupTardiness = pickTar;
    deliveryTardiness = delTar;
    computationTime = compT;
    simulationTime = simT;
    simFinish = finish;
    vehiclesAtDepot = atDepot;
    overTime = overT;
    totalVehicles = total;
    movedVehicles = moved;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this,
        ToStringStyle.MULTI_LINE_STYLE);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    final StatisticsDTO other = (StatisticsDTO) obj;
    return new EqualsBuilder().append(totalDistance, other.totalDistance)
        .append(totalPickups, other.totalPickups)
        .append(totalDeliveries, other.totalDeliveries)
        .append(totalParcels, other.totalParcels)
        .append(acceptedParcels, other.acceptedParcels)
        .append(pickupTardiness, other.pickupTardiness)
        .append(deliveryTardiness, other.deliveryTardiness)
        .append(simulationTime, other.simulationTime)
        .append(simFinish, other.simFinish)
        .append(vehiclesAtDepot, other.vehiclesAtDepot)
        .append(overTime, other.overTime)
        .append(totalVehicles, other.totalVehicles)
        .append(movedVehicles, other.movedVehicles).isEquals();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(totalDistance, totalPickups, totalParcels,
        acceptedParcels, pickupTardiness, deliveryTardiness, simulationTime,
        simFinish, vehiclesAtDepot, overTime, totalVehicles, movedVehicles);
  }
}
