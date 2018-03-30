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
package com.github.rinde.rinsim.pdptw.common;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.event.Event;

/**
 * Event dispatched by a {@link StatsProvider} indicating that some statistic
 * has changed.
 * @author Rinde van Lon
 */
public class StatsEvent extends Event {
  private final Parcel parcel;
  private final Vehicle vehicle;
  private final long tardiness;
  private final long time;

  StatsEvent(Enum<?> type, Object pIssuer, Parcel p, Vehicle v,
      long tar, long tim) {
    super(type, pIssuer);
    parcel = p;
    vehicle = v;
    tardiness = tar;
    time = tim;
  }

  /**
   * @return The parcel which pickup or delivery is tardy.
   */
  public Parcel getParcel() {
    return parcel;
  }

  /**
   * @return The vehicle that caused the tardiness.
   */
  public Vehicle getVehicle() {
    return vehicle;
  }

  /**
   * @return The tardiness.
   */
  public long getTardiness() {
    return tardiness;
  }

  /**
   * @return Current time.
   */
  public long getTime() {
    return time;
  }
}
