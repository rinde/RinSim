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
import static java.util.Objects.hash;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.measure.quantity.Duration;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;

/**
 * This model is an implementation of a simulation clock. It notifies
 * {@link TickListener}s of time progress. Instances can be obtained via
 * {@link #builder()}.
 * <p>
 * <b>Model properties</b>
 * <ul>
 * <li><i>Associated type:</i> {@link TickListener}.</li>
 * <li><i>Provides:</i> {@link Clock} and {@link ClockController}.</li>
 * <li><i>Dependencies:</i> none.</li>
 * </ul>
 * See {@link ModelBuilder} for more information about model properties.
 * @author Rinde van Lon
 */
public final class TimeModel extends AbstractModel<TickListener>
  implements ClockController {
  private volatile Set<TickListener> tickListeners;
  private volatile long time;
  private volatile boolean isPlaying;

  private final long timeStep;
  private final TimeLapse timeLapse;
  private final EventDispatcher eventDispatcher;

  TimeModel(long tickLength, Unit<Duration> unit) {
    tickListeners = new CopyOnWriteArraySet<>();
    time = 0L;
    timeStep = tickLength;

    eventDispatcher = new EventDispatcher(ClockEventType.values());

    // time lapse is reused in a Flyweight kind of style
    timeLapse = new TimeLapse(unit);
  }

  /**
   * Start the simulation.
   */
  @Override
  public void start() {
    if (!isTicking()) {
      eventDispatcher.dispatchEvent(new Event(ClockEventType.STARTED, this));
    }
    isPlaying = true;
    while (isPlaying) {
      tick();
    }
    eventDispatcher.dispatchEvent(new Event(ClockEventType.STOPPED, this));
  }

  @Override
  public void stop() {
    isPlaying = false;
  }

  @Override
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
  public <U> U get(Class<U> clazz) {
    if (clazz == Clock.class || clazz == ClockController.class) {
      return clazz.cast(this);
    }
    throw new IllegalArgumentException(
      "This model does not provides instances of " + clazz + ".");
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

  @Override
  public EventAPI getEventAPI() {
    return eventDispatcher.getPublicEventAPI();
  }

  /**
   * @return A new {@link Builder} instance for constructing {@link TimeModel}
   *         instances.
   */
  @CheckReturnValue
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for constructing {@link TimeModel} instances.
   * @author Rinde van Lon
   */
  public static class Builder extends
    AbstractModelBuilder<TimeModel, TickListener> {

    /**
     * The default time step: <code>1000</code>.
     */
    public static final long DEFAULT_TIME_STEP = 1000L;

    /**
     * The default time unit: ms.
     */
    public static final Unit<Duration> DEFAULT_TIME_UNIT = SI.MILLI(SI.SECOND);

    private long timeLength;
    private Unit<Duration> timeUnit;

    Builder() {
      setProvidingTypes(Clock.class, ClockController.class);
      timeLength = DEFAULT_TIME_STEP;
      timeUnit = DEFAULT_TIME_UNIT;
    }

    /**
     * Sets the length of a single tick. The default tick length is
     * <code>1000</code>.
     * @param length The tick length to set.
     * @return This, as per the builder pattern.
     */
    public Builder setTickLength(long length) {
      timeLength = length;
      return this;
    }

    /**
     * Sets the time unit to use. The default time unit is milliseconds.
     * @param unit The time unit to use.
     * @return This, as per the builder pattern.
     */
    public Builder setTimeUnit(Unit<Duration> unit) {
      timeUnit = unit;
      return this;
    }

    @CheckReturnValue
    @Override
    public TimeModel build(DependencyProvider dependencyProvider) {
      return new TimeModel(timeLength, timeUnit);
    }

    @Override
    public int hashCode() {
      return hash(timeLength, timeUnit);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (!(other instanceof Builder)) {
        return false;
      }
      final Builder o = (Builder) other;
      return Objects.equals(timeLength, o.timeLength)
        && Objects.equals(timeUnit, o.timeUnit);
    }

    /**
     * @return The time unit.
     */
    public Unit<Duration> getTimeUnit() {
      return timeUnit;
    }

    /**
     * @return The tick length.
     */
    public long getTickLength() {
      return timeLength;
    }
  }
}
