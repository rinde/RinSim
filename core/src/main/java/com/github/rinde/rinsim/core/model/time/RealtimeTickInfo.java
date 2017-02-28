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

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;

import com.google.auto.value.AutoValue;

/**
 * A value object containing information about the exact timings of a tick in a
 * real-time simulation. It can be obtained via
 * {@link RealtimeClockLogger#getTickInfoList()}.
 * @author Rinde van Lon
 */
@AutoValue
public abstract class RealtimeTickInfo implements Serializable {
  private static final long serialVersionUID = -5816920529507582235L;

  RealtimeTickInfo() {}

  /**
   * @return The {@link Timestamp} of the start of the tick.
   */
  public abstract Timestamp getStartTimestamp();

  /**
   * @return The {@link Timestamp} of the end of the tick.
   */
  public abstract Timestamp getEndTimestamp();

  /**
   * @return The time (in nanos) between the start and end time of the tick.
   */
  public abstract long getInterArrivalTime();

  static RealtimeTickInfo create(Timestamp start, Timestamp end) {
    checkArgument(start.getTickCount() + 1 == end.getTickCount());
    final long iat = end.getNanos() - start.getNanos();
    return new AutoValue_RealtimeTickInfo(start, end, iat);
  }
}
