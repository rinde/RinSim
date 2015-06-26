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

import java.io.Serializable;
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
public abstract class TimeModel extends AbstractModel<TickListener>
  implements ClockController {
  final TimeLapse timeLapse;

  private volatile Set<TickListener> tickListeners;
  volatile boolean isTicking;
  private final EventDispatcher eventDispatcher;

  TimeModel(Builder builder) {
    tickListeners = new CopyOnWriteArraySet<>();
    eventDispatcher = new EventDispatcher(ClockEventType.values());

    // time lapse is reused in a Flyweight kind of style
    timeLapse = new TimeLapse(builder.getTimeUnit(), 0L,
      builder.getTickLength());
  }

  /**
   * Start the simulation.
   */
  @Override
  public final void start() {
    if (!isTicking()) {
      isTicking = true;
      eventDispatcher.dispatchEvent(new Event(ClockEventType.STARTED, this));
    }
    doStart();
    eventDispatcher.dispatchEvent(new Event(ClockEventType.STOPPED, this));
  }

  @Override
  public final void stop() {
    doStop();

  }

  abstract void doStart();

  abstract void doStop();

  @Override
  public abstract void tick();

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

  protected final void tickImpl() {
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
    return Builder.create(Builder.DEFAULT_TIME_STEP, Builder.DEFAULT_TIME_UNIT,
      Builder.DEFAULT_TICK_FACTORY);
  }

  /**
   * A builder for constructing {@link TimeModel} instances.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder extends
    AbstractModelBuilder<TimeModel, TickListener> implements Serializable {

    private static final long serialVersionUID = 4029776255118617541L;

    /**
     * The default time step: <code>1000</code>.
     */
    public static final long DEFAULT_TIME_STEP = 1000L;

    /**
     * The default time unit: ms.
     */
    public static final Unit<Duration> DEFAULT_TIME_UNIT = SI.MILLI(SI.SECOND);

    static final TimeModelFactory DEFAULT_TICK_FACTORY = TMFactories.SIMULATED;

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

    abstract TimeModelFactory getTickStrategyFactory();

    /**
     * Returns a copy of this builder with the specified length of a single
     * tick. The default tick length is {@link #DEFAULT_TIME_STEP}.
     * @param tickLength The tick length to set.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withTickLength(long tickLength) {
      return create(tickLength, getTimeUnit(), getTickStrategyFactory());
    }

    /**
     * Returns a copy of this builder with the specified time unit to use. The
     * default time unit is milliseconds.
     * @param timeUnit The time unit to use.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withTimeUnit(Unit<Duration> timeUnit) {
      return create(getTickLength(), timeUnit, getTickStrategyFactory());
    }

    @CheckReturnValue
    public Builder withRealTime() {
      return create(getTickLength(), getTimeUnit(), TMFactories.REAL_TIME);
    }

    @CheckReturnValue
    @Override
    public TimeModel build(DependencyProvider dependencyProvider) {
      return getTickStrategyFactory().create(this);
    }

    static Builder create(long tickLength, Unit<Duration> timeUnit,
      TimeModelFactory tf) {
      return new AutoValue_TimeModel_Builder(tickLength, timeUnit, tf);
    }
  }

  interface TimeModelFactory {
    TimeModel create(TimeModel.Builder builder);
  }

  enum TMFactories implements TimeModelFactory {
    SIMULATED {
      @Override
      public TimeModel create(Builder builder) {
        return new SimulatedTimeModel(builder);
      }
    },
    REAL_TIME {
      @Override
      public TimeModel create(Builder builder) {
        return new RealTimeModel(builder);
      }
    }
  }

  // static class RealtimeTick implements TickStrategy {
  // long prevTime;
  // long timeStep;
  //
  // RealtimeTick(TimeLapse tl) {
  // prevTime = System.nanoTime();
  // timeStep = Measure.valueOf(tl.getTickLength(), tl.getTimeUnit())
  // .longValue(SI.NANO(SI.SECOND));
  // }
  //
  // @Override
  // public void execute(TimeLapse tl, Set<TickListener> tls) {
  // SimulatedTime.INSTANCE.execute(tl, tls);
  // final long duration = System.nanoTime() - prevTime;
  // checkState(duration <= timeStep);
  //
  // try {
  // final long diff = timeStep - duration;
  // final long ms = DoubleMath.roundToLong(diff / 1000000,
  // RoundingMode.DOWN);
  // System.out.println(duration);
  // System.out.println(" > sleep " + diff + " nano seconds, ms " + ms
  // + " nanos " + (diff - ms * 1000000));
  // Thread.sleep(ms, (int) (diff - ms * 1000000));
  // final long t = System.nanoTime();
  // System.out.println("time: " + (t - prevTime));
  // prevTime = t;
  // } catch (final InterruptedException e) {
  // throw new IllegalStateException(e);
  // }
  // }
  // }
}
