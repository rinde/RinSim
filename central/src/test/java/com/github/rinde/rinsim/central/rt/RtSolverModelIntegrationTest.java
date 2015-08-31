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

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.central.GlobalStateObject;
import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.google.common.collect.ImmutableList;

import net.openhft.affinity.AffinityLock;

@RunWith(Parameterized.class)
public class RtSolverModelIntegrationTest {

  @SuppressWarnings("null")
  RtSolverModel model;

  @SuppressWarnings("null")
  RealtimeClockController rtClock;

  @SuppressWarnings("null")
  Simulator sim;

  final boolean useThreadGrouping;

  static final Logger LOGGER =
    LoggerFactory.getLogger(RtSolverModelIntegrationTest.class);

  public RtSolverModelIntegrationTest(boolean threadGrouping) {
    useThreadGrouping = threadGrouping;
  }

  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {true}, {false}
    });
  }

  @Before
  public void setUp() {
    sim = Simulator.builder()
        .addModel(TimeModel.builder()
            .withRealTime()
            .withTickLength(100L)
            .withTimeUnit(SI.MILLI(SI.SECOND)))
        .addModel(PDPRoadModel.builder(RoadModelBuilders.plane()))
        .addModel(DefaultPDPModel.builder())
        .addModel(RtSolverModel.builder()
            .withSingleMode()
            .withThreadGrouping(useThreadGrouping))
        .build();

    model = sim.getModelProvider().getModel(RtSolverModel.class);

    rtClock = sim.getModelProvider().getModel(TimeModel.class)
        .get(RealtimeClockController.class);
    sim.register(new Depot(new Point(5, 5)));
    sim.register(new RouteFollowingVehicle(VehicleDTO.builder().build(), true));
  }

  @Test
  public void test() {
    final RtSimSolverBuilder builder = model.get(RtSimSolverBuilder.class);

    final RtSimSolver simSolver = builder.build(new Solver() {
      @Override
      public ImmutableList<ImmutableList<Parcel>> solve(
          GlobalStateObject state) {

        // quick! act busy!

        // 3 seconds
        final long waitTime = 3000000000L;
        final long start = System.nanoTime();
        LOGGER.trace("start computing");
        while (System.nanoTime() - start < waitTime) {
          if (Thread.interrupted()) {
            Thread.currentThread().interrupt();
            LOGGER.trace("interrupt");
            return ImmutableList.of();
          }
          final List<Integer> list = new ArrayList<>();
          for (int i = 0; i < 100; i++) {
            list.add(new Integer(i));
          }
        }

        final ImmutableList<ImmutableList<Parcel>> result =
          ImmutableList.of(state.getAvailableParcels().asList());
        LOGGER.trace("done computing: {}", result);
        return result;
      }
    });

    sim.addTickListener(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        LOGGER.trace("{} tick", timeLapse);
        if (timeLapse.getTime() == 200) {
          rtClock.switchToRealTime();
        } else if (timeLapse.getTime() == 300) {
          sim.register(
            Parcel.builder(new Point(0, 0), new Point(4, 4)).build());

          simSolver.solve(SolveArgs.create());
          assertNoSchedule(simSolver);
        } else if (timeLapse.getTime() == 400) {
          LOGGER.trace(AffinityLock.dumpLocks());
        } else if (timeLapse.getTime() == 1000) {
          sim.register(
            Parcel.builder(new Point(0, 0), new Point(4, 4)).build());
          simSolver.solve(SolveArgs.create());
          assertNoSchedule(simSolver);
          assertThat(simSolver.isScheduleUpdated()).isFalse();
        } else if (timeLapse.getTime() == 6000) {
          assertThat(simSolver.isScheduleUpdated()).isTrue();

          final ImmutableList<ImmutableList<Parcel>> result =
            simSolver.getCurrentSchedule();
          assertThat(result).hasSize(1);
          assertThat(result.get(0)).hasSize(2);
          assertThat(simSolver.isScheduleUpdated()).isFalse();
          sim.stop();
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });

    sim.start();
    assertThat(sim.getCurrentTime()).isEqualTo(6100);
  }

  void assertNoSchedule(RtSimSolver ss) {
    boolean fail = false;
    try {
      ss.getCurrentSchedule();
    } catch (final IllegalStateException e) {
      assertThat(e.getMessage()).contains("No schedule");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

}
