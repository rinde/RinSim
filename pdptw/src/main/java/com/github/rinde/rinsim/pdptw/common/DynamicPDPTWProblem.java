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
package com.github.rinde.rinsim.pdptw.common;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioController;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.google.common.collect.ImmutableMap;

/**
 * A problem instance for the class of problems which is called dynamic
 * pickup-and-delivery problems with time windows, often abbreviated as dynamic
 * PDPTW.
 * <p>
 * A problem instance is an instance which sets up everything related to the
 * 'problem' which one tries to solve. The idea is that a user only needs to
 * worry about adding its own solution to this instance.
 * <p>
 * By default this class needs very little customization, it needs to be given a
 * scenario which it then uses to configure the simulation.
 * @author Rinde van Lon
 */
public final class DynamicPDPTWProblem {

  // TODO stats system should be more modular (per model?) and hook directly in
  // the simulator

  // TODO if there can be some generic way to hook custom agents into the
  // simulator/scenario, this class can probably be removed

  /**
   * The {@link Simulator} which is used for the simulation.
   */
  protected final Simulator simulator;

  /**
   * The StatsTracker which is used internally for gathering statistics.
   */
  protected StatsTracker statsTracker;

  /**
   * Create a new problem instance using the specified scenario.
   * @param scen The the {@link Scenario} which is used in this problem.
   * @param randomSeed The random seed which will be passed into the random
   *          number generator in the simulator.
   * @param models An optional list of models which can be added, with this
   *          option custom models for specific solutions can be added.
   */
  public DynamicPDPTWProblem(final Scenario scen, long randomSeed,
    Iterable<? extends ModelBuilder<?, ?>> models,
    ImmutableMap<Class<?>, TimedEventHandler<?>> m) {

    final Simulator.Builder simBuilder = Simulator.builder()
      .setRandomSeed(randomSeed)
      .addModels(models)
      .addModel(
        ScenarioController.builder(scen)
          .withEventHandlers(m)
      )
      .addModel(StatsTracker.builder());

    simulator = simBuilder.build();
    statsTracker = simulator.getModelProvider().getModel(StatsTracker.class);
  }

  /**
   * @return The statistics of the current simulation. Note that calling this
   *         method while the simulation is not yet finished gives the
   *         statistics that were gathered up until that moment.
   */
  public StatisticsDTO getStatistics() {
    return statsTracker.getStatistics();
  }

  /**
   * Executes a simulation of the problem. When the simulation is finished (and
   * this method returns) the statistics of the simulation are returned.
   * @return The statistics that were gathered during the simulation.
   */
  public StatisticsDTO simulate() {
    simulator.start();
    return getStatistics();
  }

  /**
   * This method exposes the {@link Simulator} that is managed by this problem
   * instance. Be careful with using it since it is possible to significantly
   * alter the behavior of the simulation.
   * @return The simulator.
   */
  public Simulator getSimulator() {
    return simulator;
  }

  public static <T extends TimedEvent> TimedEventHandler<T> adaptCreator(
    Creator<T> c) {
    return new CreatorAdapter<>(c);
  }

  static class CreatorAdapter<T extends TimedEvent> implements
    TimedEventHandler<T> {

    private final Creator<T> creator;

    CreatorAdapter(Creator<T> c) {
      creator = c;
    }

    @Override
    public void handleTimedEvent(T event, SimulatorAPI simulator) {
      creator.create((Simulator) simulator, event);
    }
  }

  /**
   * Factory for handling a certain type {@link TimedEvent}s. It is the
   * responsible of this instance to create the appropriate object when an event
   * occurs. All created objects can be added to the {@link Simulator} by using
   * {@link Simulator#register(Object)}.
   * @param <T> The specific subclass of {@link TimedEvent} for which the
   *          creator should create objects.
   */
  public interface Creator<T extends TimedEvent> {
    /**
     * Should add an object to the simulation.
     * @param sim The simulator to which the objects can be added.
     * @param event The {@link TimedEvent} instance that contains the event
     *          details.
     * @return <code>true</code> if the creation and adding of the object was
     *         successful, <code>false</code> otherwise.
     */
    boolean create(Simulator sim, T event);
  }
}
