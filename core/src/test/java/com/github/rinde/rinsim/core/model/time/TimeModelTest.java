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
package com.github.rinde.rinsim.core.model.time;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.rinde.rinsim.core.model.FakeDependencyProvider;
import com.github.rinde.rinsim.core.model.time.Clock.ClockEventType;
import com.github.rinde.rinsim.core.model.time.RealTimeModel.PriorityThreadFactory;
import com.github.rinde.rinsim.core.model.time.TimeModel.TMFactories;
import com.github.rinde.rinsim.testutil.TestUtil;

/**
 * @author Rinde van Lon
 * @param <T>
 */
@RunWith(Parameterized.class)
public abstract class TimeModelTest<T extends TimeModel> {
  final TimeModel.Builder builder;
  private T model;

  /**
   * @param sup The supplier to use for creating model instances.
   */
  @SuppressWarnings("null")
  public TimeModelTest(TimeModel.Builder sup) {
    builder = sup;
  }

  /**
   * Sets up the model.
   */
  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    TestUtil.testEnum(ClockEventType.class);
    TestUtil.testEnum(PriorityThreadFactory.class);
    TestUtil.testEnum(TMFactories.class);
    setModel((T) builder.build(FakeDependencyProvider.empty()));
  }

  /**
   * Test static properties of time model.
   */
  @Test
  public void testDefaultProperties() {
    assertThat(getModel().getTickListeners()).isEmpty();
    assertThat(getModel().getCurrentTime()).isEqualTo(0L);
    assertThat(getModel().isTicking()).isFalse();
  }

  /**
   * Basic register/unregister tests.
   */
  @Test
  public void testTicks() {
    final TickListenerChecker a = checker();
    final LimitingTickListener ltl = new LimitingTickListener(getModel(), 1);
    a.assertCountEquals(0L);

    assertThat(getModel().register(a)).isTrue();
    assertThat(getModel().register(ltl)).isTrue();
    assertThat(getModel().getTickListeners()).containsExactly(a, ltl).inOrder();

    getModel().start();

    assertThat(getModel().getCurrentTime()).isEqualTo(
      getModel().getTickLength());
    assertThat(a.getTickCount()).isEqualTo(1L);
    getModel().unregister(a);
    assertThat(getModel().getTickListeners()).containsExactly(ltl);

    assertThat(a.getTickCount()).isEqualTo(1L);

    // re-register
    assertThat(getModel().register(a)).isTrue();
    assertThat(getModel().getTickListeners()).containsExactly(ltl, a).inOrder();
  }

  /**
   * Tests that adding a listener twice yields no result.
   */
  @Test
  public void testTwice() {
    final TickListener a = checker();
    getModel().register(a);
    boolean fail = false;
    try {
      getModel().register(a);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertThat(fail).isTrue();

    assertThat(getModel().getTickListeners()).containsExactly(a);
  }

  /**
   * Test that listeners are called in order of adding to model.
   */
  @Test
  public void testTickOrder() {
    final TickListenerChecker a = checker();
    final TickListenerChecker b = checker();
    getModel().register(a);
    getModel().register(b);

    getModel().register(new LimitingTickListener(getModel(), 1));
    getModel().start();

    assertThat(b.getLastTickTime() - a.getLastTickTime()).isAtLeast(0L);
    assertThat(a.getLastAfterTickTime() - b.getLastTickTime()).isAtLeast(0L);
    assertThat(b.getLastAfterTickTime() - a.getLastAfterTickTime())
      .isAtLeast(0L);
  }

  /**
   * Test that removing a tick listener during a tick is performed correctly.
   */
  @Test
  public void removeDuringTick() {
    final TickListenerChecker a = new TickListenerChecker(getModel()) {
      @Override
      public void tick(TimeLapse timeLapse) {
        super.tick(timeLapse);
        if (getTickCount() > 2) {
          getModel().unregister(this);
        }
      }
    };
    final TickListenerChecker b = new TickListenerChecker(getModel()) {
      @Override
      public void afterTick(TimeLapse timeLapse) {
        super.afterTick(timeLapse);
        if (getTickCount() > 2) {
          getModel().unregister(this);
        }
      }
    };
    final TickListenerChecker d = new TickListenerChecker(getModel());
    final TickListenerChecker c = new TickListenerChecker(getModel()) {
      @Override
      public void tick(TimeLapse timeLapse) {
        super.tick(timeLapse);
        if (getTickCount() > 2) {
          getModel().unregister(d);
        }
      }
    };
    assertThat(getModel().register(a)).isTrue();
    assertThat(getModel().register(b)).isTrue();
    assertThat(getModel().register(c)).isTrue();
    assertThat(getModel().register(d)).isTrue();
    assertThat(getModel().register(new LimitingTickListener(getModel(), 4)))
      .isTrue();
    getModel().start();
    assertThat(getModel().getCurrentTime()).isEqualTo(
      4 * getModel().getTickLength());

    assertThat(a.getTickCount()).isEqualTo(3);
    assertThat(a.getAfterTickCount()).isEqualTo(2);

    assertThat(b.getTickCount()).isEqualTo(3);
    assertThat(b.getAfterTickCount()).isEqualTo(3);

    assertThat(c.getTickCount()).isEqualTo(4);
    assertThat(c.getAfterTickCount()).isEqualTo(4);

    assertThat(d.getTickCount()).isEqualTo(3);
    assertThat(d.getAfterTickCount()).isEqualTo(2);
  }

  /**
   * Tests that time lapses provided via the listener are behaving as they
   * should.
   */
  @Test
  public void timeLapseSafety() {
    final List<IllegalArgumentException> failures = new ArrayList<>();
    getModel().register(new LimitingTickListener(getModel(), 3));
    getModel().register(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        timeLapse.consume(1L);
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {
        try {
          timeLapse.consume(1L);
        }
        catch (final IllegalArgumentException e) {
          failures.add(e);
        }
      }
    });
    getModel().start();
    assertThat(failures).hasSize(3);
  }

  TickListenerChecker checker() {
    return new TickListenerChecker(getModel().getTickLength(), getModel()
      .getTimeUnit());
  }

  /**
   * @return the model
   */
  T getModel() {
    return model;
  }

  /**
   * @param model the model to set
   */
  void setModel(T model) {
    this.model = model;
  }
}
