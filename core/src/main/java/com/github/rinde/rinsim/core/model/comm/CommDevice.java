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
import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;

/**
 * A communication device that can be used to communicate with other
 * {@link CommUser}s. Instances can be constructed via {@link CommDeviceBuilder}
 * . A communication device has two important properties, reliability and range.
 * For more information regarding this properties see
 * {@link #send(MessageContents, CommUser)} and
 * {@link #broadcast(MessageContents)}.
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
  private int receivedCount;
  private boolean registered;

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
    receivedCount = 0;
    model.addDevice(this, user);
    registered = true;
  }

  /**
   * @return The total number of received messages.
   */
  public int getReceivedCount() {
    return receivedCount;
  }

  /**
   * @return The number of unread messages.
   */
  public int getUnreadCount() {
    return unreadMessages.size();
  }

  /**
   * Retrieves the unread messages that this device has received. All messages
   * in the returned list are considered to be read when this method completes.
   * Calling this method will clear the unread messages of this device.
   * @return An immutable list of {@link Message}s.
   */
  public ImmutableList<Message> getUnreadMessages() {
    final ImmutableList<Message> msgs = ImmutableList.copyOf(unreadMessages);
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

  /**
   * Attempts to send a message with the specified contents to the specified
   * recipient. The actual sending of a message is done at the end of the
   * current tick. Based on the reliability, range and position it is determined
   * whether the message will be sent. Until a message is sent it lives in the
   * {@link #getOutbox()} of the device.
   * <p>
   * <b>Reliability</b> If this device has a reliability of <code>p</code> there
   * is a probability of <code>1-p</code> that the message will not be sent. If
   * the receiving device has a reliability of <code>r</code> there is a
   * probability of <code>1-r</code> that the message will not be received at
   * the other end. This means that in practice the probability of a successful
   * delivery is <code>(1-p) * (1-r)</code>.
   * <p>
   * <b>Range</b> If this device has a maximum range the message will only be
   * delivered if the recipient is within that range <i>at the moment of sending
   * at the end of the tick</i>. Note that the range only influences sending of
   * messages, it is possible to receive messages from senders that are outside
   * of its max range.
   * <p>
   * <b>Position</b> If the {@link CommUser} that owns this device has no
   * position ({@link CommUser#getPosition()} is absent) <i>and</i> this device
   * has a maximum range ({@link #getMaxRange()} is present). The message will
   * not be sent and will stay in the outbox of this device (see
   * {@link #getOutbox()}). If at a later stage the {@link CommUser} gets a
   * position all messages that are still in the outbox will be sent. The outbox
   * can be cleared by calling {@link #clearOutbox()}.
   * @param contents The contents to send as part of the message.
   * @param recipient The recipient of the message.
   * @throws IllegalStateException If the device is no longer registered.
   */
  public void send(MessageContents contents, CommUser recipient) {
    checkRegistered();
    checkArgument(user != recipient,
        "Can not send message to self %s.",
        recipient);
    checkArgument(model.contains(recipient),
        "%s can not send message to unknown recipient: %s.",
        user, recipient);
    outbox.add(Message.createDirect(user, recipient, contents, rangePredicate));
  }

  /**
   * Attempts to broadcast a message with the specified contents. The actual
   * sending of a message is done at the end of the current tick. Based on the
   * reliability, range and position it is determined to whom a message will be
   * sent. Until a message is sent it lives in the {@link #getOutbox()} of the
   * device.
   * <p>
   * <b>Reliability</b> If this device has a reliability of <code>p</code> there
   * is a probability of <code>1-p</code> that the message will not be send to a
   * particular receiver. If the receiving device has a reliability of
   * <code>r</code> there is a probability of <code>1-r</code> that the message
   * will not be received at the other end. This means that in practice the
   * probability of a successful delivery is <code>(1-p) * (1-r)</code>.
   * <p>
   * <b>Range</b> If this device has a maximum range the message will only be
   * delivered to the recipients that are within that range <i>at the moment of
   * sending at the end of the tick</i>. Note that the range only influences
   * sending of messages, it is possible to receive messages from senders that
   * are outside of its max range.
   * <p>
   * <b>Position</b> If the {@link CommUser} that owns this device has no
   * position ({@link CommUser#getPosition()} is absent) <i>and</i> this device
   * has a maximum range ({@link #getMaxRange()} is present). The message will
   * not be sent and will stay in the outbox of this device (see
   * {@link #getOutbox()}). If at a later stage the {@link CommUser} gets a
   * position all messages that are still in the outbox will be sent. The outbox
   * can be cleared by calling {@link #clearOutbox()}.
   * @param contents The contents to send as part of the message.
   * @throws IllegalStateException If the device is no longer registered.
   */
  public void broadcast(MessageContents contents) {
    checkRegistered();
    outbox.add(Message.createBroadcast(user, contents, rangePredicate));
  }

  /**
   * Clears all message in the outbox.
   * @throws IllegalStateException If the device is no longer registered.
   */
  public void clearOutbox() {
    checkRegistered();
    outbox.clear();
  }

  /**
   * Returns an unmodifiable view of the outbox of this device. Each invocation
   * of {@link #send(MessageContents, CommUser)} and
   * {@link #broadcast(MessageContents)} will add a message to the outbox. Each
   * message that has been sent is removed from the outbox. Note that if a
   * message is no longer in the outbox it doesn't necessarily imply that it has
   * also been received by the receiver.
   * @return An unmodifiable view of the outbox.
   */
  public List<Message> getOutbox() {
    return Collections.unmodifiableList(outbox);
  }

  void receive(Message m) {
    unreadMessages.add(m);
    receivedCount++;
  }

  void sendMessages() {
    if (!getMaxRange().isPresent() || user.getPosition().isPresent()) {
      for (final Message msg : outbox) {
        model.send(msg, reliability);
      }
      clearOutbox();
    }
  }

  void unregister() {
    registered = false;
  }

  void register() {
    registered = true;
  }

  void checkRegistered() {
    checkState(isRegistered(),
        "This CommDevice is unregistered and can therefore not be used.");
  }

  boolean isRegistered() {
    return registered;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper("CommDevice")
        .add("owner", user)
        .add("reliability", reliability)
        .add("range", maxRange)
        .toString();
  }

  static CommDeviceBuilder builder(CommModel m, CommUser u) {
    return new CommDeviceBuilder(m, u);
  }

  @SuppressWarnings("null")
  static class RangePredicate implements Predicate<CommUser> {
    private final CommUser user;
    private final double range;

    RangePredicate(CommUser u, double r) {
      user = u;
      range = r;
    }

    @Override
    public boolean apply(@Nullable CommUser input) {
      final Optional<Point> pos = user.getPosition();
      verify(pos.isPresent());
      final Optional<Point> otherPos = verifyNotNull(input).getPosition();
      if (otherPos.isPresent()) {
        return Point.distance(pos.get(), otherPos.get()) <= range;
      }
      return false;
    }
  }
}
