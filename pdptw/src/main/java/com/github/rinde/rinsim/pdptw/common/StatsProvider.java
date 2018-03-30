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

import com.github.rinde.rinsim.event.EventAPI;

/**
 * A provider of {@link StatisticsDTO} instances.
 * @author Rinde van Lon
 */
public interface StatsProvider {

  /**
   * @return The current {@link StatisticsDTO} instance.
   */
  StatisticsDTO getStatistics();

  /**
   * @return The {@link EventAPI}, see {@link StatsEvent}.
   */
  EventAPI getEventAPI();

  /**
   * Event types.
   * @author Rinde van Lon
   */
  enum EventTypes {
    /**
     * Indicates that there has been a pickup with tardiness &gt; 0.
     */
    PICKUP_TARDINESS,

    /**
     * Indicates that there has been a delivery with tardiness &gt; 0.
     */
    DELIVERY_TARDINESS,

    /**
     * Indicates that all vehicles have arrived at the depot.
     */
    ALL_VEHICLES_AT_DEPOT;
  }

}
