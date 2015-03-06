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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;

/**
 * A communication device that can be used to communicate. Instances can be
 * constructed via {@link CommDeviceBuilder}.
 * @author Rinde van Lon
 */
public final class CommDevice {
  private final CommModel model;
  private final CommUser user;

  private final List<Message> unreadMessages;
  private final List<Message> outbox;

  CommDevice(CommDeviceBuilder builder) {
    model = builder.model;
    user = builder.user;
    unreadMessages = new ArrayList<>();
    outbox = new ArrayList<>();
    model.addDevice(this, user);
  }

  void receive(Message m) {
    // TODO apply receive probability
    unreadMessages.add(m);
  }

  void sendMessages() {
    for (final Message msg : outbox) {
      model.send(msg);
    }
    outbox.clear();
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

  public void send(MessageContents msg, CommUser recipient) {
    outbox.add(Message.createDirect(user, recipient, msg));
  }

  public void broadcast(MessageContents msg) {
    outbox.add(Message.createBroadcast(user, msg,
        Predicates.<CommUser> alwaysTrue()));
  }

  public void broadcast(MessageContents msg, double maxRange) {
    outbox.add(Message.createBroadcast(user, msg,
        new RangePredicate(user, maxRange)));
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
      assert input != null;
      return Point.distance(user.getPosition(), input.getPosition()) <= range;
    }
  }

}
