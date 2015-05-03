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
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.CompositeModelBuilder;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
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
import com.google.auto.value.AutoValue;
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
public final class ScenarioController extends AbstractModel<Scenario> implements
  TickListener {
  /**
   * Logger for this class.
   */
  protected static final Logger LOGGER = LoggerFactory
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
  private EventType status;

  /**
   * Create an instance of ScenarioController with defined {@link Scenario} and
   * number of ticks till end. If the number of ticks is negative the simulator
   * will run until the {@link Simulator#stop()} method is called. TODO refine
   * documentation
   *
   * @param scen Scenario which is controlled.
   * @param eventHandler Is used to handle scenario events.
   * @param numberOfTicks The number of ticks play, when negative the number of
   *          tick is infinite.
   */
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
  public void afterTick(TimeLapse timeLapse) {}

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
  public boolean register(Scenario element) {
    return false;
  }

  @Override
  public boolean unregister(Scenario element) {
    return false;
  }

  @Override
  public <U> U get(Class<U> type) {
    return type.cast(this);
  }

  public static Builder builder(Scenario scenario) {
    return Builder.create(scenario);
  }

  @AutoValue
  public abstract static class Builder extends
    AbstractModelBuilder<ScenarioController, Scenario> implements
    CompositeModelBuilder<ScenarioController, Scenario> {

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

    @CheckReturnValue
    public <T extends TimedEvent> Builder withEventHandler(Class<T> type,
      TimedEventHandler<T> handler) {
      return create(
        getScenario(),
        ImmutableMap.<Class<?>, TimedEventHandler<?>> builder()
          .putAll(getEventHandlers()).put(type, handler).build(),
        getNumberOfTicks());
    }

    @CheckReturnValue
    public Builder withEventHandlers(Map<Class<?>, TimedEventHandler<?>> m) {
      return create(
        getScenario(),
        ImmutableMap.<Class<?>, TimedEventHandler<?>> builder()
          .putAll(getEventHandlers()).putAll(m).build(),
        getNumberOfTicks());
    }

    @CheckReturnValue
    public Builder withNumberOfTicks(int ticks) {
      return create(getScenario(), getEventHandlers(), ticks);
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

      // TODO it needs to be checked whether all events that occur in the
      // scenario are handled

      return new ScenarioController(sim, clock, getScenario(), b.build(),
        getNumberOfTicks());
    }

    @Override
    public ImmutableSet<ModelBuilder<?, ?>> getChildren() {
      return getScenario().getModelBuilders();
    }

    static Builder create(Scenario scen) {
      return create(
        scen,
        ImmutableMap.<Class<?>, TimedEventHandler<?>> of()
        , -1);
    }

    static Builder create(Scenario scen,
      ImmutableMap<Class<?>, TimedEventHandler<?>> handlers, int ticks) {
      return new AutoValue_ScenarioController_Builder(scen, handlers, ticks);
    }

  }
}
