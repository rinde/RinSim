/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.util.LinkedHashBiMap;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;

/**
 * This model supports sending messages between {@link CommUser}s. A
 * {@link CommUser} can use a {@link CommDevice} to communicate. Instances can
 * be obtained via {@link #builder()}.
 * <p>
 * <b>Model properties</b>
 * <ul>
 * <li><i>Associated type:</i> {@link CommUser}.</li>
 * <li><i>Provides:</i> none.</li>
 * <li><i>Dependency:</i> {@link RandomProvider}.</li>
 * </ul>
 * See {@link ModelBuilder} for more information about model properties.
 * @author Rinde van Lon
 */
public final class CommModel extends AbstractModel<CommUser>
    implements TickListener {

  /**
   * The types of events that are dispatched by {@link CommModel}. The event
   * class is {@link CommModelEvent}. Listeners can be added via
   * {@link CommModel#getEventAPI()}.
   * @author Rinde van Lon
   */
  public enum EventTypes {

    /**
     * Event type indicating that a new {@link CommUser} is added to the
     * {@link CommModel}.
     */
    ADD_COMM_USER,

    /**
     * Event type indicating that a {@link CommUser} is removed from the
     * {@link CommModel}.
     */
    REMOVE_COMM_USER;
  }

  private final double defaultReliability;
  private final Optional<Double> defaultMaxRange;
  private final BiMap<CommUser, CommDevice> usersDevices;
  private ImmutableBiMap<CommUser, CommDevice> usersDevicesSnapshot;
  private final RandomGenerator randomGenerator;
  private final BiMap<CommUser, CommDevice> unregisteredUsersDevices;
  private boolean usersHasChanged;
  private final EventDispatcher eventDispatcher;

  CommModel(RandomGenerator rng, Builder b) {
    defaultReliability = b.defaultReliability();
    defaultMaxRange = b.defaultMaxRange();
    usersHasChanged = false;
    usersDevices = Maps.synchronizedBiMap(
      LinkedHashBiMap.<CommUser, CommDevice>create());
    unregisteredUsersDevices = LinkedHashBiMap.<CommUser, CommDevice>create();
    usersDevicesSnapshot = ImmutableBiMap.of();
    eventDispatcher = new EventDispatcher(EventTypes.values());
    randomGenerator = rng;
  }

  /**
   * @return The {@link EventAPI} which allows adding listeners for events on
   *         this {@link CommModel}.
   */
  public EventAPI getEventAPI() {
    return eventDispatcher.getPublicEventAPI();
  }

  @Override
  public boolean register(CommUser commUser) {
    checkArgument(!contains(commUser), "%s is already registered.", commUser);
    if (unregisteredUsersDevices.containsKey(commUser)) {
      // re-register, reuse already created device
      final CommDevice dev = unregisteredUsersDevices.remove(commUser);
      dev.register();
      addDevice(dev, commUser);
    } else {
      final CommDeviceBuilder builder = CommDevice.builder(this, commUser)
        .setReliability(defaultReliability);
      commUser.setCommDevice(builder);
      checkState(
        builder.isUsed(),
        "%s is not implemented correctly, a CommDevice must be constructed in"
          + " setCommDevice()",
        commUser);
    }
    return true;
  }

  @Override
  public boolean unregister(CommUser commUser) {
    checkArgument(contains(commUser), "CommModel does not contain %s.",
      commUser);

    final CommDevice unregDevice = usersDevices.remove(commUser);
    unregDevice.unregister();
    unregisteredUsersDevices.put(commUser, unregDevice);
    usersHasChanged = true;
    if (eventDispatcher.hasListenerFor(EventTypes.REMOVE_COMM_USER)) {
      eventDispatcher.dispatchEvent(new CommModelEvent(
        EventTypes.REMOVE_COMM_USER, this, unregDevice, commUser));
    }
    return true;
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
  @Nonnull
  public <U> U get(Class<U> clazz) {
    checkArgument(clazz == CommModel.class);
    return clazz.cast(this);
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

  /**
   * @return An immutable copy of the bimap containing all {@link CommUser}s and
   *         {@link CommDevice}s.
   */
  public ImmutableBiMap<CommUser, CommDevice> getUsersAndDevices() {
    if (usersHasChanged) {
      synchronized (usersDevices) {
        usersDevicesSnapshot = ImmutableBiMap.copyOf(usersDevices);
        usersHasChanged = false;
      }
    }
    return usersDevicesSnapshot;
  }

  void send(Message msg, double senderReliability) {
    // direct
    if (msg.to().isPresent()) {
      if (usersDevices.containsKey(msg.to().get())) {
        final CommDevice recipient = usersDevices.get(msg.to().get());
        doSend(msg, msg.to().get(), recipient, senderReliability);
      }
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
      && hasSucces(sendReliability, recipient.getReliability())) {
      recipient.receive(msg);
    }
  }

  void addDevice(CommDevice device, CommUser user) {
    usersDevices.put(user, device);
    usersHasChanged = true;
    if (eventDispatcher.hasListenerFor(EventTypes.ADD_COMM_USER)) {
      eventDispatcher.dispatchEvent(new CommModelEvent(
        EventTypes.ADD_COMM_USER, this, device, user));
    }
  }

  boolean hasSucces(double senderReliability, double receiverReliability) {
    if (senderReliability == 1d && receiverReliability == 1d) {
      return true;
    }
    return randomGenerator.nextDouble() < senderReliability
      * receiverReliability;
  }

  /**
   * @return A new builder for creating a {@link CommModel}.
   */
  public static Builder builder() {
    return Builder.create();
  }

  static void checkReliability(double reliability) {
    checkArgument(reliability >= 0d && reliability <= 1d,
      "Reliability must be 0 <= r <= 1, found %s.", reliability);
  }

  static void checkMaxRange(double maxRange) {
    checkArgument(maxRange >= 0d);
  }

  /**
   * A builder for creating a {@link CommModel}.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder
      extends AbstractModelBuilder<CommModel, CommUser>
      implements Serializable {
    private static final long serialVersionUID = -6598454973114403967L;
    private static final double DEFAULT_RELIABILITY = 1d;

    Builder() {
      setDependencies(RandomProvider.class);
      setProvidingTypes(CommModel.class);
    }

    static Builder create() {
      return new AutoValue_CommModel_Builder(DEFAULT_RELIABILITY,
        Optional.<Double>absent());
    }

    abstract double defaultReliability();

    abstract Optional<Double> defaultMaxRange();

    /**
     * Returns a copy of this builder with the reliability of the device to be
     * constructed set to the specified value. The reliability is applied for
     * both sending and receiving messages. Reliability must be
     * <code>0 &le; r &le; 1</code>.
     * @param reliability The reliability to set.
     * @return A new instance of {@link Builder} with reliability set to the
     *         specified value.
     */
    @CheckReturnValue
    public Builder withDefaultDeviceReliability(double reliability) {
      checkReliability(reliability);
      return new AutoValue_CommModel_Builder(reliability, defaultMaxRange());
    }

    /**
     * Returns a copy of this builder with the default maximum range for all
     * devices set to the specified value. By default devices will only be able
     * to send messages to other devices that are within this max range.
     * @param maxRange The maxRange to set.
     * @return A new instance of {@link Builder} with max range set to the
     *         specified value.
     */
    @CheckReturnValue
    public Builder withDefaultDeviceMaxRange(double maxRange) {
      checkMaxRange(maxRange);
      return new AutoValue_CommModel_Builder(defaultReliability(),
        Optional.of(maxRange));
    }

    @Override
    public CommModel build(DependencyProvider dependencyProvider) {
      return new CommModel(
        dependencyProvider.get(RandomProvider.class).newInstance(), this);
    }
  }

  /**
   * Event class for events dispatched by {@link CommModel}. Contains references
   * to {@link CommDevice} and {@link CommUser} that caused the event.
   *
   * @author Rinde van Lon
   */
  public static final class CommModelEvent extends Event {
    private final CommDevice device;
    private final CommUser user;

    CommModelEvent(Enum<?> type, Object pIssuer, CommDevice d, CommUser u) {
      super(type, pIssuer);
      device = d;
      user = u;
    }

    /**
     * @return the device
     */
    public CommDevice getDevice() {
      return device;
    }

    /**
     * @return the user
     */
    public CommUser getUser() {
      return user;
    }
  }
}
