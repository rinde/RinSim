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
package com.github.rinde.rinsim.core.model.pdp;

import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;

/**
 * Abstract base class for vehicle concept: moving {@link Container}.
 * @author Rinde van Lon
 */
public abstract class Vehicle extends ContainerImpl implements MovingRoadUser,
    TickListener {

  final VehicleDTO dto;

  /**
   * Instantiate a new vehicle based on the specified properties.
   * @param vehicleDto The data transfer object that holds all vehicle
   *          properties.
   */
  protected Vehicle(VehicleDTO vehicleDto) {
    dto = vehicleDto;
    setStartPosition(dto.getStartPosition());
    setCapacity(dto.getCapacity());
  }

  @Override
  public final PDPType getType() {
    return PDPType.VEHICLE;
  }

  @Override
  public final void tick(TimeLapse time) {
    // finish previously started pickup and delivery actions that need to
    // consume time
    getPDPModel().continuePreviousActions(this, time);
    tickImpl(time);
  }

  @Override
  public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

  /**
   * Is called every tick. This replaces the
   * {@link TickListener#tick(TimeLapse)} for vehicles.
   * @param time The time lapse that can be used.
   * @see TickListener#tick(TimeLapse)
   */
  protected abstract void tickImpl(TimeLapse time);

  @Override
  public void afterTick(TimeLapse time) {}

  /**
   * @return The data transfer object which holds the immutable properties of
   *         this vehicle.
   */
  public final VehicleDTO getDTO() {
    return dto;
  }

  @Override
  public final double getSpeed() {
    return dto.getSpeed();
  }

  /**
   * @return The time window in which this vehicle is available.
   */
  public final TimeWindow getAvailabilityTimeWindow() {
    return dto.getAvailabilityTimeWindow();
  }

  /**
   * @return The start position of the vehicle.
   */
  public final Point getStartPosition() {
    return dto.getStartPosition();
  }
}
