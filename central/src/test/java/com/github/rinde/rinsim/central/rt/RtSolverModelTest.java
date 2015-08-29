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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.central.GlobalStateObject;
import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.central.rt.RtSolverModel.Mode;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.FakeDependencyProvider;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController;
import com.github.rinde.rinsim.core.model.time.TimeLapseFactory;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.testutil.TestUtil;
import com.google.common.collect.ImmutableList;

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
        "only provides " + RtSimSolverBuilder.class.getSimpleName());
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
        public void receiveSnapshot(GlobalStateObject snapshot) {
          throw new IllegalArgumentException("This is a test");
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
    public void receiveSnapshot(GlobalStateObject snapshot) {}

  }

}
