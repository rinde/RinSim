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
package com.github.rinde.rinsim.scenario;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.core.model.time.ClockController;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.scenario.Scenario.ProblemClass;
import com.github.rinde.rinsim.scenario.ScenarioController.StopModel;
import com.github.rinde.rinsim.scenario.StopCondition.TypeProvider;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.FluentIterable;
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
    SCENARIO_FINISHED,

    /**
     * Dispatched when a scenario event has been dispatched and handled.
     * @see ScenarioEvent
     */
    SCENARIO_EVENT;
  }

  final Scenario scenario;
  final Queue<TimedEvent> scenarioQueue;
  final EventDispatcher disp;
  final SimulatorAPI simulator;
  final ClockController clock;
  final ImmutableMap<Class<? extends TimedEvent>, TimedEventHandler<?>> handlers;
  @Nullable
  StopModel stopModel;
  boolean endOfScenario;
  @Nullable
  private EventType status;
  private int ticks;

  ScenarioController(SimulatorAPI sim, ClockController c, Scenario s,
      ImmutableMap<Class<? extends TimedEvent>, TimedEventHandler<?>> m,
      int t) {
    simulator = sim;
    clock = c;
    ticks = t;

    scenario = s;
    scenarioQueue = scenario.asQueue();

    handlers = m;

    disp = new EventDispatcher(EventType.values());

    final ScenarioController sc = this;
    clock.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        if (clock.getCurrentTime() == 0) {
          dispatchSetupEvents();
        }
        if (sc.endOfScenario) {
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
    while ((e = scenarioQueue.peek()) != null && e.getTime() < 0) {
      scenarioQueue.poll();
      dispatch(e);
    }
  }

  /**
   * @return The {@link Scenario#getProblemClass()} of the scenario controlled
   *         by this controller.
   */
  public ProblemClass getScenarioProblemClass() {
    return scenario.getProblemClass();
  }

  /**
   * @return The {@link Scenario#getProblemInstanceId()} of the scenario
   *         controlled by this controller.
   */
  public String getScenarioId() {
    return scenario.getProblemInstanceId();
  }

  @SuppressWarnings("unchecked")
  <T extends TimedEvent> void dispatch(T e) {
    ((TimedEventHandler<T>) handlers.get(e.getClass())).handleTimedEvent(e,
      simulator);

    disp.dispatchEvent(new ScenarioEvent(e));
  }

  /**
   * @return <code>true</code> if all events of this scenario have been
   *         dispatched, <code>false</code> otherwise.
   */
  public boolean isScenarioFinished() {
    return scenarioQueue.isEmpty();
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    if (endOfScenario) {
      return;
    }
    if (ticks == 0) {
      stopClock(timeLapse);
    }
    if (LOGGER.isDebugEnabled() && ticks >= 0) {
      LOGGER.debug("ticks to end: " + ticks);
    }
    if (ticks > 0) {
      ticks--;
    }
    dispatchEvents(timeLapse);

    if (ticks == 0 && status == EventType.SCENARIO_FINISHED) {
      stopClock(timeLapse);
      endOfScenario = true;
    }
  }

  private void dispatchEvents(TimeLapse timeLapse) {
    TimedEvent e = null;

    while ((e = scenarioQueue.peek()) != null
      && e.getTime() <= timeLapse.getTime()) {
      scenarioQueue.poll();
      if (status == null) {
        LOGGER.info("scenario started at virtual time:" + timeLapse.getTime());
        status = EventType.SCENARIO_STARTED;
        disp.dispatchEvent(new Event(status, this));
      }
      dispatch(e);
    }

    if ((e = scenarioQueue.peek()) != null
      && e.getTime() <= timeLapse.getTime() + timeLapse.getTickLength()
      && clock instanceof RealtimeClockController) {
      LOGGER.trace("Found an event in next tick, switch to RT");
      ((RealtimeClockController) clock).switchToRealTime();
    }

    if (e == null && status != EventType.SCENARIO_FINISHED) {
      status = EventType.SCENARIO_FINISHED;
      disp.dispatchEvent(new Event(status, this));
    }
  }

  private void stopClock(TimeLapse timeLapse) {
    LOGGER.info("scenario finished at virtual time:" + timeLapse.getTime()
      + "[stopping simulation]");
    clock.stop();
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {
    if (verifyNotNull(stopModel).evaluate()) {
      clock.stop();
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
   * Event that indicates that a {@link TimedEvent} has just been dispatched and
   * handled.
   * @author Rinde van Lon
   */
  public static final class ScenarioEvent extends Event {
    private final TimedEvent event;

    ScenarioEvent(TimedEvent te) {
      super(EventType.SCENARIO_EVENT);
      event = te;
    }

    /**
     * @return The {@link TimedEvent}.
     */
    public TimedEvent getTimedEvent() {
      return event;
    }

    @Override
    public int hashCode() {
      return Objects.hash(event);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (other == null || other.getClass() != getClass()) {
        return false;
      }
      final ScenarioEvent o = (ScenarioEvent) other;
      return Objects.equals(o.event, event);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(ScenarioEvent.class)
        .add("event", event)
        .toString();
    }
  }

  /**
   *
   * @author Rinde van Lon
   *
   */
  @AutoValue
  public abstract static class Builder
      extends AbstractModelBuilder<ScenarioController, StopModel>
      implements CompositeModelBuilder<ScenarioController, StopModel> {

    Builder() {
      setProvidingTypes(ScenarioController.class);
      setDependencies(SimulatorAPI.class, ClockController.class);
    }

    abstract Scenario getScenario();

    abstract ImmutableMap<Class<? extends TimedEvent>, TimedEventHandler<?>> getEventHandlers();

    abstract int getNumberOfTicks();

    abstract StopModelBuilder getStopModelBuilder();

    abstract boolean isIgnoreRedundantHandlers();

    /**
     * Add a {@link TimedEventHandler} to the controller that handles
     * {@link TimedEvent}s of the specified type.
     * @param type The type of event to handle.
     * @param handler The handler that handles the event.
     * @param <T> The type of event to handle.
     * @return A new {@link Builder} instance.
     * @throws IllegalArgumentException If an interface class is provided.
     */
    @CheckReturnValue
    public <T extends TimedEvent> Builder withEventHandler(Class<T> type,
        TimedEventHandler<T> handler) {
      checkHandlerType(type);

      return create(
        getScenario(),
        ImmutableMap
          .<Class<? extends TimedEvent>, TimedEventHandler<?>>builder()
          .putAll(getEventHandlers()).put(type, handler).build(),
        getNumberOfTicks(),
        getStopModelBuilder(), isIgnoreRedundantHandlers());
    }

    /**
     * Adds the map of {@link Class} to {@link TimedEventHandler} to the 
     * builder.
     * @param entries The event handler mapping. 
     * @return A new builder instance with the specified handlers added.
     */
    public Builder withEventHandlers(
        Map<Class<? extends TimedEvent>, TimedEventHandler<?>> entries) {
      for (final Entry<Class<? extends TimedEvent>, TimedEventHandler<?>> entry : entries
        .entrySet()) {
        checkHandlerType(entry.getClass());
      }
      return create(
        getScenario(),
        ImmutableMap
          .<Class<? extends TimedEvent>, TimedEventHandler<?>>builder()
          .putAll(getEventHandlers())
          .putAll(entries)
          .build(),
        getNumberOfTicks(),
        getStopModelBuilder(), isIgnoreRedundantHandlers());
    }

    static void checkHandlerType(Class<?> type) {
      checkArgument(!type.isInterface(),
        "Must handle a concrete class, not: %s.", type);
    }

    /**
     * Change the behavior of handling redundant handlers. A redundant handler
     * is a {@link TimedEventHandler} that handles an {@link TimedEvent} type
     * that does not occur in the specified {@link Scenario}, it is therefore
     * redundant. By default, adding a redundant {@link TimedEventHandler}
     * yields a {@link IllegalStateException}. By calling this method with
     * <code>true</code> this exception can be suppressed.
     * @param ignore If <code>true</code> redundant handlers are ignored,
     *          otherwise redundant handlers will generate a
     *          {@link IllegalStateException}.
     * @return A new {@link Builder} instance.
     */
    @CheckReturnValue
    public Builder withIgnoreRedundantHandlers(boolean ignore) {
      return create(getScenario(), getEventHandlers(), getNumberOfTicks(),
        getStopModelBuilder(), ignore);
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
        getStopModelBuilder(), isIgnoreRedundantHandlers());
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
          .stopCondition(),
          stp));
      }
      return create(getScenario(), getEventHandlers(), getNumberOfTicks(), smb,
        isIgnoreRedundantHandlers());
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
          .stopCondition(),
          stp));
      }
      return create(getScenario(), getEventHandlers(), getNumberOfTicks(), smb,
        isIgnoreRedundantHandlers());
    }

    @SuppressWarnings("unchecked")
    @Override
    public ScenarioController build(DependencyProvider dependencyProvider) {
      final SimulatorAPI sim = dependencyProvider.get(SimulatorAPI.class);
      final ClockController clockController = dependencyProvider
        .get(ClockController.class);

      final Scenario s = getScenario();
      final Set<Class<?>> required = collectClasses(s.getEvents());
      final Map<Class<? extends TimedEvent>, TimedEventHandler<?>> m =
        newLinkedHashMap(getEventHandlers());
      final Set<Class<? extends TimedEvent>> covered =
        newLinkedHashSet(getEventHandlers().keySet());

      for (final Class<?> c : required) {
        if (!covered.remove(c)) {
          checkState(TimedEvent.class.isAssignableFrom(c.getSuperclass()),
            "No handler found for event %s.", c);
          checkState(covered.remove(c.getSuperclass()),
            "No handler found for event: %s.", c.getSuperclass());

          checkState(m.containsKey(c.getSuperclass()),
            "Cannot place a handler");
          m.put((Class<TimedEvent>) c, m.get(c.getSuperclass()));
          m.remove(c.getSuperclass());
        }
      }
      checkState(isIgnoreRedundantHandlers() || covered.isEmpty(),
        "Found redundant event handlers for event type(s): %s, no event with "
          + "these type(s) was found. All added handlers: %s, all event types"
          + " in the scenario: %s. Scenario (problem class:'%s', instance "
          + "id:'%s').",
        covered, m.entrySet(), required, s.getProblemClass(),
        s.getProblemInstanceId());
      return new ScenarioController(sim, clockController, s,
        ImmutableMap.copyOf(m), getNumberOfTicks());
    }

    @Override
    public ImmutableSet<ModelBuilder<?, ?>> getChildren() {
      return ImmutableSet.<ModelBuilder<?, ?>>builder()
        .addAll(getScenario().getModelBuilders())
        .add(getStopModelBuilder())
        .build();
    }

    private static ImmutableSet<Class<?>> collectClasses(
        Iterable<? extends TimedEvent> objs) {
      return FluentIterable.from(objs).transform(ToClassFunc.INSTANCE).toSet();
    }

    enum ToClassFunc implements Function<Object, Class<?>> {
      INSTANCE {
        @Override
        @Nullable
        public Class<?> apply(@Nullable Object input) {
          return verifyNotNull(input).getClass();
        }
      }
    }

    static Builder create(Scenario scen) {
      final int ticks = scen.getTimeWindow().end() == Long.MAX_VALUE ? -1
        : (int) (scen.getTimeWindow().end() - scen.getTimeWindow().begin());

      return create(
        scen,
        ImmutableMap.<Class<? extends TimedEvent>, TimedEventHandler<?>>of(),
        ticks,
        StopModelBuilder.create(scen.getStopCondition()), false);
    }

    static Builder create(Scenario scen,
        ImmutableMap<Class<? extends TimedEvent>, TimedEventHandler<?>> handlers,
        int ticks,
        StopModelBuilder stop, boolean ignoreRedundantHandlers) {
      return new AutoValue_ScenarioController_Builder(scen, handlers, ticks,
        stop, ignoreRedundantHandlers);
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
      final ImmutableClassToInstanceMap.Builder<Object> b =
        ImmutableClassToInstanceMap
          .builder();
      for (final Class<?> c : dependencies()) {
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
