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

import static com.google.common.base.Preconditions.checkState;

/**
 * A builder for creating {@link CommDevice} instances. This builder is injected
 * in implementors of the {@link CommUser} interface.
 *
 * @author Rinde van Lon
 */
public final class CommDeviceBuilder extends AbstractBuilder<CommDeviceBuilder> {
  final CommUser user;
  final CommModel model;
  private boolean used;

  CommDeviceBuilder(CommModel m, CommUser u) {
    model = m;
    user = u;
    used = false;
    deviceReliability = model.getDefaultReliability();
    deviceMaxRange = model.getDefaultMaxRange();
  }

  @Override
  public CommDeviceBuilder setMaxRange(double range) {
    return super.setMaxRange(range);
  }

  @Override
  public CommDeviceBuilder setReliability(double reliability) {
    return super.setReliability(reliability);
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

  @Override
  CommDeviceBuilder self() {
    return this;
  }
}
