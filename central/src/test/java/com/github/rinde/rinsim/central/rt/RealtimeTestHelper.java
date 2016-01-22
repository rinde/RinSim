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
package com.github.rinde.rinsim.central.rt;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.pdptw.common.StatsStopConditions;
import com.github.rinde.rinsim.pdptw.common.StatsTracker;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioController;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;

/**
 *
 * @author Rinde van Lon
 */
public class RealtimeTestHelper {

  static Simulator.Builder init(
      TimedEventHandler<AddVehicleEvent> vehicleHandler,
      Iterable<? extends TimedEvent> events) {

    final Scenario scenario = Scenario.builder()
        .addEvent(AddDepotEvent.create(-1, new Point(5, 5)))
        .addEvent(AddVehicleEvent.create(-1, VehicleDTO.builder().build()))
        .addEvent(AddVehicleEvent.create(-1, VehicleDTO.builder().build()))
        .addEvents(events)
        .build();

    final ScenarioController.Builder sb = ScenarioController.builder(scenario)
        .withEventHandler(AddParcelEvent.class, AddParcelEvent.defaultHandler())
        .withEventHandler(AddVehicleEvent.class, vehicleHandler)
        .withEventHandler(AddDepotEvent.class, AddDepotEvent.defaultHandler())
        .withEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
        .withOrStopCondition(StatsStopConditions.vehiclesDoneAndBackAtDepot())
        .withOrStopCondition(StatsStopConditions.timeOutEvent());

    return Simulator.builder()
        .addModel(PDPRoadModel.builder(RoadModelBuilders.plane())
            .withAllowVehicleDiversion(true))
        .addModel(DefaultPDPModel.builder())
        .addModel(TimeModel.builder()
            .withRealTime()
            .withStartInClockMode(ClockMode.SIMULATED)
            .withTickLength(100))
        .addModel(sb)
        .addModel(StatsTracker.builder());
  }

}
