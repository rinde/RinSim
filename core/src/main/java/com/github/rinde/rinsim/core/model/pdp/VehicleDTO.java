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

import static com.google.common.base.Preconditions.checkArgument;

import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.auto.value.AutoValue;

/**
 * An immutable value object representing a vehicle in a pickup-and-delivery
 * problem. Instances can be created via {@link #builder()}.
 * @author Rinde van Lon
 */
@AutoValue
public abstract class VehicleDTO {

  VehicleDTO() {}

  /**
   * @return The start position of the vehicle.
   */
  public abstract Point getStartPosition();

  /**
   * @return The maximum speed of the vehicle.
   */
  public abstract double getSpeed();

  /**
   * @return The maximum capacity of the vehicle.
   */
  public abstract int getCapacity();

  /**
   * @return The time window in which this vehicle is available.
   */
  public abstract TimeWindow getAvailabilityTimeWindow();

  /**
   * @return A new builder for constructing {@link VehicleDTO}s.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for constructing {@link VehicleDTO}s.
   * @author Rinde van Lon
   */
  public static class Builder {
    private static final double DEFAULT_SPEED = 50d;
    private static final Point DEFAULT_START_POSITION = new Point(0, 0);

    Point startPosition;
    double speed;
    int capacity;
    TimeWindow availabilityTimeWindow;

    Builder() {
      startPosition = DEFAULT_START_POSITION;
      speed = DEFAULT_SPEED;
      capacity = 1;
      availabilityTimeWindow = TimeWindow.always();
    }

    /**
     * Copy the value of the specified vehicle into this builder.
     * @param dto The dto to copy values from.
     * @return This, as per the builder pattern.
     */
    public Builder use(VehicleDTO dto) {
      return startPosition(dto.getStartPosition())
        .availabilityTimeWindow(dto.getAvailabilityTimeWindow())
        .speed(dto.getSpeed())
        .capacity(dto.getCapacity());
    }

    /**
     * Sets the start position of the vehicle. Default value: (0,0).
     * @param point The position.
     * @return This, as per the builder pattern.
     */
    public Builder startPosition(Point point) {
      startPosition = point;
      return this;
    }

    /**
     * Sets the speed of the vehicle. Default value: 50 (using the speed unit of
     * the scenario/simulator where it is used).
     * @param s The speed, must be <code> &gt; 0</code>.
     * @return This, as per the builder pattern.
     */
    public Builder speed(double s) {
      checkArgument(s > 0, "Speed must be positive, found %s.", s);
      speed = s;
      return this;
    }

    /**
     * Sets the capacity of the vehicle. Default value: 1.
     * @param c The capacity, must be <code> &gt;= 0</code>.
     * @return This, as per the builder pattern.
     */
    public Builder capacity(int c) {
      checkArgument(c >= 0, "Capacity may not be negative, found %s.", c);
      capacity = c;
      return this;
    }

    /**
     * Sets the availability {@link TimeWindow} of the vehicle. Default value:
     * {@link TimeWindow#always()}.
     * @param tw The time window.
     * @return This, as per the builder pattern.
     */
    public Builder availabilityTimeWindow(TimeWindow tw) {
      availabilityTimeWindow = tw;
      return this;
    }

    /**
     * @return A new {@link VehicleDTO} instance.
     */
    public VehicleDTO build() {
      return new AutoValue_VehicleDTO(startPosition, speed, capacity,
        availabilityTimeWindow);
    }
  }
}
