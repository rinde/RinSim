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

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.google.auto.value.AutoValue;

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
      isPlaying = true;
      eventDispatcher.dispatchEvent(new Event(ClockEventType.STARTED, this));
    }
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
    return Builder.create(Builder.DEFAULT_TIME_STEP, Builder.DEFAULT_TIME_UNIT);
  }

  /**
   * A builder for constructing {@link TimeModel} instances.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder extends
    AbstractModelBuilder<TimeModel, TickListener> {

    /**
     * The default time step: <code>1000</code>.
     */
    public static final long DEFAULT_TIME_STEP = 1000L;

    /**
     * The default time unit: ms.
     */
    public static final Unit<Duration> DEFAULT_TIME_UNIT = SI.MILLI(SI.SECOND);

    Builder() {
      setProvidingTypes(Clock.class, ClockController.class);
    }

    /**
     * @return The tick length.
     */
    public abstract long getTickLength();

    /**
     * @return The time unit.
     */
    public abstract Unit<Duration> getTimeUnit();

    /**
     * Returns a copy of this builder with the specified length of a single
     * tick. The default tick length is {@link #DEFAULT_TIME_STEP}.
     * @param tickLength The tick length to set.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withTickLength(long tickLength) {
      return create(tickLength, getTimeUnit());
    }

    /**
     * Returns a copy of this builder with the specified time unit to use. The
     * default time unit is milliseconds.
     * @param timeUnit The time unit to use.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withTimeUnit(Unit<Duration> timeUnit) {
      return create(getTickLength(), timeUnit);
    }

    @CheckReturnValue
    @Override
    public TimeModel build(DependencyProvider dependencyProvider) {
      return new TimeModel(getTickLength(), getTimeUnit());
    }

    static Builder create(long tickLength, Unit<Duration> timeUnit) {
      return new AutoValue_TimeModel_Builder(tickLength, timeUnit);
    }
  }
}
