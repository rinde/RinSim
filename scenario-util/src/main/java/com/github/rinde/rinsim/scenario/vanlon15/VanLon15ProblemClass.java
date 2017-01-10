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
package com.github.rinde.rinsim.scenario.vanlon15;

import java.util.Locale;

import com.github.rinde.rinsim.scenario.Scenario.ProblemClass;
import com.google.auto.value.AutoValue;

/**
 * This {@link ProblemClass} is characterized by three properties: dynamism,
 * urgency and scale. This problem class was first used in the dataset created
 * in [1].
 * <p>
 * <b>References</b>
 * <ol>
 * <li>Rinde R.S. van Lon and Tom Holvoet.<i>Towards systematic evaluation of
 * multi-agent systems in large scale and dynamic logistics</i>. PRIMA 2015:
 * Principles and Practice of Multi-Agent Systems, (2015).</li>
 * </ol>
 * @author Rinde van Lon
 */
@AutoValue
public abstract class VanLon15ProblemClass implements ProblemClass {

  VanLon15ProblemClass() {}

  /**
   * @return The dynamism of the scenario.
   */
  public abstract double getDynamism();

  /**
   * @return The urgency of the scenario.
   */
  public abstract long getUrgency();

  /**
   * @return The scale of the scenario.
   */
  public abstract double getScale();

  @Override
  public String getId() {
    return String.format(Locale.US, "%1.2f-%d-%1.2f",
      getDynamism(),
      getUrgency(),
      getScale());
  }

  /**
   * Create a new {@link VanLon15ProblemClass} instance with the specified
   * properties.
   * @param dyn The dynamism of the scenario.
   * @param urg The urgency of the scenario.
   * @param scl The scale of the scenario.
   * @return A new instance.
   */
  public static VanLon15ProblemClass create(double dyn, long urg, double scl) {
    return new AutoValue_VanLon15ProblemClass(dyn, urg, scl);
  }
}
