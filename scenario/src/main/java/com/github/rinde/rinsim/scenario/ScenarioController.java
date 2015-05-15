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
package com.github.rinde.rinsim.scenario;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.CompositeModelBuilder;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.core.model.time.ClockController;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.pdptw.DefaultDepot;
import com.github.rinde.rinsim.core.pdptw.DefaultParcel;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.scenario.ScenarioController.StopModel;
import com.github.rinde.rinsim.scenario.StopCondition.TypeProvider;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * A scenario controller represents a single simulation run using a
 * {@link Scenario}. The scenario controller makes sure that all events in the
 * scenario are dispatched at their respective time and it checks whether they
 * are handled.
 *
 * @author Rinde van Lon
 * @author Bartosz Michalik
 * @since 2.0
 */
public final class ScenarioController extends AbstractModel<StopModel>
  implements TickListener {
  /**
   * Logger for this class.
   */
  static final Logger LOGGER = LoggerFactory
    .getLogger(ScenarioController.class);

  /**
   * The {@link Event} types which can be dispatched by this class.
   * @author Rinde van Lon
   */
  public enum EventType {
    /**
     * Dispatched when the scenario starts playing.
     */
    SCENARIO_STARTED,
    /**
     * Dispatched when the scenario has finished playing.
     */
    SCENARIO_FINISHED;
  }

  final Scenario scenario;
  final Queue<TimedEvent> scenarioQueue;
  final EventDispatcher disp;
  final SimulatorAPI simulator;
  final ClockController clock;
  private int ticks;
  @Nullable
  StopModel stopModel;
  @Nullable
  private EventType status;

  ScenarioController(SimulatorAPI sim, ClockController c, Scenario s,
    ImmutableMap<Class<?>, TimedEventHandler<?>> m, int t) {
    simulator = sim;
    clock = c;
    ticks = t;

    scenario = s;
    scenarioQueue = scenario.asQueue();

    final Set<Enum<?>> typeSet = newHashSet(scenario.getPossibleEventTypes());
    typeSet.addAll(asList(EventType.values()));
    disp = new EventDispatcher(typeSet);

    disp.addListener(new InternalListener(sim, m),
      scenario.getPossibleEventTypes());

    final ScenarioController sc = this;
    clock.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        if (clock.getCurrentTime() == 0) {
          dispatchSetupEvents();
        }
        if (sc.stop) {
          clock.stop();
        }
      }
    }, Clock.ClockEventType.STARTED);

  }

  /**
   * Provides access to the {@link Event} API, allows adding and removing
   * {@link Listener}s that are notified when {@link ScenarioController}
   * dispatches {@link Event}s.
   * @return The event API of the scenario controller.
   */
  public EventAPI getEventAPI() {
    return disp.getPublicEventAPI();
  }

  /**
   * Dispatch all setup events (the ones that define initial settings). For
   * example, a vehicle that is added during setup (at time &lt; 0) will receive
   * its first tick at time 0. If the vehicle is added at the beginning of the
   * simulation (time 0) the first tick it will receive will be the second
   * (globally) tick.
   */
  protected void dispatchSetupEvents() {
    TimedEvent e = null;
    while ((e = scenarioQueue.peek()) != null && e.time < 0) {
      scenarioQueue.poll();
      disp.dispatchEvent(e);
    }
  }

  /**
   * @return <code>true</code> if all events of this scenario have been
   *         dispatched, <code>false</code> otherwise.
   */
  public boolean isScenarioFinished() {
    return scenarioQueue.isEmpty();
  }

  boolean stop = false;

  @Override
  public final void tick(TimeLapse timeLapse) {
    if (stop) {
      return;
    }
    if (ticks == 0) {
      LOGGER.info("scenario finished at virtual time:" + timeLapse.getTime()
        + "[stopping simulation]");
      clock.stop();
    }
    if (LOGGER.isDebugEnabled() && ticks >= 0) {
      LOGGER.debug("ticks to end: " + ticks);
    }
    if (ticks > 0) {
      ticks--;
    }
    TimedEvent e = null;

    while ((e = scenarioQueue.peek()) != null && e.time <= timeLapse.getTime()) {
      scenarioQueue.poll();
      if (status == null) {
        LOGGER.info("scenario started at virtual time:" + timeLapse.getTime());
        status = EventType.SCENARIO_STARTED;
        disp.dispatchEvent(new Event(status, this));
      }
      disp.dispatchEvent(e);
    }
    if (e == null && status != EventType.SCENARIO_FINISHED) {
      status = EventType.SCENARIO_FINISHED;
      disp.dispatchEvent(new Event(status, this));
    }
    if (ticks == 0 && status == EventType.SCENARIO_FINISHED) {
      LOGGER.info("scenario finished at virtual time:" + timeLapse.getTime()
        + "[stopping simulation]");
      clock.stop();
      stop = true;
    }

  }

  @Override
  public void afterTick(TimeLapse timeLapse) {
    if (verifyNotNull(stopModel).evaluate()) {
      clock.stop();
    }
  }

  static class InternalListener implements Listener {
    final SimulatorAPI simulator;
    final ImmutableMap<Class<?>, TimedEventHandler<?>> handlers;

    InternalListener(SimulatorAPI sim,
      ImmutableMap<Class<?>, TimedEventHandler<?>> h) {
      simulator = sim;
      handlers = h;
    }

    @Override
    public void handleEvent(Event e) {
      if (e.getEventType() == PDPScenarioEvent.TIME_OUT) {
        return;
      }
      checkState(handlers.containsKey(e.getClass()),
        "The event %s with event type %s is not handled.", e.getClass(),
        e.getEventType());
      handle(handlers.get(e.getClass()), e);
    }

    @SuppressWarnings("unchecked")
    <T extends TimedEvent> void handle(TimedEventHandler<T> h, Event e) {
      h.handleTimedEvent((T) e, simulator);
    }
  }

  @Override
  public boolean register(StopModel element) {
    stopModel = element;
    return false;
  }

  @Deprecated
  @Override
  public boolean unregister(StopModel element) {
    throw new UnsupportedOperationException(
      "A stop condition can not be unregistered.");
  }

  @Override
  public <U> U get(Class<U> type) {
    return type.cast(this);
  }

  /**
   * Creates a {@link Builder} for {@link ScenarioController}.
   * @param scenario The scenario to control.
   * @return A new {@link Builder}.
   */
  public static Builder builder(Scenario scenario) {
    return Builder.create(scenario);
  }

  /**
   *
   * @author Rinde van Lon
   *
   */
  @AutoValue
  public abstract static class Builder extends
    AbstractModelBuilder<ScenarioController, StopModel> implements
    CompositeModelBuilder<ScenarioController, StopModel> {

    static final TimedEventHandler<AddParcelEvent> DEFAULT_ADD_PARCEL_HANDLER = new TimedEventHandler<AddParcelEvent>() {
      @Override
      public void handleTimedEvent(AddParcelEvent event, SimulatorAPI sim) {
        sim.register(new DefaultParcel(event.parcelDTO));
      }
    };
    static final TimedEventHandler<AddDepotEvent> DEFAULT_ADD_DEPOT_HANDLER = new TimedEventHandler<AddDepotEvent>() {
      @Override
      public void handleTimedEvent(AddDepotEvent event, SimulatorAPI sim) {
        sim.register(new DefaultDepot(event.position));
      }
    };

    Builder() {
      setProvidingTypes(ScenarioController.class);
      setDependencies(SimulatorAPI.class, ClockController.class);
    }

    abstract Scenario getScenario();

    abstract ImmutableMap<Class<?>, TimedEventHandler<?>> getEventHandlers();

    abstract int getNumberOfTicks();

    abstract StopModelBuilder getStopModelBuilder();

    @CheckReturnValue
    public <T extends TimedEvent> Builder withEventHandler(Class<T> type,
      TimedEventHandler<T> handler) {
      return create(
        getScenario(),
        ImmutableMap.<Class<?>, TimedEventHandler<?>> builder()
          .putAll(getEventHandlers()).put(type, handler).build(),
        getNumberOfTicks(),
        getStopModelBuilder());
    }

    @CheckReturnValue
    public Builder withEventHandlers(Map<Class<?>, TimedEventHandler<?>> m) {
      return create(
        getScenario(),
        ImmutableMap.<Class<?>, TimedEventHandler<?>> builder()
          .putAll(getEventHandlers()).putAll(m).build(),
        getNumberOfTicks(),
        getStopModelBuilder());
    }

    /**
     * Limits the simulation to the specified number of ticks.
     * @param ticks The number of ticks run, when negative the number of ticks
     *          is infinite.
     * @return A new {@link Builder} instance.
     */
    @CheckReturnValue
    public Builder withNumberOfTicks(int ticks) {
      return create(getScenario(), getEventHandlers(), ticks,
        getStopModelBuilder());
    }

    /**
     * Adds an additional stop condition to the controller in AND fashion. The
     * first stop condition is defined by {@link Scenario#getStopCondition()}.
     * @param stp The builder that constructs the {@link StopCondition}.
     * @return A new {@link Builder} instance.
     * @see StopConditions
     */
    @CheckReturnValue
    public Builder withAndStopCondition(StopCondition stp) {
      final StopModelBuilder smb;
      if (getStopModelBuilder().stopCondition().equals(
        StopConditions.alwaysFalse())) {
        smb = StopModelBuilder.create(stp);
      } else {
        smb = StopModelBuilder.create(StopConditions.and(getStopModelBuilder()
          .stopCondition(), stp));
      }
      return create(getScenario(), getEventHandlers(), getNumberOfTicks(), smb);
    }

    /**
     * Adds an additional stop condition to the controller in OR fashion. The
     * first stop condition is defined by {@link Scenario#getStopCondition()}.
     * @param stp The builder that constructs the {@link StopCondition}.
     * @return A new {@link Builder} instance.
     * @see StopConditions
     */
    @CheckReturnValue
    public Builder withOrStopCondition(StopCondition stp) {
      final StopModelBuilder smb;
      if (getStopModelBuilder().stopCondition().equals(
        StopConditions.alwaysFalse())) {
        smb = StopModelBuilder.create(stp);
      } else {
        smb = StopModelBuilder.create(StopConditions.or(getStopModelBuilder()
          .stopCondition(), stp));
      }
      return create(getScenario(), getEventHandlers(), getNumberOfTicks(), smb);
    }

    @Override
    public ScenarioController build(DependencyProvider dependencyProvider) {
      SimulatorAPI sim = dependencyProvider.get(SimulatorAPI.class);
      ClockController clock = dependencyProvider.get(ClockController.class);

      ImmutableMap.Builder<Class<?>, TimedEventHandler<?>> b = ImmutableMap
        .builder();
      b.putAll(getEventHandlers());
      if (!getEventHandlers().containsKey(AddDepotEvent.class)) {
        b.put(AddDepotEvent.class, DEFAULT_ADD_DEPOT_HANDLER);
      }
      if (!getEventHandlers().containsKey(AddParcelEvent.class)) {
        b.put(AddParcelEvent.class, DEFAULT_ADD_PARCEL_HANDLER);
      }
      ImmutableMap<Class<?>, TimedEventHandler<?>> m = b.build();

      // TODO it needs to be checked whether all events that occur in the
      // scenario are handled

      return new ScenarioController(sim, clock, getScenario(), m,
        getNumberOfTicks());
    }

    @Override
    public ImmutableSet<ModelBuilder<?, ?>> getChildren() {
      return ImmutableSet.<ModelBuilder<?, ?>> builder()
        .addAll(getScenario().getModelBuilders())
        .add(getStopModelBuilder())
        .build();
    }

    static Builder create(Scenario scen) {
      final int ticks = scen.getTimeWindow().end == Long.MAX_VALUE ? -1
        : (int) (scen.getTimeWindow().end - scen.getTimeWindow().begin);

      return create(
        scen,
        ImmutableMap.<Class<?>, TimedEventHandler<?>> of(),
        ticks,
        StopModelBuilder.create(scen.getStopCondition()));
    }

    static Builder create(Scenario scen,
      ImmutableMap<Class<?>, TimedEventHandler<?>> handlers, int ticks,
      StopModelBuilder stop) {
      return new AutoValue_ScenarioController_Builder(scen, handlers, ticks,
        stop);
    }
  }

  static class StopModel extends AbstractModelVoid {
    final StopCondition stopCondition;
    final TypeProvider provider;

    StopModel(StopCondition sc, ImmutableClassToInstanceMap<Object> map) {
      stopCondition = sc;
      provider = new MapTypeProvider(map);
    }

    boolean evaluate() {
      return stopCondition.evaluate(provider);
    }
  }

  static class MapTypeProvider implements TypeProvider {
    final ImmutableClassToInstanceMap<Object> instanceMap;

    MapTypeProvider(ImmutableClassToInstanceMap<Object> m) {
      instanceMap = m;
    }

    @Override
    public <T> T get(Class<T> type) {
      return verifyNotNull(instanceMap.getInstance(type));
    }
  }

  @AutoValue
  abstract static class StopModelBuilder extends
    AbstractModelBuilder<StopModel, Void> {

    abstract StopCondition stopCondition();

    abstract ImmutableSet<Class<?>> dependencies();

    @Override
    public StopModel build(DependencyProvider dependencyProvider) {
      ImmutableClassToInstanceMap.Builder<Object> b = ImmutableClassToInstanceMap
        .builder();
      for (Class<?> c : dependencies()) {
        put(b, c, dependencyProvider);
      }
      return new StopModel(stopCondition(), b.build());
    }

    StopModelBuilder init() {
      setDependencies(dependencies());
      return this;
    }

    // helper method for dealing with generics
    static <T> void put(ImmutableClassToInstanceMap.Builder<Object> b,
      Class<T> c, DependencyProvider dp) {
      b.put(c, dp.get(c));
    }

    static StopModelBuilder create(StopCondition sc) {
      return new AutoValue_ScenarioController_StopModelBuilder(sc,
        sc.getTypes()).init();
    }
  }
}
