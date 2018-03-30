/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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

import javax.annotation.Nullable;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableSet;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 *
 */
public class SimulatorTest {

  @SuppressWarnings("null")
  private Simulator simulator;

  /**
   * Set up a simulator with the default models.
   */
  @Before
  public void setUp() {
    simulator = Simulator.builder()
      .setRandomGenerator(new MersenneTwister(123L))
      .setTickLength(100L)
      .setTimeUnit(SI.SECOND)
      .build();
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

  /**
   * Registration test.
   */
  @Test
  public void testRegister() {
    final Simulator sim = Simulator.builder()
      .addModel(DummyModel.builder())
      .addModel(DummyModel.builder())
      .addModel(DummyModelAsTickListener.builderAsTickListener())
      .build();

    assertThat(sim.getModels().asList().get(0)).isInstanceOf(DummyModel.class);
    assertThat(sim.getModels().asList().get(1)).isInstanceOf(DummyModel.class);
    assertThat(sim.getModels().asList().get(2)).isInstanceOf(
      DummyModelAsTickListener.class);

    sim.register(new DummyObject());

    final DummyObjectTickListener dotl = new DummyObjectTickListener();
    sim.register(dotl);

    final DummyObjectSimulationUser dosu = new DummyObjectSimulationUser();
    sim.register(dosu);
    assertEquals(sim, dosu.getAPI());

    sim.unregister(new DummyObject());
    sim.unregister(new DummyObjectTickListener());

  }

  /**
   * Models can not be unregistered.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testUnregisterModel() {
    simulator.unregister(new DummyModel());
  }

  /**
   * Test that unregister works.
   */
  @Test
  public void testUnregisterObj() {
    final Simulator sim = Simulator.builder()
      .addModel(DummyModel.builder())
      .build();

    final DummyModel dm = sim.getModelProvider().getModel(DummyModel.class);

    assertThat(dm.getRegisteredObjects()).isEmpty();
    final DummyObject dotl = new DummyObject();
    sim.register(dotl);

    assertThat(dm.getRegisteredObjects()).containsExactly(dotl);

    sim.unregister(dotl);
    sim.tick();

    assertThat(dm.getRegisteredObjects()).isEmpty();
  }

  /**
   * Tests that the rng is defined.
   */
  @Test
  public void testGetRnd() {
    assertNotNull(simulator.getRandomGenerator());
  }

  /**
   * Simple test for building models.
   */
  @Test
  public void testModelBuilder() {
    final Simulator sim = Simulator.builder()
      .addModel(new ModelBuilder<DummyModel, DummyObject>() {
        @Override
        public DummyModel build(DependencyProvider dp) {
          final RandomProvider rg = dp.get(RandomProvider.class);
          rg.newInstance();
          return new DummyModel();
        }

        @Override
        public ImmutableSet<Class<?>> getDependencies() {
          return ImmutableSet.<Class<?>>of(RandomProvider.class);
        }

        @Override
        public Class<DummyObject> getAssociatedType() {
          return DummyObject.class;
        }

        @Override
        public ImmutableSet<Class<?>> getProvidingTypes() {
          return ImmutableSet.<Class<?>>of(Object.class);
        }

        @Override
        public Class<DummyModel> getModelType() {
          return DummyModel.class;
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
      .addModel(new CircleA())
      .addModel(new CircleB())
      .addModel(new CircleC());

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
      .addModel(new CircleA());
    boolean fail = false;
    try {
      b.build();
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  /**
   * Tests that a duplicate provider is detected correctly.
   */
  @Test
  public void testDuplicateProvider() {
    final Simulator.Builder b = Simulator.builder()
      .addModel(new DuplicateA());
    boolean fail = false;
    try {
      b.addModel(new DuplicateB());
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(
        "provider for class java.lang.Object already exists");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  /**
   * Tests that not using a dependency is detected.
   */
  @Test
  public void testNopBuilder() {
    final Simulator.Builder b = Simulator.builder()
      .addModel(new NopBuilder())
      .addModel(new A())
      .addModel(new B());
    boolean fail = false;
    try {
      b.build();
    } catch (final IllegalStateException e) {
      assertThat(e.getMessage())
        .containsMatch("dependencies MUST be requested");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  /**
   * Tests that requesting two instances of a type results in a failure.
   */
  @Test
  public void testAskTwiceBuilder() {
    final Simulator.Builder b = Simulator.builder()
      .addModel(new AskTwiceBuilder())
      .addModel(new A())
      .addModel(new B());
    boolean fail = false;
    try {
      b.build();
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).containsMatch("is already requested");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  /**
   * Tests that asking a undeclared dependency results in a failure.
   */
  @Test
  public void testAskWrongTypeBuilder() {
    final Simulator.Builder b = Simulator.builder()
      .addModel(new AskWrongTypeBuilder())
      .addModel(new A())
      .addModel(new B());
    boolean fail = false;
    try {
      b.build();
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).containsMatch("is not a type that");
      assertThat(e.getMessage()).containsMatch("declared as a dependency");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  /**
   * A builder that has no declared dependencies can not request any.
   */
  @Test
  public void testAskWithoutAnyDepsBuilder() {
    final Simulator.Builder b = Simulator.builder();
    boolean fail = false;
    try {
      b.addModel(new AskWithoutAnyDepsBuilder());
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).containsMatch(
        "did not declare any dependencies");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  abstract class IllBehavedBuilderBase extends
      AbstractModelBuilder<GenericModel<Object>, Object> {
    IllBehavedBuilderBase() {
      setDependencies(ProviderForA.class);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return this == other;
    }

    @Override
    public int hashCode() {
      return 0;
    }
  }

  class NopBuilder extends IllBehavedBuilderBase {
    @Override
    public GenericModel<Object> build(DependencyProvider dependencyProvider) {
      return new GenericModel<>();
    }
  }

  class AskTwiceBuilder extends IllBehavedBuilderBase {
    @Override
    public GenericModel<Object> build(DependencyProvider dependencyProvider) {
      dependencyProvider.get(ProviderForA.class);
      dependencyProvider.get(ProviderForA.class);
      return new GenericModel<>();
    }
  }

  class AskWrongTypeBuilder extends IllBehavedBuilderBase {
    @Override
    public GenericModel<Object> build(DependencyProvider dependencyProvider) {
      dependencyProvider.get(ProviderForA.class);
      dependencyProvider.get(ProviderForB.class);
      return new GenericModel<>();
    }
  }

  abstract class TestModelBuilder<T extends Model<? extends U>, U> extends
      AbstractModelBuilder<T, U> {
    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return this == other;
    }
  }

  class AskWithoutAnyDepsBuilder extends
      TestModelBuilder<GenericModel<Object>, Object> {
    @Override
    public GenericModel<Object> build(DependencyProvider dependencyProvider) {
      dependencyProvider.get(ProviderForA.class);
      return new GenericModel<>();
    }
  }

  class DuplicateA extends TestModelBuilder<GenericModel<Object>, Object> {
    DuplicateA() {
      setProvidingTypes(Object.class);
    }

    @Override
    public GenericModel<Object> build(DependencyProvider dependencyProvider) {
      return new GenericModel<>();
    }
  }

  class DuplicateB extends TestModelBuilder<GenericModel<Object>, Object> {
    DuplicateB() {
      setProvidingTypes(Object.class);
    }

    @Override
    public GenericModel<Object> build(DependencyProvider dependencyProvider) {
      return new GenericModel<>();
    }
  }

  class GenericModel<T> extends AbstractModel<T> {
    Set<T> set;
    ImmutableClassToInstanceMap<Object> map;

    GenericModel() {
      this(ImmutableClassToInstanceMap.builder().build());
    }

    GenericModel(ImmutableClassToInstanceMap<Object> m) {
      set = new LinkedHashSet<>();
      map = m;
    }

    @Override
    public boolean register(T element) {
      return set.add(element);
    }

    @Override
    public boolean unregister(T element) {
      return set.remove(element);
    }

    @Override
    public <U> U get(Class<U> type) {
      final U value = map.getInstance(type);
      assertNotNull(value);
      return value;
    }
  }

  class A extends TestModelBuilder<GenericModel<Object>, Object> {
    A() {
      setProvidingTypes(ProviderForA.class);
      setDependencies(ProviderForB.class);
    }

    @Override
    public GenericModel<Object> build(DependencyProvider dependencyProvider) {
      dependencyProvider.get(ProviderForB.class);
      return new GenericModel<>(ImmutableClassToInstanceMap.builder()
        .put(ProviderForA.class, new ProviderForA())
        .build());
    }
  }

  class B extends TestModelBuilder<GenericModel<Object>, Object> {
    B() {
      setProvidingTypes(ProviderForB.class);
    }

    @Override
    public GenericModel<Object> build(DependencyProvider dependencyProvider) {
      return new GenericModel<>(ImmutableClassToInstanceMap.builder()
        .put(ProviderForB.class, new ProviderForB())
        .build());
    }
  }

  class CircleA extends TestModelBuilder<GenericModel<Object>, Object> {
    CircleA() {
      setProvidingTypes(ProviderForA.class);
      setDependencies(ProviderForB.class);
    }

    @SuppressWarnings("null")
    @Override
    public GenericModel<Object> build(DependencyProvider dependencyProvider) {
      return null;
    }
  }

  class CircleB extends TestModelBuilder<GenericModel<Object>, Object> {
    CircleB() {
      setProvidingTypes(ProviderForB.class);
      setDependencies(ProviderForC.class);
    }

    @SuppressWarnings("null")
    @Override
    public GenericModel<Object> build(DependencyProvider dependencyProvider) {
      return null;
    }
  }

  class CircleC extends TestModelBuilder<GenericModel<Object>, Object> {
    CircleC() {
      setProvidingTypes(ProviderForC.class);
      setDependencies(ProviderForA.class);
    }

    @SuppressWarnings("null")
    @Override
    public GenericModel<Object> build(DependencyProvider dependencyProvider) {
      return null;
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
    @SuppressWarnings("null")
    private SimulatorAPI receivedAPI;

    @Override
    public void setSimulator(SimulatorAPI api) {
      receivedAPI = api;
    }

    public SimulatorAPI getAPI() {
      return receivedAPI;
    }
  }

  static class DummyModelAsTickListener extends DummyModel implements
      TickListener {

    @Override
    public void tick(TimeLapse tl) {}

    @Override
    public void afterTick(TimeLapse tl) {}

    static Builder builderAsTickListener() {
      return new Builder();
    }

    static class Builder extends
        AbstractModelBuilder<DummyModelAsTickListener, DummyObject> {

      @Override
      public DummyModelAsTickListener build(
          DependencyProvider dependencyProvider) {
        return new DummyModelAsTickListener();
      }

      @Override
      public int hashCode() {
        return System.identityHashCode(this);
      }

      @Override
      public boolean equals(@Nullable Object other) {
        return other != null && other.getClass() == getClass();
      }
    }
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
        if (tl.getTime() > limit * tl.getTickLength()) {
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
