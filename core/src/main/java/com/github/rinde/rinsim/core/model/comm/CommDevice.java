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
import static com.google.common.base.Verify.verifyNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;

/**
 * A communication device that can be used to communicate with other
 * {@link CommUser}s. Instances can be constructed via {@link CommDeviceBuilder}
 * .
 * @author Rinde van Lon
 */
public final class CommDevice {
  private final CommModel model;
  private final CommUser user;
  private final double reliability;
  private final Optional<Double> maxRange;
  private final Predicate<CommUser> rangePredicate;

  private final List<Message> unreadMessages;
  private final List<Message> outbox;

  CommDevice(CommDeviceBuilder builder) {
    model = builder.model;
    user = builder.user;
    reliability = builder.deviceReliability;
    maxRange = builder.deviceMaxRange;
    if (maxRange.isPresent()) {
      rangePredicate = new RangePredicate(user, maxRange.get());
    } else {
      rangePredicate = Predicates.alwaysTrue();
    }
    unreadMessages = new ArrayList<>();
    outbox = new ArrayList<>();
    model.addDevice(this, user);
  }

  /**
   * Retrieves the unread messages that this device has received. Calling this
   * method will clear the unread messages of this device.
   * @return An immutable list of {@link Message}s.
   */
  public ImmutableList<Message> getUnreadMessages() {
    final ImmutableList<Message> msgs = ImmutableList
        .copyOf(unreadMessages);
    unreadMessages.clear();
    return msgs;
  }

  /**
   * @return The reliability of this device for sending and receiving messages.
   */
  public double getReliability() {
    return reliability;
  }

  /**
   * @return The maximum range for sending messages, or
   *         {@link Optional#absent()} if the device as unlimited range.
   */
  public Optional<Double> getMaxRange() {
    return maxRange;
  }

  // talk about reliability. send success = sender reliability * receiver
  // reliability. max range, if recipient is further away, message will not be
  // send
  public void send(MessageContents msg, CommUser recipient) {
    checkArgument(user != recipient,
        "Can not send message to self %s.",
        recipient);
    checkArgument(model.contains(recipient),
        "%s can not send message to unknown recipient: %s.",
        user, recipient);
    outbox.add(Message.createDirect(user, recipient, msg, rangePredicate));
  }

  public void broadcast(MessageContents msg) {
    outbox.add(Message.createBroadcast(user, msg, rangePredicate));
  }

  void receive(Message m) {
    unreadMessages.add(m);
  }

  void sendMessages() {
    for (final Message msg : outbox) {
      model.send(msg, reliability);
    }
    outbox.clear();
  }

  static CommDeviceBuilder builder(CommModel m, CommUser u) {
    return new CommDeviceBuilder(m, u);
  }

  static class RangePredicate implements Predicate<CommUser> {
    private final CommUser user;
    private final double range;

    RangePredicate(CommUser u, double r) {
      user = u;
      range = r;
    }

    @Override
    public boolean apply(@Nullable CommUser input) {
      return Point.distance(user.getPosition(),
          verifyNotNull(input).getPosition()) <= range;
    }
  }
}
