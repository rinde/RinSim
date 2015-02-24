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
package com.github.rinde.rinsim.geom;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashMap;

import org.inferred.freebuilder.FreeBuilder;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Doubles;

/**
 * {@link ConnectionData} implementation which allows to associate multiple
 * attributes to a connection (through a {@link HashMap}). There are two
 * "default" supported properties defined: connection length, and maximum speed
 * on a connection.
 * @author Bartosz Michalik
 * @author Rinde van Lon
 * @since 2.0
 */
@FreeBuilder
public abstract class MultiAttributeData implements ConnectionData {

  MultiAttributeData() {}

  @Override
  public abstract Optional<Double> getLength();

  /**
   * Returns max speed defined for a connection. If the max speed is not
   * specified {@link Optional#absent()} is returned.
   * @return The max speed.
   */
  public abstract Optional<Double> getMaxSpeed();

  /**
   * @return All attributes that are defined in this object.
   */
  public abstract ImmutableMap<String, Object> getAttributes();

  /**
   * @return A new {@link Builder} instance for creating
   *         {@link MultiAttributeData} instances.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for creating {@link MultiAttributeData} instances.
   * @author Rinde van Lon
   */
  public static class Builder extends MultiAttributeData_Builder {
    Builder() {}

    @Override
    public Builder setLength(double length) {
      checkArgument(length >= 0d && Doubles.isFinite(length),
          "Expected positive value for length but found %s.", length);
      return super.setLength(length);
    }

    @Override
    public Builder setMaxSpeed(double speed) {
      checkArgument(speed > 0d && Doubles.isFinite(speed),
          "Expected positive value for maxSpeed but found %s.", speed);
      return super.setMaxSpeed(speed);
    }
  }
}
