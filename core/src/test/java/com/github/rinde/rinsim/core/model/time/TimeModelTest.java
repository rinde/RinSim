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
import static com.google.common.truth.Truth.assert_;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.measure.unit.NonSI;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.core.TickListener;
import com.google.common.base.Supplier;

/**
 * @author Rinde van Lon
 *
 */
@RunWith(Parameterized.class)
public class TimeModelTest {
  final Supplier<TimeModel> modelSupplier;
  TimeModel model;

  /**
   * @param sup The supplier to use for creating model instances.
   */
  @SuppressWarnings("null")
  public TimeModelTest(Supplier<TimeModel> sup) {
    modelSupplier = sup;
  }

  /**
   * Sets up the model.
   */
  @Before
  public void setUp() {
    model = modelSupplier.get();
  }

  /**
   * @return The models to test.
   */
  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        { TimeModel.defaultSupplier() },
        { TimeModel.supplier(333, NonSI.HOUR) }
    });
  }

  /**
   * Test static properties of time model.
   */
  @Test
  public void testDefaultProperties() {
    assertThat((Iterable<?>) model.getTickListeners()).isEmpty();
    assertThat(model.getCurrentTime()).comparesEqualTo(0L);
    assertThat(model.isTicking()).isFalse();
  }

  /**
   * Basic register/unregister tests.
   */
  @Test
  public void testTicks() {
    final TickListenerChecker a = checker();
    a.assertCountEquals(0L);

    assertThat(model.register(a)).isTrue();
    assertThat((Iterable<?>) model.getTickListeners()).containsExactly(a);

    model.tick();
    assertThat(model.getCurrentTime()).isEqualTo(model.getTimeStep());
    assertThat(a.getTickCount()).isEqualTo(1L);
    model.unregister(a);
    assertThat((Iterable<?>) model.getTickListeners()).isEmpty();
    model.tick();
    assertThat(a.getTickCount()).isEqualTo(1L);

    // re-register
    assertThat(model.register(a)).isTrue();
    assertThat((Iterable<?>) model.getTickListeners()).containsExactly(a);
  }

  /**
   * Tests that adding a listener twice yields no result.
   */
  @Test
  public void testTwice() {
    final TickListener a = checker();
    model.register(a);
    boolean fail = false;
    try {
      model.register(a);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertThat(fail).isTrue();

    assert_().that((Iterable<?>) model.getTickListeners()).containsExactly(a);
  }

  /**
   * Test that listeners are called in order of adding to model.
   */
  @Test
  public void testTickOrder() {
    final TickListenerChecker a = checker();
    final TickListenerChecker b = checker();
    model.register(a);
    model.register(b);
    model.tick();

    assertThat(b.getLastTickTime() - a.getLastTickTime()).isAtLeast(0L);
    assertThat(a.getLastAfterTickTime() - b.getLastTickTime()).isAtLeast(0L);
    assertThat(b.getLastAfterTickTime() - a.getLastAfterTickTime())
      .isAtLeast(0L);
  }

  /**
   * Test starting and stopping time.
   */
  @Test
  public void testStartStop() {
    final LimitingTickListener ltl = new LimitingTickListener(model, 3);
    model.register(ltl);
    model.start();
    assertEquals(3 * model.getTimeStep(), model.getCurrentTime());

    model.start();
    assertEquals(6 * model.getTimeStep(), model.getCurrentTime());
  }

  /**
   * Test that removing a tick listener during a tick is performed correctly.
   */
  @Test
  public void removeDuringTick() {
    final TickListenerChecker a = new TickListenerChecker(model) {
      @Override
      public void tick(TimeLapse timeLapse) {
        super.tick(timeLapse);
        if (getTickCount() > 2) {
          model.unregister(this);
        }
      }
    };
    final TickListenerChecker b = new TickListenerChecker(model) {
      @Override
      public void afterTick(TimeLapse timeLapse) {
        super.afterTick(timeLapse);
        if (getTickCount() > 2) {
          model.unregister(this);
        }
      }
    };
    final TickListenerChecker d = new TickListenerChecker(model);
    final TickListenerChecker c = new TickListenerChecker(model) {
      @Override
      public void tick(TimeLapse timeLapse) {
        super.tick(timeLapse);
        if (getTickCount() > 2) {
          model.unregister(d);
        }
      }
    };
    assertThat(model.register(a)).isTrue();
    assertThat(model.register(b)).isTrue();
    assertThat(model.register(c)).isTrue();
    assertThat(model.register(d)).isTrue();
    assertThat(model.register(new LimitingTickListener(model, 4))).isTrue();
    model.start();

    assertThat(model.getCurrentTime()).isEqualTo(4 * model.getTimeStep());

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
    model.register(new LimitingTickListener(model, 3));
    model.register(new TickListener() {
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
    model.start();
    assertThat(failures).hasSize(3);
  }

  TickListenerChecker checker() {
    return new TickListenerChecker(model.getTimeStep(), model.getTimeUnit());
  }
}
