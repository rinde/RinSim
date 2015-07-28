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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.central.GlobalStateObject;
import com.github.rinde.rinsim.central.RandomSolver;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.central.rt.RtSolverModel.Mode;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.FakeDependencyProvider;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.testutil.TestUtil;
import com.google.common.collect.Range;

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
    clock = mock(RealtimeClockController.class);
    when(clock.getTimeUnit()).thenReturn(SI.MILLI(SI.SECOND));
    when(clock.getCurrentTime()).thenReturn(0L);

    final PDPRoadModel rm = mock(PDPRoadModel.class);
    when(rm.getSpeedUnit()).thenReturn(NonSI.KILOMETERS_PER_HOUR);
    when(rm.getDistanceUnit()).thenReturn(SI.KILOMETER);

    final PDPModel pm = mock(PDPModel.class);
    dependencyProvider = FakeDependencyProvider.builder()
        .add(clock, RealtimeClockController.class)
        .add(rm, PDPRoadModel.class)
        .add(pm, PDPModel.class)
        .build();

    model = RtSolverModel.builder().build(dependencyProvider);
    assertThat(model.mode).isEqualTo(Mode.UNKNOWN);

    TestUtil.testEnum(Mode.class);
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
   * Tests execution of two solvers at the same time.
   */
  @Test
  public void testMultiExecution() {
    final List<RtSimSolver> solvers = new ArrayList<>();
    model.register(new RtSolverUser() {
      @Override
      public void setSolverProvider(RtSimSolverBuilder builder) {
        solvers.add(
          builder.build(SleepySolver.create(500L, RandomSolver.create(123))));
      }
    });
    model.register(new RtSolverUser() {
      @Override
      public void setSolverProvider(RtSimSolverBuilder builder) {
        solvers.add(
          builder.build(SleepySolver.create(1000L, RandomSolver.create(123))));
      }
    });

    verify(clock, times(0)).switchToRealTime();
    verify(clock, times(0)).switchToSimulatedTime();

    solvers.get(0).solve(SolveArgs.create());
    solvers.get(1).solve(SolveArgs.create());

    verify(clock, times(2)).switchToRealTime();
    verify(clock, times(0)).switchToSimulatedTime();

    final long start = System.nanoTime();
    while (model.manager.isComputing()) {
      verify(clock, times(0)).switchToSimulatedTime();
      try {
        Thread.sleep(10);
      } catch (final InterruptedException e) {
        throw new IllegalStateException(e);
      }
    }
    final double duration = (System.nanoTime() - start) / 1000000000d;
    assertThat(duration).isIn(Range.open(0.9, 1.1));

    verify(clock, times(2)).switchToRealTime();
    verify(clock, times(1)).switchToSimulatedTime();
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
