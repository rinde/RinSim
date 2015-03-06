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

import java.util.Map.Entry;
import java.util.Set;

import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.util.LinkedHashBiMap;
import com.google.common.collect.BiMap;

/**
 * @author Rinde van Lon
 *
 */
public class CommModel implements Model<CommUser>, TickListener {
  private final Unit<Length> distanceUnit;
  private final double globalReliability;
  private final BiMap<CommUser, CommDevice> usersDevices;

  CommModel(Builder b) {
    globalReliability = b.globalReliability;
    distanceUnit = b.distanceUnit;
    usersDevices = LinkedHashBiMap.create();
  }

  @Override
  public boolean register(CommUser commUser) {
    final CommDeviceBuilder builder = CommDevice.builder(this, commUser)
        .setReliability(globalReliability);
    commUser.setCommDevice(builder);
    checkState(
        builder.isUsed(),
        "%s is not implemented correctly, a CommDevice must be constructed in "
            + "setCommDevice()",
        commUser);
    return false;
  }

  @Override
  public boolean unregister(CommUser element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<CommUser> getSupportedType() {
    return CommUser.class;
  }

  @Override
  public void tick(TimeLapse timeLapse) {}

  @Override
  public void afterTick(TimeLapse timeLapse) {
    final Set<CommDevice> devices = usersDevices.values();
    for (final CommDevice device : devices) {
      device.sendMessages();
    }
  }

  void send(Message msg) {
    // TODO reliability
    // direct
    if (msg.to().isPresent()) {
      usersDevices.get(msg.to().get()).receive(msg);
    } else {
      // broadcast
      for (final Entry<CommUser, CommDevice> entry : usersDevices.entrySet()) {
        if (msg.predicate().apply(entry.getKey())
            && msg.from() != entry.getKey()) {
          entry.getValue().receive(msg);
        }
      }
    }
  }

  void addDevice(CommDevice commDevice, CommUser user) {
    usersDevices.put(user, commDevice);
  }

  /**
   * @return
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private double globalReliability;
    private Unit<Length> distanceUnit;

    /**
     * @param globalReliability the globalReliability to set
     */
    public Builder setGlobalReliability(double globalReliability) {
      this.globalReliability = globalReliability;
      return this;
    }

    /**
     * @param distanceUnit the distanceUnit to set
     */
    public Builder setDistanceUnit(Unit<Length> distanceUnit) {
      this.distanceUnit = distanceUnit;
      return this;
    }

    public CommModel build() {
      return new CommModel(this);
    }
  }

}
