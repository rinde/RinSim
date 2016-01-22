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

import static com.github.rinde.rinsim.core.model.time.Clock.ClockEventType.STARTED;
import static com.github.rinde.rinsim.core.model.time.Clock.ClockEventType.STOPPED;
import static com.github.rinde.rinsim.core.model.time.RealtimeClockController.RtClockEventType.SWITCH_TO_REAL_TIME;
import static com.github.rinde.rinsim.core.model.time.RealtimeClockController.RtClockEventType.SWITCH_TO_SIM_TIME;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;

/**
 * A utility class that allows to create a log of real-time clock events.
 * Instances can be obtained via {@link #builder()}.
 * @author Rinde van Lon
 */
public final class RealtimeClockLogger extends AbstractModelVoid
    implements TickListener {

  final List<LogEntry> log;
  final RealtimeClockController clock;
  long rtCounter;
  long stCounter;

  RealtimeClockLogger(RealtimeClockController c) {
    log = new ArrayList<>();
    clock = c;
    clock.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        log.add(LogEntry.create(clock.getCurrentTime(),
          clock.getCurrentTime() + clock.getTickLength(),
          clock.getClockMode(), e.getEventType()));
      }
    }, SWITCH_TO_REAL_TIME, SWITCH_TO_SIM_TIME, STARTED, STOPPED);
  }

  /**
   * @return An unmodifiable view on the log.
   */
  public List<LogEntry> getLog() {
    return Collections.unmodifiableList(log);
  }

  /**
   * @return An immutable list of {@link RealtimeTickInfo} objects.
   */
  public ImmutableList<RealtimeTickInfo> getTickInfoList() {
    return ((RealtimeModel) clock).getTickInfoList();
  }

  /**
   * @return The number of real-time ticks.
   */
  public long getRtCount() {
    return rtCounter;
  }

  /**
   * @return The number of simulated time ticks.
   */
  public long getStCount() {
    return stCounter;
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    if (clock.getClockMode() == ClockMode.REAL_TIME) {
      rtCounter++;
    } else {
      stCounter++;
    }
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}

  /**
   * @return A new builder for constructing the logger.
   */
  public static ModelBuilder<?, ?> builder() {
    return new AutoValue_RealtimeClockLogger_Builder();
  }

  /**
   * A log entry consists of the tick in which it occurred, the
   * {@link ClockMode} at that time and the clock event that was dispatched.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class LogEntry implements Serializable {

    private static final long serialVersionUID = 3044293371048196171L;

    LogEntry() {}

    /**
     * @return The tick in which the clock event was dispatched.
     */
    public abstract Range<Long> getTick();

    /**
     * @return The {@link ClockMode} at the time of this tick.
     */
    public abstract ClockMode getClockMode();

    /**
     * @return The clock event that was dispatched.
     */
    public abstract Enum<?> getClockEvent();

    static LogEntry create(long tickStart, long tickEnd, ClockMode cm,
        Enum<?> ce) {
      return new AutoValue_RealtimeClockLogger_LogEntry(
          Range.closedOpen(tickStart, tickEnd), cm, ce);
    }
  }

  @AutoValue
  static class Builder
      extends AbstractModelBuilder<RealtimeClockLogger, Void>
      implements Serializable {

    private static final long serialVersionUID = -1003703925029199193L;

    Builder() {
      setDependencies(RealtimeClockController.class);
    }

    @Override
    public RealtimeClockLogger build(DependencyProvider dependencyProvider) {
      final RealtimeClockController c =
        dependencyProvider.get(RealtimeClockController.class);
      return new RealtimeClockLogger(c);
    }
  }
}
