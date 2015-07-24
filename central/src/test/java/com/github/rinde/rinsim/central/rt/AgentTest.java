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
package com.github.rinde.rinsim.central.rt;

import static java.util.Arrays.asList;

import java.util.List;

import org.junit.Before;

import com.github.rinde.rinsim.central.RandomSolver;
import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;

/**
 *
 * @author Rinde van Lon
 */
public class AgentTest {

  @Before
  public void setUp() {

    final List<TimedEvent> events = asList(
      AddParcelEvent.create(
        Parcel.builder(new Point(0, 0), new Point(3, 3))
            .orderAnnounceTime(200)
            .pickupTimeWindow(new TimeWindow(1000, 2000))
            .buildDTO()),
      AddParcelEvent.create(
        Parcel.builder(new Point(0, 0), new Point(3, 3))
            .orderAnnounceTime(1000)
            .pickupTimeWindow(new TimeWindow(60000, 80000))
            .serviceDuration(180000L)
            .buildDTO()),
      TimeOutEvent.create(3000));

    final Simulator.Builder b =
      RealtimeTestHelper.init(Handler.INSTANCE, events);
  }

  enum Handler implements TimedEventHandler<AddVehicleEvent> {
    INSTANCE {

      @Override
      public void handleTimedEvent(AddVehicleEvent event, SimulatorAPI sim) {
        sim.register(new Agent(event.getVehicleDTO()));
      }
    }
  }

  static class Agent extends RouteFollowingVehicle implements RtSolverUser {
    private final Solver solver;
    private Optional<RtSimSolver> simSolver;

    Agent(VehicleDTO dto) {
      super(dto, true);
      solver = RandomSolver.supplier().get(123L);
      simSolver = Optional.absent();
    }

    @Override
    public void setSolverProvider(RtSimSolverBuilder builder) {
      simSolver = Optional.of(builder.build(solver));
    }

  }

}
