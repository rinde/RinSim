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
package com.github.rinde.rinsim.core.model.pdp;

/**
 * PDP specific event type that may occur in a scenario.
 * @author Rinde van Lon 
 */
public enum PDPScenarioEvent {

  /**
   * Indicates the arrival of a new vehicle.
   */
  ADD_VEHICLE,

  /**
   * Indicates the removal of a vehicle.
   */
  REMOVE_VEHICLE,

  /**
   * Indicates the arrival of a new parcel.
   */
  ADD_PARCEL,

  /**
   * Indicates the removal of a parcel.
   */
  REMOVE_PARCEL,

  /**
   * Indicates the arrival of a depot.
   */
  ADD_DEPOT,

  /**
   * Indicates the removal of a depot.
   */
  REMOVE_DEPOT,

  /**
   * Indicates the end of scenario time, e.g. this may be used to indicate the
   * end of a working day.
   */
  TIME_OUT;
}
