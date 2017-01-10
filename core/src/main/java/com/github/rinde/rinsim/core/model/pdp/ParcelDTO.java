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
package com.github.rinde.rinsim.core.model.pdp;

import java.io.Serializable;

import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.auto.value.AutoValue;

/**
 * This is an immutable value object representing a parcel in a
 * pickup-and-delivery problem. It defines pickup location, destination
 * location, capacity and related time windows and service durations. Can be
 * constructed via {@link Parcel#builder(Point, Point)}.
 * @author Rinde van Lon
 */
@AutoValue
public abstract class ParcelDTO implements Serializable, IParcel {
  private static final long serialVersionUID = -6128057042614968652L;

  ParcelDTO() {}

  @Override
  public abstract Point getPickupLocation();

  @Override
  public abstract Point getDeliveryLocation();

  @Override
  public abstract TimeWindow getPickupTimeWindow();

  @Override
  public abstract TimeWindow getDeliveryTimeWindow();

  @Override
  public abstract double getNeededCapacity();

  @Override
  public abstract long getOrderAnnounceTime();

  @Override
  public abstract long getPickupDuration();

  @Override
  public abstract long getDeliveryDuration();

  @Override
  public String toString() {
    return new StringBuilder("ParcelDTO-").append(
      Integer.toHexString(hashCode())).toString();
  }
}
