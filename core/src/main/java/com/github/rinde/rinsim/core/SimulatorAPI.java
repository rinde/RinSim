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
package com.github.rinde.rinsim.core;

import javax.measure.quantity.Duration;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.rand.RandomModel;
import com.github.rinde.rinsim.core.model.rand.RandomUser;
import com.github.rinde.rinsim.event.EventAPI;

/**
 * Limited simulator API that provides an API for simulation elements (e.g.,
 * agents).
 * @author Bartosz Michalik
 * @author Rinde van Lon
 * @since 2.0
 *
 */
public interface SimulatorAPI {

  /**
   * Register a given entity in the simulator. During registration the object is
   * provided all features it requires (declared by interfaces) and bound to the
   * required models (if they were registered in the simulator before).
   * @param o object to register
   * @return <code>true</code> when registration of the object in the simulator
   *         was successful
   * @throws IllegalStateException when simulator is not configured.
   */
  boolean register(Object o);

  /**
   * Unregister an object from simulator.
   * @param o The object to be unregistered.
   * @return True if the object could be unregistered, false otherwise.
   */
  boolean unregister(Object o);

  /**
   * Get access to the main random generator used in the simulator.
   * @return the random generator of the simulator
   * @deprecated Use {@link RandomModel} and {@link RandomUser} instead.
   */
  @Deprecated
  RandomGenerator getRandomGenerator();

  /**
   * @return The current simulation time.
   */
  long getCurrentTime();

  /**
   * @return The time step (in simulation time) which is added to current time
   *         at every tick.
   */
  long getTimeStep();

  /**
   * @return The unit of time that is used for generating ticks.
   */
  Unit<Duration> getTimeUnit();

  /**
   * Reference to the {@link EventAPI} of the Simulator. Can be used to add
   * listeners to events dispatched by the simulator. Simulator events are
   * defined in
   * {@link com.github.rinde.rinsim.core.Simulator.SimulatorEventType}.
   * @return {@link EventAPI}
   */
  EventAPI getEventAPI();
}
