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
package com.github.rinde.rinsim.util;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;

import javax.annotation.Nullable;

import com.google.common.base.Objects;

/**
 * A time window is an interval in time. It is defined as a half open interval:
 * [begin, end).
 * @author Rinde van Lon
 */
public final class TimeWindow implements Serializable {
  private static final long serialVersionUID = 7548761538022038612L;

  /**
   * A time window which represents 'always'.
   */
  public static final TimeWindow ALWAYS = new TimeWindow(0, Long.MAX_VALUE);

  /**
   * Begin of the time window (inclusive). Must be a non-negative value.
   */
  public final long begin;

  /**
   * End of the time window (exclusive).
   */
  public final long end;

  /**
   * Create a new time window [begin, end).
   * @param pBegin {@link #begin}.
   * @param pEnd {@link #end}.
   */
  public TimeWindow(long pBegin, long pEnd) {
    checkArgument(pBegin >= 0, "Time must be non-negative.");
    checkArgument(pBegin <= pEnd, "Begin can not be later than end.");
    begin = pBegin;
    end = pEnd;
  }

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
    return time >= begin;
  }

  /**
   * @param time The time to check.
   * @return <code>true</code> when <code>time &lt; {@link #end}</code>,
   *         <code>false</code> otherwise.
   */
  public boolean isBeforeEnd(long time) {
    return time < end;
  }

  /**
   * @param time The time to check.
   * @return <code>true</code> when <code>time &lt; {@link #begin}</code>,
   *         <code>false</code> otherwise.
   */
  public boolean isBeforeStart(long time) {
    return time < begin;
  }

  /**
   * @param time The time to check.
   * @return <code>true</code> when <code>time &gt;= {@link #end}</code>,
   *         <code>false</code> otherwise.
   */
  public boolean isAfterEnd(long time) {
    return time >= end;
  }

  /**
   * @return The length of the time window:
   *         <code>{@link #end} - {@link #begin}</code>.
   */
  public long length() {
    return end - begin;
  }

  @Override
  public String toString() {
    return toStringHelper(this).add("begin", begin).add("end", end)
        .toString();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == null) {
      return false;
    }
    if (this == other) {
      return true;
    }
    if (!(other instanceof TimeWindow)) {
      return false;
    }
    final TimeWindow otherTW = (TimeWindow) other;
    return equal(begin, otherTW.begin)
        && equal(end, otherTW.end);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(begin, end);
  }
}
