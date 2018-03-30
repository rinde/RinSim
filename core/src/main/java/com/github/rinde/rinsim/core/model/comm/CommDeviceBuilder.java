/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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

import static com.github.rinde.rinsim.core.model.comm.CommModel.checkRangeIsPositive;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;

/**
 * A builder for creating {@link CommDevice} instances. This builder is injected
 * in implementors of the {@link CommUser} interface, it can not be constructed
 * manually.
 *
 * @author Rinde van Lon
 */
public final class CommDeviceBuilder {
  final CommUser user;
  final CommModel model;
  double deviceReliability;
  Optional<Double> deviceMaxRange;
  private boolean used;

  CommDeviceBuilder(CommModel m, CommUser u) {
    model = m;
    user = u;
    used = false;
    deviceReliability = model.getDefaultReliability();
    deviceMaxRange = model.getDefaultMaxRange();
  }

  /**
   * Sets the reliability of the device to be constructed. The reliability is
   * applied for both sending and receiving messages. Reliability must be
   * <code>0 &le; r &le; 1</code>.
   * @param reliability The reliability to set.
   * @return This, as per the builder pattern.
   */
  public CommDeviceBuilder setReliability(double reliability) {
    CommModel.checkReliability(reliability);
    deviceReliability = reliability;
    return this;
  }

  /**
   * Sets the maximum range. This means that the device to be created will only
   * be able to send messages to other devices that are within this range.
   * @param range The max range to set.
   * @return This, as per the builder pattern.
   */
  public CommDeviceBuilder setMaxRange(double range) {
    checkRangeIsPositive(range);
    deviceMaxRange = Optional.of(range);
    return this;
  }

  /**
   * @return A new {@link CommDevice} instance.
   */
  public CommDevice build() {
    checkState(!used,
      "Only one communication device can be created per user, user: %s.",
      user);
    used = true;
    return new CommDevice(this);
  }

  boolean isUsed() {
    return used;
  }
}
