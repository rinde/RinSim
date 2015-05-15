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
package com.github.rinde.rinsim.examples.pdptw.gradientfield;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.experiment.DefaultMASConfiguration;
import com.github.rinde.rinsim.scenario.AddParcelEvent;
import com.github.rinde.rinsim.scenario.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 *
 * @author Rinde van Lon
 */
class GradientFieldConfiguration extends DefaultMASConfiguration {
  private static final long serialVersionUID = -3882825964052190638L;

  @Override
  public ImmutableList<? extends ModelBuilder<?, ?>> getModels() {
    return ImmutableList.<ModelBuilder<?, ?>> of(GradientModel.builder());
  }

  @Override
  public TimedEventHandler<AddVehicleEvent> getVehicleCreator() {
    return new TimedEventHandler<AddVehicleEvent>() {
      @Override
      public void handleTimedEvent(AddVehicleEvent event, SimulatorAPI sim) {
        sim.register(new Truck(event.vehicleDTO));
      }
    };
  }

  @Override
  public Optional<? extends TimedEventHandler<AddParcelEvent>> getParcelCreator() {
    return Optional.of(new TimedEventHandler<AddParcelEvent>() {
      @Override
      public void handleTimedEvent(AddParcelEvent event, SimulatorAPI sim) {
        // all parcels are accepted by default
        sim.register(new GFParcel(event.parcelDTO));
      }
    });
  }
}
