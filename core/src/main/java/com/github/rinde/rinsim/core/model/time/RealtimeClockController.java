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

/**
 * Controller for manipulating a real-time clock. A real-time clock synchronizes
 * the simulation ticks to the real time. By default the clock runs in
 * real-time, but it has an option to switch to simulated time (using
 * {@link #switchToSimulatedTime()}). While in simulated time mode the ticks are
 * executed as fast as possible, time consistency is preserved by making sure
 * that each {@link TickListener} does not take more <i>computation time</i>
 * than is allowed according to the {@link TimeLapse} it receives. The
 * simulation mode can be used to 'fast forward' the simulation to a point in
 * time where it becomes relevant again to use the real-time mode (switching
 * back can be done via {@link #switchToRealTime()}).
 * <p>
 * To ensure further time consistency, {@link #tick()} is deprecated and the
 * clock can be started and stopped only once.
 * @author Rinde van Lon
 */
public interface RealtimeClockController extends ClockController {

  /**
   * Tells the clock to switch to real time mode. The real time mode will be in
   * effect starting from the <i>next</i> tick. If the clock is already in
   * {@link ClockMode#REAL_TIME} mode, calling this method will have no effect.
   * When this method is called, any subsequent calls to
   * {@link #switchToSimulatedTime()} <i>during the same tick</i> are ignored.
   * @throws IllegalStateException If this is called after {@link #stop()} is
   *           called.
   */
  void switchToRealTime();

  /**
   * Tells the clock to switch to simulated time mode. The simulated time mode
   * will be in effect starting from the <i>next</i> tick. If the clock is
   * already in {@link ClockMode#SIMULATED} mode, calling this method has no
   * effect.
   * @throws IllegalStateException If this is called after {@link #stop()} is
   *           called.
   */
  void switchToSimulatedTime();

  /**
   * Returns the current mode of the clock, if {@link #isTicking()} is
   * <code>true</code> the mode is either {@link ClockMode#REAL_TIME} or
   * {@link ClockMode#SIMULATED}, if {@link #isTicking()} is <code>false</code>
   * the mode is always {@link ClockMode#STOPPED}.
   * @return The current {@link ClockMode}.
   */
  ClockMode getClockMode();

  /**
   * @deprecated To ensure time consistency this method can not be used, use
   *             {@link #start()} instead.
   * @throws UnsupportedOperationException Always.
   */
  @Deprecated
  @Override
  void tick();

  /**
   * The three different modes the clock can be in.
   * @author Rinde van Lon
   */
  public enum ClockMode {

    /**
     * Indicates the mode where time is synchronized to real time.
     */
    REAL_TIME,

    /**
     * Indicates the mode where all ticks are executed as fast as is possible.
     */
    SIMULATED,

    /**
     * Indicates that the clock is stopped.
     */
    STOPPED;
  }

  /**
   * Additional event types dispatched by {@link RealtimeClockController}. See
   * {@link Clock.ClockEventType} for more event types.
   * @author Rinde van Lon
   */
  public enum RtClockEventType {

    /**
     * Is dispatched right after clock mode has changed,
     * {@link RealtimeClockController#getClockMode()} will return
     * {@link ClockMode#REAL_TIME}.
     */
    SWITCH_TO_REAL_TIME,

    /**
     * Is dispatched right after clock mode has changed,
     * {@link RealtimeClockController#getClockMode()} will return
     * {@link ClockMode#SIMULATED}.
     */
    SWITCH_TO_SIM_TIME;
  }
}
