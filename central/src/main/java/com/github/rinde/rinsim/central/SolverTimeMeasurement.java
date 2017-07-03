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
package com.github.rinde.rinsim.central;

import com.google.auto.value.AutoValue;

/**
 * Value object of the time it took for a particular solver to compute a
 * solution for a problem specified by a {@link GlobalStateObject}.
 * @author Rinde van Lon
 */
@AutoValue
public abstract class SolverTimeMeasurement {
  /**
   * @return The input {@link GlobalStateObject} that specifies the problem for
   *         which a solution is computed.
   */
  public abstract GlobalStateObject input();

  /**
   * @return The duration (wall clock time) in nanoseconds it took to compute a
   *         solution.
   */
  public abstract long durationNs();

  /**
   * Create a new instance.
   * @param input The input problem as given to the solver.
   * @param dur The duration it took to find a solution for the input.
   * @return A new instance.
   */
  public static SolverTimeMeasurement create(GlobalStateObject input,
      long dur) {
    return new AutoValue_SolverTimeMeasurement(input, dur);
  }
}
