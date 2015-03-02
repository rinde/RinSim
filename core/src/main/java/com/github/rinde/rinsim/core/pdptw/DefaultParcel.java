/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.core.pdptw;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;

/**
 * Default {@link Parcel} implementation. It is instantiated using a
 * {@link ParcelDTO}.
 * @author Rinde van Lon 
 */
public class DefaultParcel extends Parcel {

  /**
   * A data object which describes the immutable properties of this parcel.
   */
  public final ParcelDTO dto;

  /**
   * Instantiate a new parcel using the data transfer object.
   * @param pDto {@link #dto}
   */
  public DefaultParcel(ParcelDTO pDto) {
    super(pDto.deliveryLocation, pDto.pickupDuration, pDto.pickupTimeWindow,
        pDto.deliveryDuration, pDto.deliveryTimeWindow, pDto.neededCapacity);
    setStartPosition(pDto.pickupLocation);
    dto = pDto;
  }

  @Override
  public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

  @Override
  public String toString() {
    return "[DefaultParcel " + dto + "]";
  }

  /**
   * @return The pickup location of this parcel.
   */
  public Point getPickupLocation() {
    return dto.pickupLocation;
  }
}
