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
package com.github.rinde.rinsim.util;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;

import com.google.auto.value.AutoValue;

/**
 * A time window is an interval in time. It is defined as a half open interval:
 * [begin, end).
 * @author Rinde van Lon
 */
@AutoValue
public abstract class TimeWindow implements Serializable {

  private static final long serialVersionUID = -5186749588224283755L;
  private static final TimeWindow ALWAYS = TimeWindow.create(0, Long.MAX_VALUE);

  TimeWindow() {}

  /**
   * @return Begin of the time window (inclusive). Must be a non-negative value.
   */
  public abstract long begin();

  /**
   * @return End of the time window (exclusive).
   */
  public abstract long end();

  /**
   * @param time The time to check.
   * @return <code>true</code> when <code>time</code> is in [{@link #begin},
   *         {@link #end}), <code>false</code> otherwise.
   */
  public boolean isIn(long time) {
    return isAfterStart(time) && isBeforeEnd(time);
  }

  /**
   * @param time The time to check.
   * @return <code>true</code> when <code>time &gt;= {@link #begin}</code>,
   *         <code>false</code> otherwise.
   */
  public boolean isAfterStart(long time) {
    return time >= begin();
  }

  /**
   * @param time The time to check.
   * @return <code>true</code> when <code>time &lt; {@link #end}</code>,
   *         <code>false</code> otherwise.
   */
  public boolean isBeforeEnd(long time) {
    return time < end();
  }

  /**
   * @param time The time to check.
   * @return <code>true</code> when <code>time &lt; {@link #begin}</code>,
   *         <code>false</code> otherwise.
   */
  public boolean isBeforeStart(long time) {
    return time < begin();
  }

  /**
   * @param time The time to check.
   * @return <code>true</code> when <code>time &gt;= {@link #end}</code>,
   *         <code>false</code> otherwise.
   */
  public boolean isAfterEnd(long time) {
    return time >= end();
  }

  /**
   * @return The length of the time window: <code>{@link #end} -
   *         {@link #begin}</code>.
   */
  public long length() {
    return end() - begin();
  }

  /**
   * Create a new time window [begin, end).
   * @param begin {@link #begin()}.
   * @param end {@link #end()}.
   * @return A new time window instance.
   */
  public static TimeWindow create(long begin, long end) {
    checkArgument(begin >= 0, "Time must be non-negative.");
    checkArgument(begin <= end, "Begin (%s) can not be later than end (%s).",
      begin, end);
    return new AutoValue_TimeWindow(begin, end);
  }

  /**
   * @return A time window which represents 'always'.
   */
  public static TimeWindow always() {
    return ALWAYS;
  }
}
