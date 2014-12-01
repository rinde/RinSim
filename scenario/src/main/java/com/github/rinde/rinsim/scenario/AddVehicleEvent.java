/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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
package com.github.rinde.rinsim.scenario;

import com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent;
import com.github.rinde.rinsim.core.pdptw.VehicleDTO;

/**
 * Event indicating that a vehicle can be created.
 * @author Rinde van Lon 
 */
public class AddVehicleEvent extends TimedEvent {

  /**
   * Data which describes the vehicle that should be added.
   */
  public final VehicleDTO vehicleDTO;

  /**
   * New instance.
   * @param t {@link TimedEvent#time}
   * @param dto {@link #vehicleDTO}
   */
  public AddVehicleEvent(long t, VehicleDTO dto) {
    super(PDPScenarioEvent.ADD_VEHICLE, t);
    vehicleDTO = dto;
  }
}
