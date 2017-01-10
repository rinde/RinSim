/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.measure.quantity.Duration;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.ModelManager;
import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.rand.RandomModel;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.time.ClockController;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

/**
 * Simulator is the core class of a simulation. It is responsible for managing
 * time which it does by periodically providing {@link TimeLapse} instances to
 * registered {@link TickListener}s. Further it provides methods to start and
 * stop simulations. The simulator also acts as a facade through which
 * {@link Model}s and objects can be added to the simulator.
 *
 * The configuration phase of the simulator looks as follows:
 * <ol>
 * <li>Use {@link Simulator#builder()} to register models and define other
 * global settings.</li>
 * <li>Obtain a {@link Simulator} instance via {@link Simulator.Builder#build()}
 * .
 * <li>register objects using {@link #register(Object)}</li>
 * <li>start simulation by calling {@link #start()}</li>
 * </ol>
 *
 * @author Rinde van Lon
 * @author Bartosz Michalik
 */
public final class Simulator implements SimulatorAPI {
  private static final Logger LOGGER = LoggerFactory.getLogger(Simulator.class);

  private final ModelManager modelManager;
  private final ClockController clock;
  private final RandomModel rand;
  private final Set<Object> toUnregister;

  Simulator(Builder b) {
    modelManager = b.mmBuilder
      .add(SimulatorModelBuilder.create(this))
      .addDefaultProvider(
        RandomModel.builder().withRandomGenerator(
          StochasticSuppliers.constant(b.rng)))
      .addDefaultProvider(
        TimeModel.builder()
          .withTickLength(b.tickLength)
          .withTimeUnit(b.timeUnit))
      .build();
    toUnregister = new LinkedHashSet<>();
    clock = modelManager.getModel(TimeModel.class);
    rand = modelManager.getModel(RandomModel.class);
  }

  /**
   * @param m The model.
   * @return False.
   * @deprecated To add a {@link Model} use {@link #builder()} instead.
   * @throws UnsupportedOperationException is always thrown.
   */
  @SuppressWarnings("static-method")
  @Deprecated
  public boolean register(Model<?> m) {
    throw new UnsupportedOperationException(
      "Models can be added via Simulator.builder().");
  }

  @Override
  public void register(Object obj) {
    LOGGER.info("{} - register({})", clock.getCurrentTime(), obj);
    modelManager.register(obj);
  }

  /**
   * {@inheritDoc} Unregistration from the models is delayed until all ticks are
   * processed.
   */
  @Override
  public void unregister(Object o) {
    if (o instanceof Model<?>) {
      throw new IllegalArgumentException("can not unregister a model");
    }
    toUnregister.add(o);
  }

  void checkUnregister() {
    if (!toUnregister.isEmpty()) {
      for (final Object o : toUnregister) {
        modelManager.unregister(o);
      }
      toUnregister.clear();
    }
  }

  /**
   * Returns all models registered in the simulator.
   * @return immutable list of models.
   */
  public ImmutableSet<Model<?>> getModels() {
    return modelManager.getModels();
  }

  /**
   * Returns the {@link ModelProvider} that has all registered models.
   * @return The model provider
   */
  public ModelProvider getModelProvider() {
    return modelManager;
  }

  /**
   * @return The current simulation time.
   */
  public long getCurrentTime() {
    return clock.getCurrentTime();
  }

  /**
   * @return The time step (in simulation time) which is added to current time
   *         at every tick.
   */
  public long getTimeStep() {
    return clock.getTickLength();
  }

  /**
   * Adds a tick listener to the simulator.
   * @param listener The listener to add.
   */
  public void addTickListener(TickListener listener) {
    register(listener);
  }

  /**
   * Removes the listener specified.
   * @param listener The listener to remove
   */
  public void removeTickListener(TickListener listener) {
    unregister(listener);
  }

  /**
   * Start the simulation.
   */
  public void start() {
    if (modelManager.getUserInterface().isPresent()) {
      modelManager.getUserInterface().get().show();
    } else {
      clock.start();
    }
  }

  /**
   * Either starts or stops the simulation depending on the current state.
   */
  public void togglePlayPause() {
    if (!clock.isTicking()) {
      start();
    } else {
      stop();
    }
  }

  /**
   * Stops the simulation.
   */
  public void stop() {
    clock.stop();
  }

  /**
   * Advances the time a single tick.
   */
  public void tick() {
    clock.tick();
  }

  /**
   * @return true if simulator is playing, false otherwise.
   */
  public boolean isPlaying() {
    return clock.isTicking();
  }

  /**
   * Get access to the main random generator used in the simulator.
   * @return the random generator of the simulator
   */
  @Override
  public RandomGenerator getRandomGenerator() {
    return rand.get(RandomProvider.class).masterInstance();
  }

  /**
   * @return The unit of time that is used for generating ticks.
   */
  public Unit<Duration> getTimeUnit() {
    return clock.getTimeUnit();
  }

  /**
   * @return A new {@link Builder} for creating a {@link Simulator} instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for {@link Simulator}.
   * @author Rinde van Lon
   */
  public static class Builder {
    static final long DEFAULT_SEED = 123L;
    static final long DEFAULT_TICK_LENGTH = 1000L;

    RandomGenerator rng;
    Unit<Duration> timeUnit;
    long tickLength;
    ModelManager.Builder mmBuilder;

    Builder() {
      rng = new MersenneTwister(DEFAULT_SEED);
      timeUnit = SI.MILLI(SI.SECOND);
      tickLength = DEFAULT_TICK_LENGTH;
      mmBuilder = ModelManager.builder();
    }

    /**
     * Sets the random seed used in the {@link RandomGenerator} used in the
     * simulator. The default {@link RandomGenerator} is a
     * {@link MersenneTwister} with seed <code>123</code>.
     * @param seed The seed to use.
     * @return This, as per the builder pattern.
     */
    public Builder setRandomSeed(long seed) {
      rng.setSeed(seed);
      return this;
    }

    /**
     * Sets the {@link RandomGenerator} to use in the simulator. This overwrites
     * any previous calls made to {@link #setRandomSeed(long)}. The default
     * {@link RandomGenerator} is a {@link MersenneTwister} with seed
     * <code>123</code>.
     * @param randomGenerator The generator to set.
     * @return This, as per the builder pattern.
     */
    public Builder setRandomGenerator(RandomGenerator randomGenerator) {
      rng = randomGenerator;
      return this;
    }

    /**
     * Sets the time unit to use in the simulator. The default time unit is
     * milliseconds.
     * @param unit The time unit to use.
     * @return This, as per the builder pattern.
     */
    public Builder setTimeUnit(Unit<Duration> unit) {
      timeUnit = unit;
      return this;
    }

    /**
     * Sets the length of a single tick in the simulator. The default tick
     * length is <code>1000</code>.
     * @param length The tick length to set.
     * @return This, as per the builder pattern.
     */
    public Builder setTickLength(long length) {
      checkArgument(length > 0,
        "Tick length must be strictly positive but is %s.", length);
      tickLength = length;
      return this;
    }

    /**
     * Adds the specified {@link ModelBuilder} to the simulator. The
     * {@link ModelBuilder} will be used to obtain a {@link Model} instance that
     * will be added to the simulator.
     * @param builder The builder to add.
     * @return This, as per the builder pattern.
     */
    public Builder addModel(ModelBuilder<?, ?> builder) {
      mmBuilder.add(builder);
      return this;
    }

    /**
     * Adds the specified {@link ModelBuilder}s to the simulator. Each
     * {@link ModelBuilder} will be used to obtain a {@link Model} instance that
     * will be added to the simulator.
     * @param builders The builders to add.
     * @return This, as per the builder pattern.
     */
    public Builder addModels(Iterable<? extends ModelBuilder<?, ?>> builders) {
      for (final ModelBuilder<?, ?> b : builders) {
        addModel(b);
      }
      return this;
    }

    /**
     * Builds the simulator, at least one {@link Model} must have been added.
     * @return A new {@link Simulator} instance.
     */
    public Simulator build() {
      return new Simulator(this);
    }
  }

  @AutoValue
  abstract static class SimulatorModelBuilder extends
      AbstractModelBuilder<SimulatorModel, SimulatorUser> {

    SimulatorModelBuilder() {
      setProvidingTypes(SimulatorAPI.class);
    }

    abstract Simulator getSimulator();

    @Override
    public SimulatorModel build(DependencyProvider dependencyProvider) {
      return new SimulatorModel(getSimulator());
    }

    static SimulatorModelBuilder create(Simulator sim) {
      return new AutoValue_Simulator_SimulatorModelBuilder(sim);
    }
  }

  static class SimulatorModel extends AbstractModel<SimulatorUser>
      implements TickListener {
    final Simulator simulator;

    SimulatorModel(Simulator sim) {
      simulator = sim;
    }

    @Override
    public boolean register(SimulatorUser element) {
      element.setSimulator(simulator);
      return true;
    }

    @Override
    public boolean unregister(SimulatorUser element) {
      return true;
    }

    @Override
    @Nonnull
    public <U> U get(Class<U> clazz) {
      checkArgument(clazz == SimulatorAPI.class);
      return clazz.cast(simulator);
    }

    @Override
    public void tick(TimeLapse timeLapse) {}

    @Override
    public void afterTick(TimeLapse timeLapse) {
      simulator.checkUnregister();
    }
  }
}
