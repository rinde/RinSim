/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.measure.quantity.Duration;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

/**
 * This model is an implementation of a simulation clock. It notifies
 * {@link TickListener}s of time progress. Instances can be obtained via
 * {@link #builder()}.
 * <p>
 * <b>Model properties</b>
 * <ul>
 * <li><i>Associated type:</i> {@link TickListener}.</li>
 * <li><i>Provides:</i> {@link Clock} and {@link ClockController} and optionally
 * {@link RealtimeClockController} (if created via
 * {@link Builder#withRealTime()}).</li>
 * <li><i>Dependencies:</i> none.</li>
 * </ul>
 * See {@link ModelBuilder} for more information about model properties.
 * @author Rinde van Lon
 */
public abstract class TimeModel extends AbstractModel<TickListener>
    implements ClockController {
  private static final Logger LOGGER =
    LoggerFactory.getLogger(RealtimeModel.class);

  final TimeLapse timeLapse;
  final EventDispatcher eventDispatcher;
  volatile boolean isTicking;
  private volatile Set<TickListener> tickListeners;

  TimeModel(AbstractBuilder<?> builder, Enum<?>... additionalEventTypes) {
    tickListeners = new CopyOnWriteArraySet<>();
    final Set<Enum<?>> allEventTypes = ImmutableSet.<Enum<?>>builder()
      .add(ClockEventType.values())
      .add(additionalEventTypes)
      .build();
    eventDispatcher = new EventDispatcher(allEventTypes);

    // time lapse is reused in a Flyweight kind of style
    timeLapse = new TimeLapse(builder.getTimeUnit(), 0L,
      builder.getTickLength());
  }

  /**
   * Start the simulation.
   */
  @Override
  public final void start() {
    checkState(!isTicking(), "Time is already ticking.");
    isTicking = true;
    eventDispatcher.dispatchEvent(new Event(ClockEventType.STARTED, this));
    doStart();
    eventDispatcher.dispatchEvent(new Event(ClockEventType.STOPPED, this));
  }

  @Override
  public abstract void stop();

  abstract void doStart();

  @Override
  public abstract void tick();

  @OverridingMethodsMustInvokeSuper
  void cleanUpAfterException() {
    eventDispatcher.dispatchEvent(new Event(ClockEventType.STOPPED, this));
  }

  @Override
  @Nonnull
  public <U> U get(Class<U> clazz) {
    if (clazz == Clock.class || clazz == ClockController.class) {
      return clazz.cast(this);
    }
    throw new IllegalArgumentException(
      getClass().getSimpleName() + " does not provide instances of "
        + clazz.getName() + ".");
  }

  @OverridingMethodsMustInvokeSuper
  @Override
  public boolean register(TickListener element) {
    checkArgument(tickListeners.add(element),
      "A TickListener can not be registered more than once: %s.", element);
    return true;
  }

  @OverridingMethodsMustInvokeSuper
  @Override
  public boolean unregister(TickListener element) {
    return tickListeners.remove(element);
  }

  final void tickImpl() {
    for (final TickListener t : tickListeners) {
      timeLapse.reset();
      t.tick(timeLapse);
    }
    // in the after tick the TimeLapse can no longer be consumed
    timeLapse.consumeAll();
    for (final TickListener t : tickListeners) {
      t.afterTick(timeLapse);
    }
    // advance time
    timeLapse.next();

    if (Thread.interrupted() && isTicking()) {
      LOGGER.info("Simulation interrupted after tick {}, stopping.", timeLapse);
      stop();
    }
  }

  /**
   * @return true if time is ticking, false otherwise.
   */
  @Override
  @CheckReturnValue
  public boolean isTicking() {
    return isTicking;
  }

  @Override
  @CheckReturnValue
  public Unit<Duration> getTimeUnit() {
    return timeLapse.getTimeUnit();
  }

  @Override
  @CheckReturnValue
  public long getCurrentTime() {
    return timeLapse.getStartTime();
  }

  @Override
  @CheckReturnValue
  public long getTickLength() {
    return timeLapse.getTickLength();
  }

  @VisibleForTesting
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
    return Builder.create(AbstractBuilder.DEFAULT_TIME_STEP,
      AbstractBuilder.DEFAULT_TIME_UNIT);
  }

  /**
   * Abstract builder for constructing {@link TimeModel} instances.
   * @author Rinde van Lon
   * @param <T> The builder type itself, necessary to make an inheritance-based
   *          builder.
   */
  public abstract static class AbstractBuilder<T>
      extends AbstractModelBuilder<TimeModel, TickListener>
      implements Serializable {

    /**
     * The default time step: <code>1000</code>.
     */
    public static final long DEFAULT_TIME_STEP = 1000L;

    /**
     * The default time unit: ms.
     */
    public static final Unit<Duration> DEFAULT_TIME_UNIT = SI.MILLI(SI.SECOND);

    private static final long serialVersionUID = 4029776255118617541L;

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
    public abstract T withTickLength(long tickLength);

    /**
     * Returns a copy of this builder with the specified time unit to use. The
     * default time unit is milliseconds.
     * @param timeUnit The time unit to use.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public abstract T withTimeUnit(Unit<Duration> timeUnit);
  }

  /**
   * Builder for constructing {@link TimeModel} instances.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder extends AbstractBuilder<Builder> {
    private static final long serialVersionUID = 4197062023514532225L;

    Builder() {
      setProvidingTypes(Clock.class, ClockController.class);
    }

    @Override
    public Builder withTickLength(long tickLength) {
      return create(tickLength, getTimeUnit());
    }

    @Override
    public Builder withTimeUnit(Unit<Duration> timeUnit) {
      return create(getTickLength(), timeUnit);
    }

    /**
     * Create a time model that synchronized ticks to the real time. An
     * additional type is provided by this model:
     * {@link RealtimeClockController}.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public RealtimeBuilder withRealTime() {
      return RealtimeBuilder.create(getTickLength(), getTimeUnit(),
        ClockMode.REAL_TIME);
    }

    @CheckReturnValue
    @Override
    public TimeModel build(DependencyProvider dependencyProvider) {
      return new SimulatedTimeModel(this);
    }

    static Builder create(long tickLength, Unit<Duration> timeUnit) {
      return new AutoValue_TimeModel_Builder(tickLength, timeUnit);
    }
  }

  /**
   * Builder for real-time version of {@link TimeModel}. Besides providing
   * {@link Clock} and {@link ClockController}, {@link RealtimeClockController}
   * is also provided.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class RealtimeBuilder
      extends AbstractBuilder<RealtimeBuilder> {

    private static final long serialVersionUID = 7255633280244047198L;

    RealtimeBuilder() {
      setProvidingTypes(Clock.class, ClockController.class,
        RealtimeClockController.class);
    }

    /**
     * @return The {@link ClockMode} the time model will start in.
     */
    public abstract ClockMode getClockMode();

    /**
     * Sets the {@link ClockMode} the model should start with. By default the
     * mode is {@link ClockMode#REAL_TIME}.
     * @param mode The mode to start with, can be either
     *          {@link ClockMode#REAL_TIME} or {@link ClockMode#STOPPED}.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public RealtimeBuilder withStartInClockMode(ClockMode mode) {
      checkArgument(mode != ClockMode.STOPPED,
        "Can not use %s as starting mode in %s.", ClockMode.STOPPED,
        toString());
      return create(getTickLength(), getTimeUnit(), mode);
    }

    @Override
    public RealtimeBuilder withTickLength(long tickLength) {
      return create(tickLength, getTimeUnit(), getClockMode());
    }

    @Override
    public RealtimeBuilder withTimeUnit(Unit<Duration> timeUnit) {
      return create(getTickLength(), timeUnit, getClockMode());
    }

    @Override
    public TimeModel build(DependencyProvider dependencyProvider) {
      return new RealtimeModel(this);
    }

    @Override
    public String toString() {
      return super.toString() + ".withRealtime()";
    }

    static RealtimeBuilder create(long length, Unit<Duration> unit,
        ClockMode mode) {
      return new AutoValue_TimeModel_RealtimeBuilder(length, unit, mode);
    }
  }
}
