/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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

import com.google.auto.value.AutoValue;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * Immutable. Should not be subclassed, can only be constructed indirectly via
 * {@link CommDevice}.
 * @author Rinde van Lon
 */
@AutoValue
public abstract class Message {

  Message() {}

  abstract CommUser from();

  abstract Optional<CommUser> to();

  abstract MessageContents contents();

  abstract Predicate<CommUser> predicate();

  public CommUser getSender() {
    return from();
  }

  public MessageContents getContents() {
    return contents();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper("Message")
        .add("sender", getSender())
        .add("contents", getContents())
        .toString();
  }

  static Message createDirect(CommUser from, CommUser to, MessageContents m) {
    return new AutoValue_Message(
        from, Optional.of(to), m, Predicates.<CommUser> alwaysTrue());
  }

  static Message createBroadcast(CommUser from, MessageContents m,
      Predicate<CommUser> p) {
    return new AutoValue_Message(
        from, Optional.<CommUser> absent(), m, p);
  }
}
