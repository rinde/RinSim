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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.CheckReturnValue;
import javax.measure.quantity.Duration;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.model.AbstractModel;
import com.google.common.base.Supplier;

/**
 * @author Rinde van Lon
 *
 */
public final class TimeModel extends AbstractModel<TickListener>
  implements Clock {

  private volatile Set<TickListener> tickListeners;
  private volatile long time;
  private volatile boolean isPlaying;

  private final long timeStep;
  private final TimeLapse timeLapse;

  TimeModel(long tickLength, Unit<Duration> unit) {
    tickListeners = new CopyOnWriteArraySet<>();
    // Collections .synchronizedSet(new LinkedHashSet<TickListener>());
    time = 0L;

    timeStep = tickLength;

    // time lapse is reused in a Flyweight kind of style
    timeLapse = new TimeLapse(unit);
  }

  /**
   * Start the simulation.
   */
  @Override
  public void start() {
    isPlaying = true;
    while (isPlaying) {
      tick();
    }
  }

  @Override
  public void stop() {
    isPlaying = false;
  }

  public void tick() {
    final long end = time + timeStep;
    for (final TickListener t : tickListeners) {
      timeLapse.initialize(time, end);
      t.tick(timeLapse);
    }
    timeLapse.initialize(time, end);
    // in the after tick the TimeLapse can no longer be consumed
    timeLapse.consumeAll();
    for (final TickListener t : tickListeners) {
      t.afterTick(timeLapse);
    }
    time += timeStep;

  }

  @Override
  public boolean register(TickListener element) {
    checkArgument(tickListeners.add(element),
      "A TickListener can not be registered more than once: %s.", element);
    return true;
  }

  @Override
  public boolean unregister(TickListener element) {
    return tickListeners.remove(element);
  }

  /**
   * @return true if time is ticking, false otherwise.
   */
  @Override
  @CheckReturnValue
  public boolean isTicking() {
    return isPlaying;
  }

  @Override
  @CheckReturnValue
  public Unit<Duration> getTimeUnit() {
    return timeLapse.getTimeUnit();
  }

  @Override
  @CheckReturnValue
  public long getCurrentTime() {
    return time;
  }

  @Override
  @CheckReturnValue
  public long getTimeStep() {
    return timeStep;
  }

  @CheckReturnValue
  Set<TickListener> getTickListeners() {
    return Collections.unmodifiableSet(tickListeners);
  }

  @CheckReturnValue
  public static Supplier<TimeModel> supplier(long step, Unit<Duration> unit) {
    return new TimeModelSupplier(step, unit);
  }

  @CheckReturnValue
  public static Supplier<TimeModel> defaultSupplier() {
    return new TimeModelSupplier(1000L, SI.MILLI(SI.SECOND));
  }

  static class TimeModelSupplier implements Supplier<TimeModel> {
    private final long step;
    private final Unit<Duration> unit;

    TimeModelSupplier(long s, Unit<Duration> u) {
      step = s;
      unit = u;
    }

    @Override
    public TimeModel get() {
      return new TimeModel(step, unit);
    }
  }
}
