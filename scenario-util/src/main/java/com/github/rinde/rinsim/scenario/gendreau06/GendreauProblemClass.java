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
package com.github.rinde.rinsim.scenario.gendreau06;

import com.github.rinde.rinsim.scenario.Scenario.ProblemClass;

/**
 * This is an enum containing the three problem classes used in the Gendreau06
 * benchmark.
 * @author Rinde van Lon
 */
public enum GendreauProblemClass implements ProblemClass {
  /**
   * Length of day: 240 minutes, frequency: 24 requests per hour, vehicles: 10.
   */
  SHORT_LOW_FREQ(240L, 24L, 10),

  /**
   * Length of day: 240 minutes, frequency: 33 requests per hour, vehicles: 10.
   */
  SHORT_HIGH_FREQ(240L, 33L, 10),

  /**
   * Length of day: 450 minutes, frequency: 24 requests per hour, vehicles: 20.
   */
  LONG_LOW_FREQ(450L, 24L, 20);

  /**
   * The (postfix) file identifier of this class. This can be used to filter for
   * this problem class in a set of files.
   */
  public final String fileId;

  /**
   * The length of the day in minutes.
   */
  public final long duration;

  /**
   * The frequency of new incoming requests in requests per hour.
   */
  public final long frequency;

  /**
   * The total number of vehicles available during the entire day.
   */
  public final int vehicles;

  GendreauProblemClass(long d, long f, int v) {
    duration = d;
    frequency = f;
    vehicles = v;
    fileId = String.format("_%d_%d", duration, frequency);
  }

  @Override
  public String getId() {
    return fileId;
  }

  /**
   * Look up the problem class instance based on the two identifying parameters.
   * @param minutes The length of the scenario in minutes.
   * @param frequency The order frequency.
   * @return The problem class if it exists, throws a
   *         {@link IllegalArgumentException} otherwise.
   */
  public static GendreauProblemClass with(long minutes, long frequency) {
    for (final GendreauProblemClass gpc : values()) {
      if (gpc.duration == minutes && gpc.frequency == frequency) {
        return gpc;
      }
    }
    throw new IllegalArgumentException(
      "There is no problem class with: minutes: " + minutes + ", frequency: "
        + frequency);
  }
}
