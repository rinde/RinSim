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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.rinde.rinsim.central.RandomSolver;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.testutil.RealtimeTests;
import com.google.common.collect.Range;

/**
 * Time sensitive tests.
 * @author Rinde van Lon
 */
@Category(RealtimeTests.class)
public class RtSolverModelTestRT extends RtSolverModelTest {

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

}
