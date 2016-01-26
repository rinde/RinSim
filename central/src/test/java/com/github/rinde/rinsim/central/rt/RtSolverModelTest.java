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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.central.GlobalStateObject;
import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.central.rt.RtSimSolver.EventType;
import com.github.rinde.rinsim.central.rt.RtSolverModel.Mode;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.FakeDependencyProvider;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeLapseFactory;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.testutil.TestUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import autovalue.shaded.com.google.common.common.base.Optional;

/**
 * Test for {@link RtSolverModel}.
 * @author Rinde van Lon
 */
public class RtSolverModelTest {

  @SuppressWarnings("null")
  RtSolverModel model;

  @SuppressWarnings("null")
  DependencyProvider dependencyProvider;

  @SuppressWarnings("null")
  RealtimeClockController clock;

  /**
   * Sets up a fake dependency provider and a working model instance.
   */
  @Before
  public void setUp() {
    clock = spy((RealtimeClockController) TimeModel.builder()
        .withRealTime()
        .build(FakeDependencyProvider.empty()));
    doNothing().when(clock).switchToRealTime();
    doNothing().when(clock).switchToSimulatedTime();

    final PDPRoadModel rm = mock(PDPRoadModel.class);
    when(rm.getSpeedUnit()).thenReturn(NonSI.KILOMETERS_PER_HOUR);
    when(rm.getDistanceUnit()).thenReturn(SI.KILOMETER);

    dependencyProvider = FakeDependencyProvider.builder()
        .add(clock, RealtimeClockController.class)
        .add(rm, PDPRoadModel.class)
        .add(DefaultPDPModel.builder())
        .build();

    model = RtSolverModel.builder().build(dependencyProvider);
    ((TimeModel) clock).register(model);
    assertThat(model.mode).isEqualTo(Mode.UNKNOWN);

    TestUtil.testEnum(Mode.class);
    TestUtil.testEnum(RtSimSolver.EventType.class);
  }

  /**
   * Tests getting multiple builders in multi mode.
   */
  @Test
  public void testRegisterMulti() {
    final FakeRealtimeSolver frs1 = new FakeRealtimeSolver();
    final FakeRealtimeSolver frs2 = new FakeRealtimeSolver();
    final FakeRealtimeSolver frs3 = new FakeRealtimeSolver();

    assertThat(model.mode).isEqualTo(Mode.UNKNOWN);
    model.register(new RtSolverUser() {
      @Override
      public void setSolverProvider(RtSimSolverBuilder builder) {
        builder.build(frs1);
      }
    });
    assertThat(model.mode).isEqualTo(Mode.MULTI_MODE);

    model.register(new RtSolverUser() {
      @Override
      public void setSolverProvider(RtSimSolverBuilder builder) {
        builder.build(frs2);
      }
    });
    final RtSimSolverBuilder b = model.get(RtSimSolverBuilder.class);
    b.build(frs3);
    assertThat(model.manager.simSolvers).hasSize(3);

    final Scheduler s1 = frs1.scheduler.get();
    final Scheduler s2 = frs2.scheduler.get();
    final Scheduler s3 = frs3.scheduler.get();

    assertThat(s1).isNotEqualTo(s2);
    assertThat(s1).isNotEqualTo(s3);
    assertThat(s2).isNotEqualTo(s3);

    assertThat(s1.getSharedExecutor()).isSameAs(s2.getSharedExecutor());
    assertThat(s1.getSharedExecutor()).isSameAs(s3.getSharedExecutor());

    boolean fail = false;
    try {
      s1.getCurrentSchedule();
    } catch (final IllegalStateException e) {
      assertThat(e.getMessage()).contains("No schedule has been set");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  /**
   * Tests getting builders in single mode.
   */
  @Test
  public void testRegisterSingle() {
    assertThat(model.mode).isEqualTo(Mode.UNKNOWN);
    final RtSimSolverBuilder b = model.get(RtSimSolverBuilder.class);
    assertThat(model.mode).isEqualTo(Mode.SINGLE_MODE);
    b.build(mock(RealtimeSolver.class));

    assertThat(model.manager.simSolvers).hasSize(1);

    final RtSimSolverBuilder b2 = model.get(RtSimSolverBuilder.class);
    boolean fail = false;
    try {
      b2.build(mock(RealtimeSolver.class));
    } catch (final IllegalStateException e) {
      assertThat(e.getMessage()).contains("can build only one");
      fail = true;
    }
    assertThat(fail).isTrue();
    assertThat(model.manager.simSolvers).hasSize(1);
    fail = false;

    try {
      model.register(new RtSolverUser() {
        @Override
        public void setSolverProvider(RtSimSolverBuilder builder) {
          builder.build(mock(RealtimeSolver.class));
        }
      });
    } catch (final IllegalStateException e) {
      assertThat(e.getMessage()).contains("is in single mode");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  /**
   * Tests that unsupported object request fails.
   */
  @Test
  public void testGetFail() {
    boolean fail = false;
    try {
      model.get(Object.class);
    } catch (final IllegalArgumentException e) {
      fail = true;
      assertThat(e.getMessage()).contains(
        " does not provide " + Object.class.toString());
    }
    assertThat(fail).isTrue();
  }

  /**
   * Tests that exception thrown in a client thread is catched and rethrown in
   * main thread.
   */
  @Test
  public void testExceptionPropagation() {
    final RtSimSolver rss =
      model.get(RtSimSolverBuilder.class).build(new RealtimeSolver() {
        @Override
        public void init(Scheduler scheduler) {}

        @Override
        public void problemChanged(GlobalStateObject snapshot) {
          throw new IllegalArgumentException("This is a test");
        }

        @Override
        public void receiveSnapshot(GlobalStateObject snapshot) {
          throw new UnsupportedOperationException();
        }

        @Override
        public void cancel() {}

        @Override
        public boolean isComputing() {
          return false;
        }
      });

    rss.solve(SolveArgs.create());

    boolean fail = false;
    try {
      while (true) {
        // loop is needed because the solver may be executed at a slightly later
        // time
        model.tick(TimeLapseFactory.create(0, 1000));
      }
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("This is a test");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  /**
   * Test exception propagation.
   */
  @Test
  public void testExceptionPropagation2() {
    final RtSimSolver rss =
      model.get(RtSimSolverBuilder.class).build(new Solver() {
        @Override
        public ImmutableList<ImmutableList<Parcel>> solve(
            GlobalStateObject state) {
          throw new IllegalArgumentException("This is a test");
        }
      });
    rss.solve(SolveArgs.create());
    boolean fail = false;
    try {
      // loop is needed because the solver may be executed at a slightly later
      // time
      while (true) {
        model.tick(TimeLapseFactory.create(0, 1000));
      }
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("This is a test");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  /**
   * Tests that the builder options are set correctly.
   */
  @Test
  public void testBuilder() {
    final RtSolverModel m = RtSolverModel.builder()
        .withMultiMode()
        .build(dependencyProvider);
    assertThat(m.mode).isEqualTo(Mode.MULTI_MODE);

    final RtSolverModel m2 = RtSolverModel.builder()
        .withSingleMode()
        .build(dependencyProvider);
    assertThat(m2.mode).isEqualTo(Mode.SINGLE_MODE);
  }

  /**
   * Test that two consecutive invocation of the same sim solver is handled
   * correctly. The correct behavior is that the calculation of the first
   * invocation is cancelled.
   */
  @Test
  public void testConcurrentSolvers() {
    final RtSimSolverBuilder b = model.get(RtSimSolverBuilder.class);
    final RtSimSolver simSolver = b.build(SolverToRealtimeAdapter
        .create(SleepySolver.create(500, new NopSolver())));
    final Parcel p = Parcel.builder(new Point(0, 0), new Point(1, 1))
        .build();

    final List<Event> events = new ArrayList<>();
    simSolver.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        events.add(e);
      }
    });

    ((TimeModel) clock).register(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        if (timeLapse.getStartTime() == 0L) {
          simSolver.solve(SolveArgs.create().useParcels(ImmutableSet.of(p)));
          simSolver.solve(SolveArgs.create().useParcels(ImmutableSet.of(p)));
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {
        if (timeLapse.getStartTime() == 3000L) {
          clock.stop();
        }
      }
    });
    clock.start();

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getEventType()).isEqualTo(EventType.NEW_SCHEDULE);
  }

  static class NopRtSolver implements RealtimeSolver {

    public NopRtSolver() {}

    @Override
    public void init(Scheduler scheduler) {}

    @Override
    public void receiveSnapshot(GlobalStateObject snapshot) {}

    @Override
    public void problemChanged(GlobalStateObject snapshot) {}

    @Override
    public void cancel() {}

    @Override
    public boolean isComputing() {
      return false;
    }

  }

  static class NopSolver implements Solver {

    @Override
    public ImmutableList<ImmutableList<Parcel>> solve(GlobalStateObject state)
        throws InterruptedException {

      final ImmutableList.Builder<ImmutableList<Parcel>> b =
        ImmutableList.builder();
      for (int i = 0; i < state.getVehicles().size(); i++) {
        b.add(ImmutableList.<Parcel>of());
      }
      return b.build();
    }
  }

  static class FakeRealtimeSolver implements RealtimeSolver {

    Optional<Scheduler> scheduler;

    FakeRealtimeSolver() {
      scheduler = Optional.absent();
    }

    @Override
    public void init(Scheduler s) {
      checkState(!scheduler.isPresent());
      scheduler = Optional.of(s);
    }

    @Override
    public void problemChanged(GlobalStateObject snapshot) {}

    @Override
    public void receiveSnapshot(GlobalStateObject snapshot) {}

    @Override
    public void cancel() {}

    @Override
    public boolean isComputing() {
      return false;
    }

  }

}
