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
package com.github.rinde.rinsim.core.model.comm;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;

/**
 * Abstract builder for reliability and max range properties.
 * @author Rinde van Lon
 */
abstract class AbstractBuilder<T> {
  double deviceReliability;
  Optional<Double> deviceMaxRange;

  AbstractBuilder() {
    deviceReliability = 1d;
    deviceMaxRange = Optional.absent();
  }

  abstract T self();

  /**
   * Sets the reliability of the device to be constructed. The reliability is
   * applied for both sending and receiving messages. Reliability must be
   * <code>0 &le; r &le; 1</code>.
   * @param reliability The reliability to set.
   * @return This, as per the builder pattern.
   */
  T setReliability(double reliability) {
    checkArgument(reliability >= 0d && reliability <= 1d,
        "Reliability must be 0 <= r <= 1, found %s.", reliability);
    deviceReliability = reliability;
    return self();
  }

  /**
   * Sets the maximum range. This means that the device to be created will only
   * be able to send messages to other devices that are within this range.
   * @param maxRange The maxRange to set.
   * @return This, as per the builder pattern.
   */
  T setMaxRange(double maxRange) {
    checkArgument(maxRange >= 0d);
    deviceMaxRange = Optional.of(maxRange);
    return self();
  }
}
