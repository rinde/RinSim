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

import static com.github.rinde.rinsim.core.model.time.RealtimeClockController.RtClockEventType.SWITCH_TO_REAL_TIME;
import static com.github.rinde.rinsim.core.model.time.RealtimeClockController.RtClockEventType.SWITCH_TO_SIM_TIME;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.central.RandomSolver;
import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.RtClockEventType;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

/**
 *
 * @author Rinde van Lon
 */
public class AgentTest {

  @SuppressWarnings("null")
  Simulator sim;

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
            .orderAnnounceTime(999)
            .pickupTimeWindow(new TimeWindow(60000, 80000))
            .serviceDuration(180000L)
            .buildDTO()),
      TimeOutEvent.create(3000));

    sim = RealtimeTestHelper.init(Handler.INSTANCE, events)
        .addModel(RtSolverModel.builder())
        .build();

  }

  @Test
  public void test() {
    final RealtimeClockController clock =
      (RealtimeClockController) sim.getModelProvider()
          .getModel(TimeModel.class);

    final List<LogEntry> log = new ArrayList<>();
    clock.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        log.add(logEntry(clock.getCurrentTime(), e.getEventType()));
      }
    }, RtClockEventType.values());

    sim.start();
    assertThat(log.get(0)).isEqualTo(logEntry(0, SWITCH_TO_SIM_TIME));
    assertThat(log.get(1)).isEqualTo(logEntry(200, SWITCH_TO_REAL_TIME));
    assertThat(log.get(2).eventType()).isEqualTo(SWITCH_TO_SIM_TIME);
    assertThat(log.get(2).time()).isIn(ImmutableSet.of(300L, 400L));
    assertThat(log.get(3)).isEqualTo(logEntry(1000, SWITCH_TO_REAL_TIME));
    assertThat(log.get(4).eventType()).isEqualTo(SWITCH_TO_SIM_TIME);
    assertThat(log.get(4).time()).isIn(ImmutableSet.of(1100L, 1200L));
    assertThat(log.get(5)).isEqualTo(logEntry(3000, SWITCH_TO_REAL_TIME));
  }

  enum Handler implements TimedEventHandler<AddVehicleEvent> {
    INSTANCE {
      @Override
      public void handleTimedEvent(AddVehicleEvent event, SimulatorAPI sim) {
        sim.register(new Agent(event.getVehicleDTO()));
      }
    }
  }

  static LogEntry logEntry(long time, Enum<?> type) {
    return new AutoValue_AgentTest_LogEntry(time, type);
  }

  @AutoValue
  abstract static class LogEntry {
    abstract long time();

    abstract Enum<?> eventType();
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
      simSolver =
        Optional.of(builder.setVehicles(ImmutableSet.of(this)).build(solver));
      final RtSimSolver s = simSolver.get();
      s.getEventAPI().addListener(new Listener() {
        @Override
        public void handleEvent(Event e) {
          assertThat(s.isScheduleUpdated()).isTrue();
          assertThat(s.getCurrentSchedule()).hasSize(1);
          assertThat(s.isScheduleUpdated()).isFalse();
        }
      }, RtSimSolver.EventType.NEW_SCHEDULE);
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
      super.initRoadPDP(pRoadModel, pPdpModel);
      getPDPModel().getEventAPI().addListener(new Listener() {
        @Override
        public void handleEvent(Event e) {
          simSolver.get().solve(SolveArgs.create());
        }
      }, PDPModelEventType.NEW_PARCEL);
    }
  }
}
