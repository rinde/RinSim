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

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioController;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
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

  // TODO create a builder for configuration of problems
  // TODO a scenario should be an event list AND a pre-configured set of models
  // describing the complete problem

  // TODO a StopCondition should be a first class simulator entity
  // TODO same with ScenarioController, StatsTracker

  // TODO perhaps a UI config should also be bundled easily?

  // TODO stats system should be more modular (per model?) and hook directly in
  // the simulator

  // TODO if there can be some generic way to hook custom agents into the
  // simulator/scenario, this class can probably be removed

  /**
   * The {@link ScenarioController} which is used to play the scenario.
   */
  protected final ScenarioController controller;

  /**
   * The {@link Simulator} which is used for the simulation.
   */
  protected final Simulator simulator;

  /**
   * The {@link UICreator} which is used for creating the default UI.
   */
  // protected final DefaultUICreator defaultUICreator;

  /**
   * The StatsTracker which is used internally for gathering statistics.
   */
  protected StatsTracker statsTracker;

  /**
   * The {@link StopConditions} which is used as the condition when the
   * simulation has to stop.
   */
  protected Predicate<Simulator> stopCondition;

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

    final int ticks = scen.getTimeWindow().end == Long.MAX_VALUE ? -1
      : (int) (scen.getTimeWindow().end - scen.getTimeWindow().begin);

    final Simulator.Builder simBuilder = Simulator.builder()
      .setRandomSeed(randomSeed)
      .addModels(models)
      .addModel(
        ScenarioController.builder(scen)
          .withEventHandlers(m)
          .withNumberOfTicks(ticks)
      )
      .addModel(StatsTracker.builder());

    simulator = simBuilder.build();
    statsTracker = simulator.getModelProvider().getModel(StatsTracker.class);
    controller = simulator.getModelProvider()
      .getModel(ScenarioController.class);
    stopCondition = scen.getStopCondition();

    simulator.addTickListener(new TickListener() {

      @Override
      public void tick(TimeLapse timeLapse) {}

      @Override
      public void afterTick(TimeLapse timeLapse) {
        if (stopCondition.apply(simulator)) {
          simulator.stop();
        }
      }
    });
    // defaultUICreator = new DefaultUICreator(this);
  }

  /**
   * @return The statistics of the current simulation. Note that calling this
   *         method while the simulation is not yet finished gives the
   *         statistics that were gathered up until that moment.
   */
  public StatisticsDTO getStatistics() {
    return statsTracker.getStatsDTO();
  }

  /**
   * Enables UI using a default visualization.
   */
  // public void enableUI() {
  // enableUI(defaultUICreator);
  // }

  /**
   * Allows to add an additional {@link CanvasRenderer} to the default UI.
   * @param r The {@link CanvasRenderer} to add.
   */
  // public void addRendererToUI(CanvasRenderer r) {
  // defaultUICreator.addRenderer(r);
  // }

  /**
   * Adds a {@link StopConditions} which indicates when the simulation has to
   * stop. The condition is added in an OR fashion to the predefined stop
   * condition of the scenario. So after this method is called the simulation
   * stops if the scenario stop condition is true OR new condition is true.
   * Subsequent invocations of this method will just add more conditions in the
   * same way.
   * @param condition The stop condition to add.
   */
  public void addStopCondition(Predicate<Simulator> condition) {
    stopCondition = Predicates.or(stopCondition, condition);
  }

  // /**
  // * Enables UI by allowing plugging in a custom {@link UICreator}.
  // * @param creator The creator to use.
  // */
  // public void enableUI(UICreator creator) {
  // controller.enableUI(creator);
  // }

  /**
   * Executes a simulation of the problem. When the simulation is finished (and
   * this method returns) the statistics of the simulation are returned.
   * @return The statistics that were gathered during the simulation.
   */
  public StatisticsDTO simulate() {
    // checkState(eventCreatorMap.containsKey(AddVehicleEvent.class),
    // "A creator for AddVehicleEvent is required, use %s.addCreator(..)",
    // this.getClass().getName());
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

  /**
   * Using this method a {@link Creator} instance can be associated with a
   * certain event. The creator will be called when the event is issued, it is
   * the responsibility of the {@link Creator} the create the appropriate
   * response. This method will override a previously existing creator for the
   * specified event type if applicable.
   * @param eventType The event type to which the creator will be associated.
   * @param creator The creator that will be used.
   * @param <T> The type of the event.
   */
  // public <T extends TimedEvent> void addCreator(Class<T> eventType,
  // Creator<T> creator) {
  // checkArgument(
  // eventType == AddVehicleEvent.class || eventType == AddParcelEvent.class
  // || eventType == AddDepotEvent.class,
  // "A creator can only be added to one of the following classes: AddVehicleEvent, AddParcelEvent, AddDepotEvent.");
  // eventCreatorMap.put(eventType, creator);
  // }

  static StatisticsDTO getStats(Simulator sim) {
    final StatsTracker t = sim.getModelProvider().tryGetModel(
      StatsTracker.class);
    if (t == null) {
      throw new IllegalStateException("No stats tracker found!");
    }
    return t.getStatsDTO();
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

  /**
   * This class contains default stop conditions which can be used in the
   * problem. If you want to create your own stop condition you can do it in the
   * following way:
   *
   * <pre>
   * Predicate&lt;SimulationInfo&gt; sc = new Predicate&lt;SimulationInfo&gt;() {
   *   &#064;Override
   *   public boolean apply(SimulationInfo context) {
   *     return true; // &lt;- insert your own condition here
   *   }
   * };
   * </pre>
   *
   * StopConditions can be combined into more complex conditions by using
   * {@link Predicates#and(Predicate, Predicate)},
   * {@link Predicates#or(Predicate, Predicate)} and
   * {@link Predicates#not(Predicate)}.
   * @author Rinde van Lon
   */
  public enum StopConditions implements Predicate<Simulator> {

    /**
     * The simulation is terminated once the
     * {@link com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent#TIME_OUT}
     * event is dispatched.
     */
    TIME_OUT_EVENT {
      @Override
      public boolean apply(@Nullable Simulator context) {
        assert context != null;
        return getStats(context).simFinish;
      }
    },

    /**
     * The simulation is terminated as soon as all the vehicles are back at the
     * depot, note that this can be before or after the
     * {@link com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent#TIME_OUT}
     * event is dispatched.
     */
    VEHICLES_DONE_AND_BACK_AT_DEPOT {
      @Override
      public boolean apply(@Nullable Simulator context) {
        assert context != null;
        final StatisticsDTO stats = getStats(context);

        return stats.totalVehicles == stats.vehiclesAtDepot
          && stats.movedVehicles > 0
          && stats.totalParcels == stats.totalDeliveries;
      }
    },

    /**
     * The simulation is terminated as soon as any tardiness occurs.
     */
    ANY_TARDINESS {
      @Override
      public boolean apply(@Nullable Simulator context) {
        assert context != null;
        final StatisticsDTO stats = getStats(context);
        return stats.pickupTardiness > 0
          || stats.deliveryTardiness > 0;
      }
    };
  }

  /**
   * This is an immutable state object which is exposed to stopconditions.
   * @author Rinde van Lon
   */
  public static class SimulationInfo {
    /**
     * The current statistics.
     */
    public final StatisticsDTO stats;

    /**
     * The scenario which is playing.
     */
    public final Scenario scenario;

    /**
     * Instantiate a new instance using statistics and scenario.
     * @param st Statistics.
     * @param scen Scenario.
     */
    protected SimulationInfo(StatisticsDTO st, Scenario scen) {
      stats = st;
      scenario = scen;
    }
  }

  /**
   * A default {@link UICreator} used for creating a UI for a problem.
   * @author Rinde van Lon
   */
  // public static class DefaultUICreator implements UICreator {
  // private static final double DEFAULT_RENDER_MARGIN = .05;
  //
  // /**
  // * A list of renderers.
  // */
  // protected final List<Renderer> renderers;
  //
  // /**
  // * The speedup that is passed to the gui.
  // */
  // protected final int speedup;
  //
  // private final DynamicPDPTWProblem problem;
  //
  // /**
  // * Create a GUI for the specified problem.
  // * @param prob The problem to create a GUI for.
  // */
  // public DefaultUICreator(DynamicPDPTWProblem prob) {
  // this(prob, 1);
  // }

  /**
   * Create a GUI for the specified problem with the specified speed.
   * @param prob The problem to create a GUI for.
   * @param speed The speed to use.
   */
  // public DefaultUICreator(DynamicPDPTWProblem prob, int speed) {
  // checkArgument(speed >= 1, "speed must be a positive integer");
  // speedup = speed;
  // problem = prob;
  // renderers = newArrayList();
  // }
  //
  // /**
  // * @return The default road model renderer.
  // */
  // protected Renderer planeRoadModelRenderer() {
  // return new PlaneRoadModelRenderer(DEFAULT_RENDER_MARGIN);
  // }
  //
  // /**
  // * @return The default road user renderer.
  // */
  // protected Renderer roadUserRenderer() {
  // final UiSchema schema = new UiSchema(false);
  // schema.add(Vehicle.class, SWT.COLOR_RED);
  // schema.add(Depot.class, SWT.COLOR_CYAN);
  // schema.add(Parcel.class, SWT.COLOR_BLUE);
  // return new RoadUserRenderer(schema, false);
  // }
  //
  // /**
  // * @return The default pdp model renderer.
  // */
  // protected Renderer pdpModelRenderer() {
  // return new PDPModelRenderer(false);
  // }
  //
  // /**
  // * Initializes all renderers.
  // */
  // protected void initRenderers() {
  // renderers.add(planeRoadModelRenderer());
  // renderers.add(roadUserRenderer());
  // renderers.add(pdpModelRenderer());
  // renderers.add(new StatsPanel(problem.statsTracker));
  // }
  //
  // @Override
  // public void createUI(Simulator sim) {
  // initRenderers();
  // View.create(sim).with(renderers.toArray(new Renderer[] {}))
  // .setSpeedUp(speedup).show();
  // }
  //
  // /**
  // * Add a renderer.
  // * @param r The renderer to add.
  // */
  // public void addRenderer(Renderer r) {
  // renderers.add(r);
  // }
  // }
}
