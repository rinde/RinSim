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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.ModelManager;
import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;

/**
 * Simulator is the core class of a simulation. It is responsible for managing
 * time which it does by periodically providing {@link TimeLapse} instances to
 * registered {@link TickListener}s. Further it provides methods to start and
 * stop simulations. The simulator also acts as a facade through which
 * {@link Model}s and objects can be added to the simulator, more info about
 * models can be found in {@link ModelManager}.
 *
 * The configuration phase of the simulator looks as follows:
 * <ol>
 * <li>Use {@link Simulator#builder()} to register models and define other
 * global settings.</li>
 * <li>Obtain a {@link Simulator} instance via {@link Simulator.Builder#build()}.
 * <li>register objects using {@link #register(Object)}</li>
 * <li>start simulation by calling {@link #start()}</li>
 * </ol>
 *
 * @author Rinde van Lon
 * @author Bartosz Michalik
 */
public final class Simulator implements SimulatorAPI {

  /**
   * The logger of the simulator.
   */
  protected static final Logger LOGGER = LoggerFactory
      .getLogger(Simulator.class);

  /**
   * Enum that describes the possible types of events that the simulator can
   * dispatch.
   */
  public enum SimulatorEventType {
    /**
     * Indicates that the simulator has stopped.
     */
    STOPPED,

    /**
     * Indicates that the simulator has started.
     */
    STARTED,

    /**
     * Indicates that the simulator has been configured.
     */
    CONFIGURED
  }

  /**
   * Contains the set of registered {@link TickListener}s.
   */
  protected volatile Set<TickListener> tickListeners;

  /**
   * Reference to dispatcher of simulator events, can be used by subclasses to
   * issue additional events.
   */
  protected final EventDispatcher dispatcher;

  /**
   * @see #isPlaying
   */
  protected volatile boolean isPlaying;

  /**
   * @see #getCurrentTime()
   */
  protected long time;

  /**
   * Model manager instance.
   */
  protected final ModelManager modelManager;
  private boolean configured;

  private Set<Object> toUnregister;
  private final RandomGenerator rand;
  private final long timeStep;
  private final TimeLapse timeLapse;

  /**
   * Create a new simulator instance.
   * @param r The random number generator that is used in this simulator.
   * @param step The time that passes each tick. This can be in any unit the
   *          programmer prefers.
   * @deprecated Use {@link #builder()} instead.
   */
  @Deprecated
  public Simulator(RandomGenerator r, Measure<Long, Duration> step) {
    this(r, step, Arrays.<Model<?>> asList());
  }

  Simulator(RandomGenerator r, Measure<Long, Duration> step,
      List<Model<?>> models) {
    LOGGER.info("Simulator constructor, time step {}", step);
    checkArgument(step.getValue() > 0L,
        "Step must be a strictly positive number.");
    timeStep = step.getValue();
    tickListeners = Collections
        .synchronizedSet(new LinkedHashSet<TickListener>());

    toUnregister = new LinkedHashSet<Object>();

    rand = r;
    time = 0L;
    // time lapse is reused in a Flyweight kind of style
    timeLapse = new TimeLapse(step.getUnit());

    modelManager = new ModelManager();

    dispatcher = new EventDispatcher(SimulatorEventType.values());
    for (final Model<?> m : models) {
      register(m);
    }

    if (!models.isEmpty()) {

      configure();
    }
  }

  /**
   * This configures the {@link Model}s in the simulator. After calling this
   * method models can no longer be added, objects can only be registered after
   * this method is called.
   * @see ModelManager#configure()
   * @deprecated Use {@link #builder()} instead.
   */
  @Deprecated
  public void configure() {
    for (final Model<?> m : modelManager.getModels()) {
      if (m instanceof TickListener) {
        LOGGER.info("adding {} as a tick listener", m.getClass().getName());
        addTickListener((TickListener) m);
      }
    }
    modelManager.configure();
    configured = true;
    dispatcher.dispatchEvent(new Event(SimulatorEventType.CONFIGURED, this));
  }

  /**
   * Register a model to the simulator.
   * @param model The {@link Model} instance to register.
   * @return true if successful, false otherwise
   * @deprecated Use {@link #builder()} instead.
   */
  @Deprecated
  public boolean register(Model<?> model) {
    if (configured) {
      throw new IllegalStateException(
          "cannot add model after calling configure()");
    }
    injectDependencies(model);
    final boolean result = modelManager.add(model);
    if (result) {
      LOGGER.info("registering model {} for type {}.", model.getClass()
          .getName(), model.getSupportedType().getName());
    }
    return result;
  }

  @Override
  public boolean register(Object obj) {
    if (obj instanceof Model<?>) {
      return register((Model<?>) obj);
    }
    if (!configured) {
      configure();
    }
    checkArgument(!modelManager.getModels().isEmpty(), "No models are added.");
    LOGGER.info("{} - register({})", time, obj);
    injectDependencies(obj);
    if (obj instanceof TickListener) {
      addTickListener((TickListener) obj);
    }
    return modelManager.register(obj);
  }

  /**
   * {@inheritDoc} Unregistration from the models is delayed until all ticks are
   * processed.
   */
  @Override
  public boolean unregister(Object o) {
    if (o instanceof Model<?>) {
      throw new IllegalArgumentException("can not unregister a model");
    }
    if (!configured) {
      throw new IllegalStateException(
          "can not unregister object before calling configure()");
    }
    if (o instanceof TickListener) {
      removeTickListener((TickListener) o);
    }
    toUnregister.add(o);
    return true;
  }

  /**
   * Inject all required dependecies basing on the declared types of the object.
   * @param o object that need to have dependecies injected
   */
  protected void injectDependencies(Object o) {
    if (o instanceof SimulatorUser) {
      ((SimulatorUser) o).setSimulator(this);
    }
  }

  /**
   * Returns a safe to modify list of all models registered in the simulator.
   * @return list of models
   */
  public List<Model<?>> getModels() {
    return modelManager.getModels();
  }

  /**
   * Returns the {@link ModelProvider} that has all registered models.
   * @return The model provider
   */
  public ModelProvider getModelProvider() {
    return modelManager;
  }

  @Override
  public long getCurrentTime() {
    return time;
  }

  @Override
  public long getTimeStep() {
    return timeStep;
  }

  /**
   * Adds a tick listener to the simulator.
   * @param listener The listener to add.
   */
  public void addTickListener(TickListener listener) {
    tickListeners.add(listener);
  }

  /**
   * Removes the listener specified. Implemented in O(1).
   * @param listener The listener to remove
   */
  public void removeTickListener(TickListener listener) {
    tickListeners.remove(listener);
  }

  /**
   * Start the simulation.
   */
  public void start() {
    if (!configured) {
      configure();
    }
    if (!isPlaying) {
      dispatcher.dispatchEvent(new Event(SimulatorEventType.STARTED, this));
    }
    isPlaying = true;
    while (isPlaying) {
      tick();
    }
    dispatcher.dispatchEvent(new Event(SimulatorEventType.STOPPED, this));
  }

  /**
   * Advances the simulator with one step (the size is determined by the time
   * step).
   */
  public void tick() {
    // unregister all pending objects
    Set<Object> copy;
    copy = toUnregister;
    toUnregister = new LinkedHashSet<Object>();

    for (final Object c : copy) {
      modelManager.unregister(c);
    }

    // using a copy to avoid concurrent modifications of this set
    // this also means that adding or removing a TickListener is
    // effectively executed after a 'tick'

    final List<TickListener> localCopy = new ArrayList<TickListener>();
    localCopy.addAll(tickListeners);

    final long end = time + timeStep;
    LOGGER.trace("{} ->----> tick ->----> {}", time, end);
    for (final TickListener t : localCopy) {
      timeLapse.initialize(time, end);
      t.tick(timeLapse);
    }
    timeLapse.initialize(time, end);
    // in the after tick the TimeLapse can no longer be consumed
    timeLapse.consumeAll();
    for (final TickListener t : localCopy) {
      t.afterTick(timeLapse);
    }
    time += timeStep;

  }

  /**
   * Either starts or stops the simulation depending on the current state.
   */
  public void togglePlayPause() {
    if (!isPlaying) {
      start();
    } else {
      isPlaying = false;
    }
  }

  /**
   * Resets the time to 0.
   */
  public void resetTime() {
    time = 0L;
  }

  /**
   * Stops the simulation.
   */
  public void stop() {
    isPlaying = false;
  }

  /**
   * @return true if simulator is playing, false otherwise.
   */
  public boolean isPlaying() {
    return isPlaying;
  }

  /**
   * @return <code>true</code> if the simulator is configured, see
   *         {@link Simulator} for more information.
   */
  public boolean isConfigured() {
    return configured;
  }

  /**
   * @return An unmodifiable view on the set of tick listeners.
   */
  public Set<TickListener> getTickListeners() {
    return Collections.unmodifiableSet(tickListeners);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RandomGenerator getRandomGenerator() {
    return rand;
  }

  @Override
  public Unit<Duration> getTimeUnit() {
    return timeLapse.getTimeUnit();
  }

  @Override
  public EventAPI getEventAPI() {
    return dispatcher.getPublicEventAPI();
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
    RandomGenerator rng;
    Unit<Duration> timeUnit;
    long tickLength;
    List<Model<?>> models;

    Builder() {
      rng = new MersenneTwister(123L);
      timeUnit = SI.MILLI(SI.SECOND);
      tickLength = 1000L;
      models = new ArrayList<>();
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
     * Adds the specified {@link Model} to the simulator.
     * @param model The model to add.
     * @return This, as per the builder pattern.
     */
    public Builder addModel(Model<?> model) {
      models.add(model);
      return this;
    }

    /**
     * Builds the simulator, at least one {@link Model} must have been added.
     * @return A new {@link Simulator} instance.
     */
    public Simulator build() {
      checkArgument(!models.isEmpty(), "At least one model must be added.");
      final Simulator sim = new Simulator(rng, Measure.valueOf(tickLength,
          timeUnit));
      for (final Model<?> m : models) {
        sim.register(m);
      }
      sim.configure();
      return sim;
    }
  }
}
