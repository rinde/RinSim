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

import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.util.LinkedHashBiMap;
import com.google.common.base.Optional;
import com.google.common.collect.BiMap;

/**
 *
 * @author Rinde van Lon
 */
public class CommModel implements Model<CommUser>, TickListener {
  private final double defaultReliability;
  private final BiMap<CommUser, CommDevice> usersDevices;
  private final Optional<RandomGenerator> randomGenerator;

  CommModel(Builder b) {
    defaultReliability = b.defaultReliability;
    randomGenerator = b.randomGenerator;
    usersDevices = LinkedHashBiMap.create();
  }

  @Override
  public boolean register(CommUser commUser) {
    final CommDeviceBuilder builder = CommDevice.builder(this, commUser)
        .setReliability(defaultReliability);
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

  /**
   * @param commUser
   */
  public boolean contains(CommUser commUser) {
    return usersDevices.containsKey(commUser);
  }

  /**
   * @return
   */
  public double getDefaultReliability() {
    return defaultReliability;
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

  Optional<RandomGenerator> getRandomGenerator() {
    return randomGenerator;
  }

  void send(Message msg, QualityOfService qos) {
    // direct
    if (msg.to().isPresent()) {
      if (qos.hasSucces()) {
        usersDevices.get(msg.to().get()).receive(msg);
      }
    } else {
      // broadcast
      for (final Entry<CommUser, CommDevice> entry : usersDevices.entrySet()) {
        if (msg.predicate().apply(entry.getKey())
            && msg.from() != entry.getKey()) {
          if (qos.hasSucces()) {
            entry.getValue().receive(msg);
          }
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
    static double DEFAULT_RELIABILITY = 1d;
    Optional<RandomGenerator> randomGenerator;
    double defaultReliability;

    Builder() {
      randomGenerator = Optional.absent();
      defaultReliability = DEFAULT_RELIABILITY;
    }

    /**
     * @param reliability the defaultReliability to set
     */
    public Builder setDefaultDeviceReliability(double reliability) {
      defaultReliability = reliability;
      return this;
    }

    /**
     * Should insert a new instance for independence
     * @param rng
     * @return
     */
    public Builder setRandomGenerator(RandomGenerator rng) {
      randomGenerator = Optional.of(rng);
      return this;
    }

    public CommModel build() {
      if (defaultReliability < 1d) {
        checkArgument(randomGenerator != null);
      }
      return new CommModel(this);
    }
  }

  interface QualityOfService {

    boolean hasSucces();

  }

  static class StochasticQoS implements QualityOfService {
    private final double reliability;
    private final RandomGenerator randomGenerator;

    StochasticQoS(double r, RandomGenerator rng) {
      reliability = r;
      randomGenerator = rng;
    }

    @Override
    public boolean hasSucces() {
      return randomGenerator.nextDouble() < reliability;
    }
  }

  enum QoS implements QualityOfService {
    RELIABLE {
      @Override
      public boolean hasSucces() {
        return true;
      }
    }
  }

}
