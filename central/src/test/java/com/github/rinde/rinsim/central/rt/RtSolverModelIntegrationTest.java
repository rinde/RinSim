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

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
import com.github.rinde.rinsim.testutil.RealtimeTests;
import com.google.common.collect.ImmutableList;

import net.openhft.affinity.AffinityLock;

@RunWith(Parameterized.class)
@Category(RealtimeTests.class)
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

  /**
   * Tests correct interruption of a running solver.
   */
  @Test
  public void testSolverInterruption() {
    final RtSimSolverBuilder builder = model.get(RtSimSolverBuilder.class);
    final RtSimSolver simSolver =
      builder.build(new InterruptibleBusySolver(3L));

    sim.addTickListener(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        LOGGER.trace("{} tick", timeLapse);
        if (timeLapse.getTime() == 200) {
          rtClock.switchToRealTime();
        } else if (timeLapse.getTime() == 300) {
          sim.register(
            Parcel.builder(new Point(0, 0), new Point(4, 4)).build());

          simSolver.solve(SolveArgs.create()
              .useCurrentRoutes(ImmutableList.of(ImmutableList.<Parcel>of())));
          assertNoSchedule(simSolver);
        } else if (timeLapse.getTime() == 400) {
          LOGGER.trace(AffinityLock.dumpLocks());
        } else if (timeLapse.getTime() == 1000) {
          sim.register(
            Parcel.builder(new Point(0, 0), new Point(4, 4)).build());
          simSolver.solve(SolveArgs.create()
              .useCurrentRoutes(ImmutableList.of(ImmutableList.<Parcel>of())));
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

  /**
   * Test that an exception is correctly propagated to outside.
   */
  @Test
  public void testMisbehavingTickListener() {
    final RtSimSolverBuilder builder = model.get(RtSimSolverBuilder.class);
    final RtSimSolver simSolver =
      builder.build(new InterruptibleBusySolver(3L));

    sim.addTickListener(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        if (timeLapse.getTime() == 100) {
          rtClock.switchToRealTime();
        } else if (timeLapse.getTime() == 200) {
          sim.register(
            Parcel.builder(new Point(0, 0), new Point(4, 4)).build());
          simSolver.solve(SolveArgs.create()
              .useCurrentRoutes(ImmutableList.of(ImmutableList.<Parcel>of())));
        } else if (timeLapse.getTime() == 500) {
          throw new IllegalStateException("This is a test");
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });
    boolean fail = false;
    try {
      sim.start();
    } catch (final IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("This is a test");
      fail = true;
    }
    assertThat(fail).isTrue();
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

  static class InterruptibleBusySolver implements Solver {
    static final Logger LOGGER2 =
      LoggerFactory.getLogger(InterruptibleBusySolver.class);
    final long busyNs;

    InterruptibleBusySolver(long busySeconds) {
      busyNs = 1000000000L * busySeconds;
    }

    @Override
    public ImmutableList<ImmutableList<Parcel>> solve(
        GlobalStateObject state) throws InterruptedException {
      // quick! act busy!

      final long start = System.nanoTime();
      LOGGER2.trace("start computing");
      while (System.nanoTime() - start < busyNs) {
        if (Thread.interrupted()) {
          Thread.currentThread().interrupt();
          LOGGER2.trace("interrupt");
          throw new InterruptedException();
        }
        final List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
          list.add(new Integer(i));
        }
      }

      final ImmutableList<ImmutableList<Parcel>> result =
        ImmutableList.of(state.getAvailableParcels().asList());
      LOGGER2.trace("done computing: {}", result);
      return result;
    }
  }

}
