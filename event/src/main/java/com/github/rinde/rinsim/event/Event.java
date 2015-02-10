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
package com.github.rinde.rinsim.event;

import javax.annotation.Nullable;

import com.google.common.base.Optional;
// TODO convert to interface and create a SimpleEvent which is equivalent to current implementation
/**
 * The base event class. It is immutable.
 * @author Rinde van Lon 
 * @author Bartosz Michalik 
 */
public class Event {

  /**
   * The type of event.
   */
  protected final Enum<?> eventType;
  private final Optional<Object> issuer;

  /**
   * Create a new event instance.
   * @param type the event type.
   * @param pIssuer The event issuer, may be null.
   */
  public Event(Enum<?> type, @Nullable Object pIssuer) {
    eventType = type;
    issuer = Optional.fromNullable(pIssuer);
  }

  /**
   * Create a new event without a issuer.
   * @param type The event type.
   */
  protected Event(Enum<?> type) {
    this(type, null);
  }

  /**
   * @return <code>true</code> if this event has an issuer, <code>false</code>
   *         otherwise.
   */
  public boolean hasIssuer() {
    return issuer.isPresent();
  }

  /**
   * @return The event issuer.
   */
  public Object getIssuer() {
    return issuer.get();
  }

  /**
   * @return The type of event.
   */
  public Enum<?> getEventType() {
    return eventType;
  }

  @Override
  public String toString() {
    return "[Event " + eventType + "]";
  }
}
