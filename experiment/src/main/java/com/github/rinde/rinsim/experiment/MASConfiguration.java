/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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

import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem.Creator;
import com.github.rinde.rinsim.scenario.AddDepotEvent;
import com.github.rinde.rinsim.scenario.AddParcelEvent;
import com.github.rinde.rinsim.scenario.AddVehicleEvent;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * This represents a configuration of a multi-agent system in a simulation.
 * Implementations should always be immutable.
 * @author Rinde van Lon 
 */
public interface MASConfiguration {

  /**
   * @return A possibly empty list of model suppliers.
   */
  ImmutableList<? extends StochasticSupplier<? extends Model<?>>> getModels();

  /**
   * @return A creator that creates vehicle agents.
   */
  Creator<AddVehicleEvent> getVehicleCreator();

  /**
   * @return A creator that creates depot agents.
   */
  Optional<? extends Creator<AddDepotEvent>> getDepotCreator();

  /**
   * @return A creator that creates parcel agents.
   */
  Optional<? extends Creator<AddParcelEvent>> getParcelCreator();
}
