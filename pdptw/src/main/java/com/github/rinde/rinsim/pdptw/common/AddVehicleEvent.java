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
package com.github.rinde.rinsim.pdptw.common;

import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.google.auto.value.AutoValue;

/**
 * Event indicating the creation of a vehicle.
 * @author Rinde van Lon
 */
@AutoValue
public abstract class AddVehicleEvent implements TimedEvent {

  AddVehicleEvent() {}

  /**
   * @return Data which describes the vehicle that should be added.
   */
  public abstract VehicleDTO getVehicleDTO();

  /**
   * Creates a new {@link AddVehicleEvent}.
   * @param time The time at which the vehicle is added.
   * @param dto The {@link VehicleDTO} that describes the vehicle.
   * @return A new {@link AddVehicleEvent} instance.
   */
  public static AddVehicleEvent create(long time, VehicleDTO dto) {
    return new AutoValue_AddVehicleEvent(time, dto);
  }
}
