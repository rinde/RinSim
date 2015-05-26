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

import java.io.Serializable;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.RandomVehicle;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;

public class TestMASConfiguration extends DefaultMASConfiguration implements
  Serializable {

  private final String name;

  TestMASConfiguration(String nm) {
    name = nm;
  }

  public static TestMASConfiguration create(String name) {
    return new TestMASConfiguration(name);
  }

  @Override
  public TimedEventHandler<AddVehicleEvent> getVehicleCreator() {
    return new TimedEventHandler<AddVehicleEvent>() {
      @Override
      public void handleTimedEvent(AddVehicleEvent event, SimulatorAPI sim) {
        sim.register(new RandomVehicle(event.getVehicleDTO()));
      }
    };
  }

  @Override
  public String toString() {
    return name;
  }
}
