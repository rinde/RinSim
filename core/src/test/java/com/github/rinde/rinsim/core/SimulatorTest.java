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
package com.github.rinde.rinsim.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.Simulator.SimulatorEventType;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.testutil.TestUtil;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 *
 */
public class SimulatorTest {

  @SuppressWarnings("null")
  private Simulator simulator;

  @Before
  public void setUp() {
    simulator = Simulator.builder()
      .setRandomGenerator(new MersenneTwister(123L))
      .setTickLength(100L)
      .setTimeUnit(SI.SECOND)
      .build();
    TestUtil.testEnum(SimulatorEventType.class);
  }

  /**
   * Models should not register.
   */
  @SuppressWarnings("deprecation")
  @Test(expected = UnsupportedOperationException.class)
  public void testRegisterModel() {
    simulator.register(new DummyModel());
  }

  /**
   * Models should not register.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testRegisterModel2() {
    simulator.register((Object) new DummyModel());
  }

  /**
   * Test the correct setting of the time model settings.
   */
  @Test
  public void testTimeModelSettings() {
    final Simulator sim = Simulator.builder()
      .setTickLength(123L)
      .setTimeUnit(NonSI.WEEK)
      .build();

    assertThat(sim.getTimeStep()).isEqualTo(123L);
    assertThat(sim.getTimeUnit()).isEqualTo(NonSI.WEEK);
  }

  @Test
  public void testRegister() {
    final DummyModel m1 = new DummyModel();
    final DummyModel m2 = new DummyModel();
    final DummyModelAsTickListener m3 = new DummyModelAsTickListener();
    final Simulator sim = Simulator.builder()
      .addModel(Suppliers.ofInstance(m1))
      .addModel(Suppliers.ofInstance(m2))
      .addModel(Suppliers.ofInstance(m3))
      .build();

    assertThat((Iterable<?>) sim.getModels()).containsAllOf(m1, m2, m3)
      .inOrder();

    sim.register(new DummyObject());

    final DummyObjectTickListener dotl = new DummyObjectTickListener();
    sim.register(dotl);

    final DummyObjectSimulationUser dosu = new DummyObjectSimulationUser();
    sim.register(dosu);
    assertEquals(sim, dosu.getAPI());

    sim.unregister(new DummyObject());
    sim.unregister(new DummyObjectTickListener());

  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnregisterModel() {
    simulator.unregister(new DummyModel());
  }

  @Test
  public void testStartWithoutConfiguring() {
    final LimitingTickListener ltl = new LimitingTickListener(simulator, 3);
    simulator.addTickListener(ltl);
    simulator.start();
    assertEquals(300, simulator.getCurrentTime());
  }

  @Test
  public void testGetRnd() {
    assertNotNull(simulator.getRandomGenerator());
  }

  @Test
  public void testModelBuilder() {
    final Simulator sim = Simulator.builder()
      .addModel(new AbstractModelBuilder<DummyObject>(Object.class) {
        @Override
        public Model<DummyObject> build(DependencyProvider dp) {
          final RandomProvider rg = dp.get(RandomProvider.class);
          rg.newInstance();
          return new DummyModel();
        }

        @Override
        public ImmutableSet<Class<?>> getDependencies() {
          return ImmutableSet.<Class<?>> of(RandomProvider.class);
        }
      })
      .build();
    assertThat((Iterable<?>) sim.getModels()).containsNoDuplicates();

  }

  /**
   * Tests correct detection of circular dependencies.
   */
  @Test
  public void testCircularDependencies() {
    final Simulator.Builder b = Simulator.builder()
      .addModel(new A())
      .addModel(new B())
      .addModel(new C());

    boolean fail = false;
    try {
      b.build();
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  /**
   * Tests correct detection of a dependency that is not supplied.
   */
  @Test
  public void testUnknownDependency() {
    final Simulator.Builder b = Simulator.builder()
      .addModel(new A());
    boolean fail = false;
    try {
      b.build();
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  /**
   * Tests that a duplicate provided is detected correctly.
   */
  @Test
  public void testDuplicateProvider() {
    final Simulator.Builder b = Simulator.builder()
      .addModel(new DuplicateA())
      .addModel(new DuplicateB());
    boolean fail = false;
    try {
      b.build();
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  class DuplicateA extends AbstractModelBuilder<Object> {
    DuplicateA() {
      super(Object.class);
    }

    @Override
    public GenericModel<Object> build(DependencyProvider dependencyProvider) {
      return new GenericModel<>();
    }
  }

  class DuplicateB extends AbstractModelBuilder<Object> {
    DuplicateB() {
      super(Object.class);
    }

    @Override
    public GenericModel<Object> build(DependencyProvider dependencyProvider) {
      return new GenericModel<>();
    }
  }

  class GenericModel<T> extends AbstractModel<T> {
    Set<T> set;

    GenericModel() {
      set = new LinkedHashSet<>();
    }

    @Override
    public boolean register(T element) {
      return set.add(element);
    }

    @Override
    public boolean unregister(T element) {
      return set.remove(element);
    }
  }

  class A extends AbstractModelBuilder<Object> {
    A() {
      super(ProviderForA.class);
    }

    @Override
    public Model<Object> build(DependencyProvider dependencyProvider) {
      return null;
    }

    @Override
    public ImmutableSet<Class<?>> getDependencies() {
      return ImmutableSet.<Class<?>> of(ProviderForB.class);
    }
  }

  class B extends AbstractModelBuilder<Object> {
    B() {
      super(ProviderForB.class);
    }

    @Override
    public Model<Object> build(DependencyProvider dependencyProvider) {
      return null;
    }

    @Override
    public ImmutableSet<Class<?>> getDependencies() {
      return ImmutableSet.<Class<?>> of(ProviderForC.class);
    }
  }

  class C extends AbstractModelBuilder<Object> {
    C() {
      super(ProviderForC.class);
    }

    @Override
    public Model<Object> build(DependencyProvider dependencyProvider) {
      return null;
    }

    @Override
    public ImmutableSet<Class<?>> getDependencies() {
      return ImmutableSet.<Class<?>> of(ProviderForA.class);
    }
  }

  class ProviderForA {}

  class ProviderForB {}

  class ProviderForC {}

  class DummyObject {}

  class DummyObjectTickListener implements TickListener {
    @Override
    public void tick(TimeLapse tl) {}

    @Override
    public void afterTick(TimeLapse tl) {}
  }

  class DummyObjectSimulationUser implements SimulatorUser {
    private SimulatorAPI receivedAPI;

    @Override
    public void setSimulator(SimulatorAPI api) {
      receivedAPI = api;
    }

    public SimulatorAPI getAPI() {
      return receivedAPI;
    }
  }

  class DummyModelAsTickListener extends DummyModel implements TickListener {

    @Override
    public void tick(TimeLapse tl) {}

    @Override
    public void afterTick(TimeLapse tl) {}

  }

  class LimitingTickListener implements TickListener {
    private final int limit;
    private int tickCount;
    private final Simulator sim;

    public LimitingTickListener(Simulator s, int tickLimit) {
      sim = s;
      limit = tickLimit;
      tickCount = 0;
    }

    public void reset() {
      tickCount = 0;
    }

    @Override
    public void tick(TimeLapse tl) {
      tickCount++;
    }

    @Override
    public void afterTick(TimeLapse tl) {
      if (tickCount >= limit) {
        assertTrue(sim.isPlaying());
        if (tl.getTime() > limit * tl.getTimeStep()) {
          sim.togglePlayPause();
        }
        sim.stop();
        assertFalse(sim.isPlaying());
        reset();
      }
    }
  }

  class TickListenerImpl implements TickListener {
    private int count = 0;
    private long execTime;
    private long afterTime;

    @Override
    public void tick(TimeLapse tl) {
      count++;
      execTime = System.nanoTime();
    }

    public long getExecTime() {
      return execTime;
    }

    public long getAfterExecTime() {
      return afterTime;
    }

    public int getTickCount() {
      return count;
    }

    @Override
    public void afterTick(TimeLapse tl) {
      afterTime = System.nanoTime();
    }
  }

}
