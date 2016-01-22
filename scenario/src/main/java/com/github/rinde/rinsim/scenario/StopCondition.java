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

import com.google.common.collect.ImmutableSet;

/**
 * A stop condition is a condition that determines if and when a simulation
 * should be stopped. A stop condition can depend on types provided by
 * {@link com.github.rinde.rinsim.core.model.Model}s in the simulator. The
 * dependent types should be advertised via {@link #getTypes()}. If registration
 * of the {@link StopCondition} to the {@link ScenarioController} is successful
 * it is guaranteed that all advertised types are available via the
 * {@link TypeProvider} in {@link #evaluate(TypeProvider)}.
 * @author Rinde van Lon
 */
public interface StopCondition {

  /**
   * @return A set of types that the stop condition requires in the received
   *         {@link TypeProvider}. If an empty set is returned, there is no
   *         guarantee that there will be types in {@link TypeProvider}.
   */
  ImmutableSet<Class<?>> getTypes();

  /**
   * Evaluates the condition.
   * @param provider Contains all types as advertised by {@link #getTypes()}.
   * @return <code>true</code> to stop the simulation, <code>false</code> to
   *         continue the simulation.
   */
  boolean evaluate(TypeProvider provider);

  /**
   * A provider of types.
   * @author Rinde van Lon
   */
  public interface TypeProvider {

    /**
     * Retrieves an instance of the specified type.
     * @param type The type to check.
     * @param <T> The type.
     * @return An instance of the specified type.
     * @throws IllegalArgumentException If the specified type is not available
     *           in the provider.
     */
    <T> T get(Class<T> type);
  }
}
