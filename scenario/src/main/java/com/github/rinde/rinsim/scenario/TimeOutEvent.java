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
package com.github.rinde.rinsim.scenario;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.google.auto.value.AutoValue;

/**
 * {@link TimedEvent} indicating the end of scenario time, e.g. this may be used
 * to indicate the end of a working day.
 * @author Rinde van Lon
 */
@AutoValue
public abstract class TimeOutEvent implements TimedEvent {

  TimeOutEvent() {}

  /**
   * Creates a new {@link TimeOutEvent}.
   * @param time The time.
   * @return A new instance.
   */
  public static TimeOutEvent create(long time) {
    return new AutoValue_TimeOutEvent(time);
  }

  /**
   * A {@link TimedEventHandler} that ignores every {@link TimeOutEvent} it
   * receives.
   * @return The handler.
   */
  public static TimedEventHandler<TimeOutEvent> ignoreHandler() {
    return Handler.INSTANCE;
  }

  enum Handler implements TimedEventHandler<TimeOutEvent> {
    INSTANCE {
      @Override
      public void handleTimedEvent(TimeOutEvent event,
          SimulatorAPI simulator) {}

      @Override
      public String toString() {
        return TimeOutEvent.class.getSimpleName() + ".ignoreHandler()";
      }
    };
  }
}
