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

import static com.google.common.base.Preconditions.checkArgument;

import javax.measure.quantity.Duration;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.TickListener;

/**
 * Represents a consumable interval of time: [start, end). Instances of time
 * lapse are handed out by the {@link Simulator} and can be received by
 * implementing the {@link TickListener} interface.
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public final class TimeLapse {

  private final Unit<Duration> timeUnit;
  private long startTime;
  private long endTime;
  private long timeLeft;

  TimeLapse(Unit<Duration> unit) {
    timeUnit = unit;
  }

  void initialize(long start, long end) {
    checkArgument(start >= 0, "time must be positive");
    checkArgument(end > start, "end time must be after start time");
    startTime = start;
    endTime = end;
    timeLeft = end - start;
  }

  /**
   * Consumes the specified amount of time, where time must be strictly positive
   * and there must be enough time left as specified by {@link #getTimeLeft()}.
   * @param time The time to consume.
   */
  public void consume(long time) {
    checkArgument(time >= 0,
        "the time to consume must be a positive value, it is %s.", time);
    checkArgument(timeLeft - time >= 0,
        "there is not enough time left to consume %s.", time);
    timeLeft -= time;
  }

  /**
   * Consumes the entire time lapse.
   */
  public void consumeAll() {
    timeLeft = 0;
  }

  /**
   * @return If there is time left to consume.
   */
  public boolean hasTimeLeft() {
    return timeLeft > 0;
  }

  /**
   * @return The amount of time left to consume.
   */
  public long getTimeLeft() {
    return timeLeft;
  }

  /**
   * @return The current time taking into account any time consumption in this
   *         instance. When looking for the time at the start of this time lapse
   *         use {@link #getStartTime()}.
   */
  public long getTime() {
    return endTime - timeLeft;
  }

  /**
   * @return The step (or length) of this time lapse.
   */
  public long getTimeStep() {
    return endTime - startTime;
  }

  /**
   * @return The time that was previously consumed in this time lapse.
   */
  public long getTimeConsumed() {
    return endTime - startTime - timeLeft;
  }

  /**
   * @return The start time of this time lapse.
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * @return The end time of this time lapse.
   */
  public long getEndTime() {
    return endTime;
  }

  /**
   * Checks whether the specified time lies in this {@link TimeLapse}.
   * @param t The time to check.
   * @return <code>true</code> if the specified time is in this
   *         {@link TimeLapse}, <code>false</code> otherwise.
   */
  public boolean isIn(long t) {
    return t >= startTime && t < endTime;
  }

  /**
   * @return The time unit of this {@link TimeLapse}.
   */
  public Unit<Duration> getTimeUnit() {
    return timeUnit;
  }

  @Override
  public String toString() {
    return new StringBuilder("[").append(startTime).append(",").append(endTime)
        .append(")").toString();
  }
}
