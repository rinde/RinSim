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
import static com.google.common.base.Preconditions.checkState;

import com.github.rinde.rinsim.core.model.comm.CommModel.QoS;
import com.github.rinde.rinsim.core.model.comm.CommModel.QualityOfService;
import com.github.rinde.rinsim.core.model.comm.CommModel.StochasticQoS;

/**
 * A builder for creating {@link CommDevice} instances. This builder is injected
 * in implementors of the {@link CommUser} interface.
 *
 * @author Rinde van Lon
 */
public final class CommDeviceBuilder {
  double deviceReliability;
  double deviceMaxRange;
  int deviceMemorySize;
  final CommUser user;
  final CommModel model;
  private boolean used;

  CommDeviceBuilder(CommModel m, CommUser u) {
    model = m;
    user = u;
    used = false;
    deviceReliability = model.getDefaultReliability();
  }

  /**
   * @param reliability the reliability to set
   * @return This, as per the builder pattern.
   */
  public CommDeviceBuilder setReliability(double reliability) {
    checkArgument(reliability >= 0d && reliability <= 1d);
    deviceReliability = reliability;
    return this;
  }

  /**
   * @param maxRange the maxRange to set
   * @return
   */
  public CommDeviceBuilder setMaxRange(double maxRange) {
    checkArgument(maxRange >= 0d);
    deviceMaxRange = maxRange;
    return this;
  }

  /**
   * @param numberOfMessagesToKeep the deviceMemorySize to set
   * @return
   */
  public CommDeviceBuilder setDeviceMemorySize(int numberOfMessagesToKeep) {
    checkArgument(numberOfMessagesToKeep > 0);
    deviceMemorySize = numberOfMessagesToKeep;
    return this;
  }

  public CommDevice build() {
    checkState(!used,
        "Only one communication device can be created per user, user: %s.",
        user);
    used = true;

    QualityOfService rc;
    if (deviceReliability == 1d) {
      rc = QoS.RELIABLE;
    } else {
      checkArgument(
          model.getRandomGenerator().isPresent(),
          "An unreliable comm device can only be created when the CommModel has"
              + " a RandomGenerator.");
      rc = new StochasticQoS(deviceReliability, model.getRandomGenerator()
          .get());
    }
    return new CommDevice(this, rc);
  }

  boolean isUsed() {
    return used;
  }
}
