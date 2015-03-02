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
package com.github.rinde.rinsim.geom;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.primitives.Doubles;

/**
 * Simple immutable implementation of {@link ConnectionData}, allowing to
 * specify the length of a connection.
 * @author Bartosz Michalik
 * @author Rinde van Lon
 */
@AutoValue
public abstract class LengthData implements ConnectionData {

  private static final LengthData EMPTY_LENGTH_DATA = create(Optional
      .<Double> absent());

  LengthData() {}

  @Override
  public abstract Optional<Double> getLength();

  /**
   * @return A {@link LengthData} instance that does not specify a length.
   */
  public static LengthData empty() {
    return EMPTY_LENGTH_DATA;
  }

  /**
   * Create a new {@link LengthData} instance using the specified length.
   * @param length The length of the connection.
   * @return A new instance.
   */
  public static LengthData create(Optional<Double> length) {
    if (length.isPresent()) {
      checkArgument(length.get() >= 0d && Doubles.isFinite(length.get()),
          "Only positive values are allowed for length, it is: %s.",
          length.get());
    }
    return new AutoValue_LengthData(length);
  }

  /**
   * Create a new {@link LengthData} instance using the specified length.
   * @param length The length of the connection.
   * @return A new instance.
   */
  public static LengthData create(double length) {
    return create(Optional.of(length));
  }
}
