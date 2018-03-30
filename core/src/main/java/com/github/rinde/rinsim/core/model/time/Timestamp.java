/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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

import java.io.Serializable;

import com.google.auto.value.AutoValue;

/**
 * A timestamp marks the end of one {@link TimeLapse} and the start of another.
 * @author Rinde van Lon
 */
@AutoValue
public abstract class Timestamp implements Serializable {
  private static final long serialVersionUID = 4952394924947361518L;

  Timestamp() {}

  /**
   * The tick count. If tick count is <code>n</code> it means that this is the
   * n-th {@link TimeLapse} in the current real-time period of the
   * {@link RealtimeClockController}. Each time a new real-time period is
   * started the counter is reset to <code>0</code>.
   * @return The tick count, <code>&ge; 0</code>
   */
  public abstract long getTickCount();

  /**
   * @return The value of {@link System#currentTimeMillis()} at the start of the
   *         tick.
   */
  public abstract long getMillis();

  /**
   * @return The value of {@link System#nanoTime()} at the start of the tick.
   */
  public abstract long getNanos();

  static Timestamp now(long tickCount) {
    return create(tickCount, System.currentTimeMillis(), System.nanoTime());
  }

  static Timestamp create(long tickCount, long millis, long nanos) {
    return new AutoValue_Timestamp(tickCount, millis, nanos);
  }
}
