/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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

import java.util.ArrayList;
import java.util.List;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.time.Clock.ClockEventType;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Range;

/**
 *
 * @author Rinde van Lon
 */
public final class TimeUtil {

  TimeUtil() {}

  public static List<Double> interArrivalTimes(Iterable<Long> timeStamps) {
    final PeekingIterator<Long> it =
      Iterators.peekingIterator(timeStamps.iterator());
    final List<Double> interArrivalTimes = new ArrayList<>();
    for (long l1 = it.next(); it.hasNext(); l1 = it.next()) {
      final long l2 = it.peek();
      interArrivalTimes.add((l2 - l1) / 1000000d);
    }
    return interArrivalTimes;
  }

  public static ModelBuilder<?, ?> timeTracker() {
    return TimeTracker.builder();
  }

  public static class TimeTracker
      extends AbstractModelVoid
      implements TickListener {
    final RealtimeClockController clock;
    final List<Long> beforeTimeStamps;
    final List<Long> afterTimeStamps;
    final List<Range<Long>> simTimeLapses;
    final List<ClockMode> clockModes;

    TimeTracker(RealtimeClockController c, boolean debugPrinting) {
      clock = c;
      beforeTimeStamps = new ArrayList<>();
      afterTimeStamps = new ArrayList<>();
      simTimeLapses = new ArrayList<>();
      clockModes = new ArrayList<>();

      if (debugPrinting) {
        clock.getEventAPI().addListener(new Listener() {
          @Override
          public void handleEvent(Event e) {
            final List<Double> beforeDurations =
              interArrivalTimes(beforeTimeStamps);
            final List<Double> afterDurations =
              interArrivalTimes(afterTimeStamps);

            for (int i = 0; i < simTimeLapses.size(); i++) {
              if (i > 0) {
                System.out.printf("\t %1.3f \t %1.3f\n",
                  beforeDurations.get(i - 1),
                  afterDurations.get(i - 1));
              }
              System.out.println(simTimeLapses.get(i).lowerEndpoint() + "\t\t\t"
                + clockModes.get(i));

            }
          }
        }, ClockEventType.STOPPED);
      }
    }

    /**
     * @return the beforeTimeStamps
     */
    public List<Long> getBeforeTimeStamps() {
      return beforeTimeStamps;
    }

    /**
     * @return the afterTimeStamps
     */
    public List<Long> getAfterTimeStamps() {
      return afterTimeStamps;
    }

    /**
     * @return the simTimeLapses
     */
    public List<Range<Long>> getSimTimeLapses() {
      return simTimeLapses;
    }

    /**
     * @return the clockModes
     */
    public List<ClockMode> getClockModes() {
      return clockModes;
    }

    @Override
    public void tick(TimeLapse timeLapse) {
      beforeTimeStamps.add(System.nanoTime());
      simTimeLapses.add(
        Range.closedOpen(timeLapse.getStartTime(), timeLapse.getEndTime()));
      clockModes.add(clock.getClockMode());
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
      afterTimeStamps.add(System.nanoTime());
    }

    static Builder builder() {
      return new AutoValue_TimeUtil_TimeTracker_Builder(false);
    }

    @AutoValue
    public abstract static class Builder
        extends AbstractModelBuilder<TimeTracker, Void> {
      Builder() {
        setDependencies(RealtimeClockController.class);
      }

      abstract boolean debugPrinting();

      public Builder setDebugPrinting(boolean flag) {
        return new AutoValue_TimeUtil_TimeTracker_Builder(flag);
      }

      @Override
      public TimeTracker build(DependencyProvider dependencyProvider) {
        final RealtimeClockController clock =
          dependencyProvider.get(RealtimeClockController.class);
        return new TimeTracker(clock, debugPrinting());
      }
    }
  }
}
