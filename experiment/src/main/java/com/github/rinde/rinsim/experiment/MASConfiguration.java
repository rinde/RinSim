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
package com.github.rinde.rinsim.experiment;

import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * This represents a configuration of a multi-agent system in a simulation.
 * Implementations should always be immutable.
 * @author Rinde van Lon
 */
public interface MASConfiguration {

  /**
   * @return A possibly empty list of model builders.
   */
  ImmutableList<? extends ModelBuilder<?, ?>> getModels();

  /**
   * @return A creator that creates vehicle agents.
   */
  TimedEventHandler<AddVehicleEvent> getVehicleCreator();

  /**
   * @return A creator that creates depot agents.
   */
  Optional<? extends TimedEventHandler<AddDepotEvent>> getDepotCreator();

  /**
   * @return A creator that creates parcel agents.
   */
  Optional<? extends TimedEventHandler<AddParcelEvent>> getParcelCreator();
}
