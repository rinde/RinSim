/**
 * 
 */
package com.github.rinde.rinsim.core.pdptw;

import static com.google.common.base.Preconditions.checkArgument;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.google.common.base.Optional;

/**
 * Default implementation of {@link Vehicle}, it initializes the vehicle based
 * on a {@link VehicleDTO} but it does not move.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class DefaultVehicle extends Vehicle {

  /**
   * The data transfer object which holds the immutable properties of this
   * vehicle.
   */
  protected final VehicleDTO dto;

  /**
   * A reference to the {@link RoadModel}, it is absent until
   * {@link #initRoadPDP(RoadModel, PDPModel)} is called.
   */
  protected Optional<RoadModel> roadModel;

  /**
   * A reference to the {@link PDPModel}, it is absent until
   * {@link #initRoadPDP(RoadModel, PDPModel)} is called.
   */
  protected Optional<PDPModel> pdpModel;

  private double speed;

  /**
   * Instantiate a new vehicle using the specified properties.
   * @param pDto {@link #dto}
   */
  public DefaultVehicle(VehicleDTO pDto) {
    setStartPosition(pDto.startPosition);
    setCapacity(pDto.capacity);
    setSpeed(pDto.speed);
    dto = pDto;
    roadModel = Optional.absent();
    pdpModel = Optional.absent();
  }

  @Override
  public final double getSpeed() {
    return speed;
  }

  /**
   * Changes the speed of the vehicle.
   * @param newSpeed The new speed.
   */
  protected final void setSpeed(double newSpeed) {
    checkArgument(newSpeed >= 0, "Speed may not be negative.");
    speed = newSpeed;
  }

  @Override
  public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
    roadModel = Optional.of(pRoadModel);
    pdpModel = Optional.of(pPdpModel);
  }

  /**
   * @return The {@link #dto}.
   */
  public VehicleDTO getDTO() {
    return dto;
  }
}
