/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import com.github.rinde.rinsim.core.model.road.RoadUser;

/**
 * Base interface for objects in {@link PDPModel}. Can be used directly but
 * usually one of its subclasses are used instead:
 * <ul>
 * <li>{@link Vehicle}</li>
 * <li>{@link Parcel}</li>
 * <li>{@link Depot}</li>
 * </ul>
 * 
 * @author Rinde van Lon
 */
public interface PDPObject extends RoadUser {

  /**
   * @return The type of the PDPObject.
   */
  PDPType getType();

  /**
   * Is called when object is registered in {@link PDPModel}.
   * @param model A reference to the model.
   */
  void initPDPObject(PDPModel model);

}
