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
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Range;

/**
 *
 * @author Rinde van Lon
 */
public final class RealtimeClockLogger extends AbstractModelVoid {

  final List<LogEntry> log;

  RealtimeClockLogger(final RealtimeClockController clock) {
    log = new ArrayList<>();
    clock.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        log.add(LogEntry.create(clock.getCurrentTime(),
            clock.getCurrentTime() + clock.getTickLength(),
            clock.getClockMode(), e.getEventType()));
      }
    }, SWITCH_TO_REAL_TIME, SWITCH_TO_SIM_TIME, STARTED, STOPPED);
  }

  public List<LogEntry> getLog() {
    return Collections.unmodifiableList(log);
  }

  public static Builder builder() {
    return new AutoValue_RealtimeClockLogger_Builder();
  }

  @AutoValue
  public static class Builder
      extends AbstractModelBuilder<RealtimeClockLogger, Void>
      implements Serializable {

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

  @AutoValue
  public abstract static class LogEntry implements Serializable {

    LogEntry() {}

    public abstract Range<Long> getTick();

    public abstract ClockMode getClockMode();

    public abstract Enum<?> getClockEvent();

    static LogEntry create(long tickStart, long tickEnd, ClockMode cm,
        Enum<?> ce) {
      return new AutoValue_RealtimeClockLogger_LogEntry(
          Range.closedOpen(tickStart, tickEnd), cm, ce);
    }
  }
}
