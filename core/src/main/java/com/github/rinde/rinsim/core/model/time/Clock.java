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
package com.github.rinde.rinsim.core.model.time;

import javax.annotation.CheckReturnValue;
import javax.measure.quantity.Duration;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.event.EventAPI;

/**
 * Represents the clock in a simulation. This class provides several methods
 * that allow to retrieve the time and related properties. For controlling the
 * simulation clock see {@link ClockController}. For getting notified about time
 * progress see {@link TickListener}. For information about events see
 * {@link #getEventAPI()}.
 * @author Rinde van Lon
 */
public interface Clock {

  /**
   * @return <code>true</code> if the clock is ticking, <code>false</code>
   *         otherwise.
   */
  @CheckReturnValue
  boolean isTicking();

  /**
   * @return The unit in which time is expressed.
   */
  @CheckReturnValue
  Unit<Duration> getTimeUnit();

  /**
   * @return The clock's current time.
   */
  @CheckReturnValue
  long getCurrentTime();

  /**
   * @return The length of a single tick.
   */
  @CheckReturnValue
  long getTickLength();

  /**
   * Reference to the {@link EventAPI} of the clock. Can be used to add
   * listeners to events dispatched by the clock. Clock event types are defined
   * in {@link ClockEventType}.
   * @return {@link EventAPI}
   */
  EventAPI getEventAPI();

  /**
   * Enum that describes the possible types of events that the simulator can
   * dispatch.
   */
  public enum ClockEventType {
    /**
     * Indicates that the simulator has stopped.
     */
    STOPPED,

    /**
     * Indicates that the simulator has started.
     */
    STARTED
  }
}
