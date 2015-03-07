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
 * This model supports sending messages between {@link CommUser}s. A
 * {@link CommUser} can use a {@link CommDevice} to communicate.
 * @author Rinde van Lon
 */
public class CommModel implements Model<CommUser>, TickListener {
  private final double defaultReliability;
  private final Optional<Double> defaultMaxRange;
  private final BiMap<CommUser, CommDevice> usersDevices;
  private final QualityOfService qos;

  CommModel(Builder b) {
    defaultReliability = b.defaultReliability;
    defaultMaxRange = b.defaultMaxRange;
    if (b.randomGenerator.isPresent()) {
      qos = new StochasticQoS(b.randomGenerator.get());
    } else {
      qos = QoS.PERFECT;
    }
    usersDevices = LinkedHashBiMap.create();
  }

  @Override
  public boolean register(CommUser commUser) {
    checkArgument(!contains(commUser), "%s is already registered.", commUser);
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
   * Checks if the specified {@link CommUser} is registered in this model.
   * @param commUser The user to check.
   * @return <code>true</code> if {@link CommUser} is registered,
   *         <code>false</code> otherwise.
   */
  public boolean contains(CommUser commUser) {
    return usersDevices.containsKey(commUser);
  }

  /**
   * @return The default reliability for all {@link CommDevice}s in this model.
   */
  public double getDefaultReliability() {
    return defaultReliability;
  }

  /**
   * @return The default maximum range for all {@link CommDevice}s in this
   *         model, or {@link Optional#absent()} if there is an unlimited range
   *         by default.
   */
  public Optional<Double> getDefaultMaxRange() {
    return defaultMaxRange;
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

  boolean hasRandomGenerator() {
    return qos != QoS.PERFECT;
  }

  void send(Message msg, double senderReliability) {
    // direct
    if (msg.to().isPresent()) {
      final CommDevice recipient = usersDevices.get(msg.to().get());
      doSend(msg, msg.to().get(), recipient, senderReliability);
    } else {
      // broadcast
      for (final Entry<CommUser, CommDevice> entry : usersDevices.entrySet()) {
        if (msg.from() != entry.getKey()) {
          doSend(msg, entry.getKey(), entry.getValue(), senderReliability);
        }
      }
    }
  }

  private void doSend(Message msg, CommUser to, CommDevice recipient,
      double sendReliability) {
    if (msg.predicate().apply(to)
        && qos.hasSucces(sendReliability, recipient.getReliability())) {
      recipient.receive(msg);
    }
  }

  void addDevice(CommDevice commDevice, CommUser user) {
    usersDevices.put(user, commDevice);
  }

  /**
   * @return A new builder for creating a {@link CommModel}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for creating a {@link CommModel}.
   * @author Rinde van Lon
   */
  public static class Builder extends AbstractBuilder<Builder> {
    static double DEFAULT_RELIABILITY = 1d;
    Optional<RandomGenerator> randomGenerator;
    double defaultReliability;
    Optional<Double> defaultMaxRange;

    Builder() {
      randomGenerator = Optional.absent();
      defaultReliability = DEFAULT_RELIABILITY;
      defaultMaxRange = Optional.absent();
    }

    /**
     * Sets the reliability of the device to be constructed. The reliability is
     * applied for both sending and receiving messages. Reliability must be
     * <code>0 &le; r &le; 1</code>.
     * @param reliability The reliability to set.
     * @return This, as per the builder pattern.
     */
    public Builder setDefaultDeviceReliability(double reliability) {
      return super.setReliability(reliability);
    }

    /**
     * Sets the default maximum range for all devices. This means that by
     * default devices will only be able to send messages to other devices that
     * are within this range.
     * @param maxRange The maxRange to set.
     * @return This, as per the builder pattern.
     */
    public Builder setDefaultDeviceMaxRange(double maxRange) {
      return super.setMaxRange(maxRange);
    }

    /**
     * Sets the specified {@link RandomGenerator} to use in the model. If
     * reliability is not taken into account (i.e. all devices are considered to
     * have perfect connections) this random generator is ignored and doesn't
     * need to be set. In order to have independent stochastic behavior it is
     * necessary to construct a new random generator instance just for this
     * model.
     * @param rng The random generator to use.
     * @return This, as per the builder pattern.
     */
    public Builder setRandomGenerator(RandomGenerator rng) {
      randomGenerator = Optional.of(rng);
      return this;
    }

    /**
     * Construct a new {@link CommModel} instance.
     * @return A new instance.
     */
    public CommModel build() {
      if (defaultReliability < 1d) {
        checkArgument(randomGenerator.isPresent(),
            "A random generator is required when modeling reliability < 1.");
      }
      return new CommModel(this);
    }

    @Override
    Builder self() {
      return this;
    }
  }

  interface QualityOfService {
    boolean hasSucces(double senderReliability, double receiverReliability);
  }

  static class StochasticQoS implements QualityOfService {
    private final RandomGenerator randomGenerator;

    StochasticQoS(RandomGenerator rng) {
      randomGenerator = rng;
    }

    @Override
    public boolean hasSucces(double senderReliability,
        double receiverReliability) {
      if (senderReliability == 1d && receiverReliability == 1d) {
        return true;
      }
      return randomGenerator.nextDouble() < senderReliability
          * receiverReliability;
    }
  }

  enum QoS implements QualityOfService {
    PERFECT {
      @Override
      public boolean hasSucces(double senderReliability,
          double receiverReliability) {
        return true;
      }
    }
  }
}
