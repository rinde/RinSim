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

import java.util.Set;

/**
 * The Event API provides an interface which can be presented to objects which
 * have an interest to receive events of a certain object.
 * 
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public interface EventAPI {

  /**
   * Adds the specified listener. From now on, the specified listener will be
   * notified of events with one of the <code>eventTypes</code>. If
   * <code>eventTypes</code> is not specified, the listener will be notified of
   * <b>all</b> events.
   * @param listener The listener, may not be null.
   * @param eventTypes The {@link Event} types, each type but be a type that is
   *          supported by this EventDispatcher. May not be null. If no
   *          eventTypes are specified, the listener will be notified of
   *          <b>all</b> events.
   */
  void addListener(Listener listener, Enum<?>... eventTypes);

  /**
   * Adds the specified listener. From now on, the specified listener will be
   * notified of events with one of the <code>eventTypes</code>. If
   * <code>eventTypes</code> is empty, the listener will be notified of
   * <b>no</b> events.
   * @param listener The listener, may not be null.
   * @param eventTypes The {@link Event} types, each type but be a type that is
   *          supported by this EventDispatcher. May not be null. If no
   *          eventTypes are specified, the listener will be notified of
   *          <b>no</b> events.
   */
  void addListener(Listener listener, Set<Enum<?>> eventTypes);

  /**
   * Removes the specified listener with the specified event types. From now on,
   * <code>listener</code> will no longer be notified of new {@link Event} s
   * with any of <code>eventTypes</code>. If <code>eventTypes</code> undefined
   * the listener will be completely removed.
   * @param listener The {@link Listener} to remove.
   * @param eventTypes The event types.
   */
  void removeListener(Listener listener, Enum<?>... eventTypes);

  /**
   * Removes the specified listener with the specified event types. From now on,
   * <code>listener</code> will no longer be notified of new {@link Event} s
   * with any of <code>eventTypes</code>. If <code>eventTypes</code> is empty
   * the listener will be completely removed.
   * @param listener The {@link Listener} to remove.
   * @param eventTypes The event types.
   */
  void removeListener(Listener listener, Set<Enum<?>> eventTypes);

  /**
   * Checks if the specified <code>listener</code> is registered as listening to
   * <code>eventType</code>.
   * @param listener The listener to check.
   * @param eventType The type of event.
   * @return <code>true</code> if the listener is listening to
   *         <code>eventType</code>, <code>false</code> otherwise.
   */
  boolean containsListener(Listener listener, Enum<?> eventType);
}
