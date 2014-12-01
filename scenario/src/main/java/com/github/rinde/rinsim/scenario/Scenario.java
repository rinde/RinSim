/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.scenario.TimedEvent.TimeComparator;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Scenario is an unmodifiable list of events sorted by the time stamp. To
 * obtain an instance there are a number of builder methods available such as
 * {@link #builder()}.
 * @author Rinde van Lon 
 * @author Bartosz Michalik 
 */
public abstract class Scenario {
  /**
   * The default {@link ProblemClass}.
   */
  public static final ProblemClass DEFAULT_PROBLEM_CLASS = new SimpleProblemClass(
      "DEFAULT");
  private final ImmutableList<TimedEvent> events;
  private final ImmutableSet<Enum<?>> supportedTypes;

  /**
   * Creates a new empty instance with no events or event types.
   */
  protected Scenario() {
    events = ImmutableList.of();
    supportedTypes = ImmutableSet.of();
  }

  /**
   * Creates a new instance using a copy of the specified events and event
   * types.
   * @param evs A list of event sorted by time. It is the caller's
   *          responsibility to ensure that the events are sorted, this is not
   *          checked.
   * @param ts A set of event types, it must contain at least all event types of
   *          the events.
   */
  protected Scenario(List<? extends TimedEvent> evs, Set<Enum<?>> ts) {
    supportedTypes = ImmutableSet.copyOf(ts);
    events = ImmutableList.copyOf(evs);
  }

  /**
   * Return a scenario as a list of (time sorted) events.
   * @return the list of events.
   */
  public ImmutableList<TimedEvent> asList() {
    return events;
  }

  /**
   * @return A queue containing all events of this scenario.
   */
  public Queue<TimedEvent> asQueue() {
    return newLinkedList(events);
  }

  /**
   * @return The number of events that is in this scenario.
   */
  public int size() {
    return events.size();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    if (other.getClass() != getClass()) {
      return false;
    }
    return events.equals(((Scenario) other).events);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(supportedTypes, events);
  }

  /**
   * Specify event types that can occur in a scenario. The events added to
   * scenario are checked for the event type.
   * @return event types
   */
  public Set<Enum<?>> getPossibleEventTypes() {
    return supportedTypes;
  }

  /**
   * @return Should return a list of newly created {@link Model}s which will be
   *         used for simulating this scenario.
   */
  public abstract ImmutableList<? extends Supplier<? extends Model<?>>> getModelSuppliers();

  /**
   * @return The {@link TimeWindow} of the scenario indicates the start and end
   *         of scenario.
   */
  public abstract TimeWindow getTimeWindow();

  /**
   * @return The size of a tick.
   */
  public abstract long getTickSize();

  /**
   * @return The stop condition indicating when a simulation should end.
   */
  public abstract Predicate<Simulator> getStopCondition();

  /**
   * @return The time unit used in the simulator.
   */
  public abstract Unit<Duration> getTimeUnit();

  /**
   * @return The speed unit used in the
   *         {@link com.github.rinde.rinsim.core.model.road.RoadModel}.
   */
  public abstract Unit<Velocity> getSpeedUnit();

  /**
   * @return The distance unit used in the
   *         {@link com.github.rinde.rinsim.core.model.road.RoadModel}.
   */
  public abstract Unit<Length> getDistanceUnit();

  /**
   * @return The 'class' to which this scenario belongs.
   */
  public abstract ProblemClass getProblemClass();

  /**
   * @return The instance id of this scenario.
   */
  public abstract String getProblemInstanceId();

  /**
   * Finds all event types in the provided events.
   * @param pEvents The events to check.
   * @return A set of event types.
   */
  protected static ImmutableSet<Enum<?>> collectEventTypes(
      Collection<? extends TimedEvent> pEvents) {
    final Set<Enum<?>> types = newLinkedHashSet();
    for (final TimedEvent te : pEvents) {
      types.add(te.getEventType());
    }
    return ImmutableSet.copyOf(types);
  }

  /**
   * @return A new {@link Builder} instance with {@link #DEFAULT_PROBLEM_CLASS}.
   */
  public static Builder builder() {
    return builder(DEFAULT_PROBLEM_CLASS);
  }

  /**
   * Copying builder. Creates a new builder that builds instances with the same
   * properties as the specified scenario.
   * @param scenario The scenario from which properties will be copied.
   * @return A new {@link Builder} instance.
   */
  public static Builder builder(Scenario scenario) {
    return builder(scenario.getProblemClass()).copyProperties(scenario);
  }

  /**
   * Create a {@link Builder} to construct {@link Scenario} instances.
   * @param problemClass The problem class of the instance to construct.
   * @return A new {@link Builder} instance.
   */
  public static Builder builder(ProblemClass problemClass) {
    return new Builder(problemClass);
  }

  /**
   * Creates a new {@link Builder} based on an existing builder. All properties
   * will be copied from the specified builder into the newly created builder.
   * @param base The builder to copy properties from.
   * @param problemClass The {@link ProblemClass} the new builder should have.
   * @return The newly constructed {@link Builder}.
   */
  public static Builder builder(AbstractBuilder<?> base,
      ProblemClass problemClass) {
    return new Builder(Optional.<AbstractBuilder<?>> of(base), problemClass);
  }

  /**
   * Represents a class of scenarios.
   * @author Rinde van Lon 
   */
  public interface ProblemClass {
    /**
     * @return The id of this problem class.
     */
    String getId();
  }

  /**
   * String based implementation of {@link ProblemClass}.
   * @author Rinde van Lon 
   */
  public static final class SimpleProblemClass implements ProblemClass {
    private final String id;

    /**
     * Create a new instance.
     * @param name The name to use as id.
     */
    public SimpleProblemClass(String name) {
      id = name;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public String toString() {
      return String.format("SimpleProblemClass(%s)", id);
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (null == o || getClass() != o.getClass()) {
        return false;
      }
      return id.equals(((SimpleProblemClass) o).id);
    }
  }

  /**
   * A builder for constructing {@link Scenario} instances.
   * @author Rinde van Lon 
   */
  public static class Builder extends AbstractBuilder<Builder> {
    final List<TimedEvent> eventList;
    final Set<Enum<?>> eventTypeSet;
    final ImmutableList.Builder<Supplier<? extends Model<?>>> modelSuppliers;
    ProblemClass problemClass;
    String instanceId;

    Builder(ProblemClass pc) {
      this(Optional.<AbstractBuilder<?>> absent(), pc);
    }

    Builder(Optional<AbstractBuilder<?>> base, ProblemClass pc) {
      super(base);
      problemClass = pc;
      instanceId = "";
      eventList = newArrayList();
      eventTypeSet = newLinkedHashSet();
      modelSuppliers = ImmutableList.builder();
    }

    /**
     * Add the specified {@link TimedEvent} to the builder.
     * @param event The event to add.
     * @return This, as per the builder pattern.
     */
    public Builder addEvent(TimedEvent event) {
      eventList.add(event);
      return self();
    }

    /**
     * Add the specified {@link TimedEvent}s to the builder.
     * @param events The events to add.
     * @return This, as per the builder pattern.
     */
    public Builder addEvents(Iterable<? extends TimedEvent> events) {
      for (final TimedEvent te : events) {
        addEvent(te);
      }
      return self();
    }

    /**
     * Adds an event type to the builder.
     * @param eventType The event type to add.
     * @return This, as per the builder pattern.
     */
    public Builder addEventType(Enum<?> eventType) {
      eventTypeSet.add(eventType);
      return self();
    }

    /**
     * Adds all specified event types to the builder.
     * @param eventTypes The event types to add.
     * @return This, as per the builder pattern.
     */
    public Builder addEventTypes(Iterable<? extends Enum<?>> eventTypes) {
      for (final Enum<?> e : eventTypes) {
        addEventType(e);
      }
      return self();
    }

    /**
     * The instance id to use for the next scenario that is created.
     * @param id The id to use.
     * @return This, as per the builder pattern.
     */
    public Builder instanceId(String id) {
      instanceId = id;
      return self();
    }

    /**
     * The {@link ProblemClass} to use for the next scenario that is created.
     * @param pc The problem class to use.
     * @return This, as per the builder pattern.
     */
    public Builder problemClass(ProblemClass pc) {
      problemClass = pc;
      return self();
    }

    /**
     * Adds the model supplier. The suppliers will be used to instantiate
     * {@link Model}s needed for the scenario.
     * @param modelSupplier The model supplier to add.
     * @return This, as per the builder pattern.
     */
    public Builder addModel(Supplier<? extends Model<?>> modelSupplier) {
      modelSuppliers.add(modelSupplier);
      return self();
    }

    /**
     * Adds the model suppliers. The suppliers will be used to instantiate
     * {@link Model}s needed for the scenario.
     * @param suppliers The model suppliers to add.
     * @return This, as per the builder pattern.
     */
    public Builder addModels(
        Iterable<? extends Supplier<? extends Model<?>>> suppliers) {
      modelSuppliers.addAll(suppliers);
      return self();
    }

    @Override
    public Builder copyProperties(Scenario scenario) {
      return super.copyProperties(scenario)
          .addEvents(scenario.asList())
          .addEventTypes(scenario.getPossibleEventTypes())
          .problemClass(scenario.getProblemClass())
          .instanceId(scenario.getProblemInstanceId())
          .addModels(scenario.getModelSuppliers());
    }

    /**
     * Removes all events that do not satisfy <code>filter</code>.
     * @param filter The {@link Predicate} that specifies which
     *          {@link TimedEvent}s will be retained.
     * @return This, as per the builder pattern.
     */
    public Builder filterEvents(Predicate<? super TimedEvent> filter) {
      eventList.retainAll(Collections2.filter(eventList, filter));
      return self();
    }

    /**
     * Limits or grows the number of {@link TimedEvent}s that satisfy the
     * specified {@link Predicate} to such that the number of occurrences is
     * equal to <code>frequency</code>. If the number of events that satisfy the
     * <code>filter</code> already equals the specified <code>frequency</code>,
     * nothing happens.
     * <p>
     * <b>Preconditions:</b>
     * <ul>
     * <li>Frequency may not be negative</li>
     * <li>This {@link Builder} must contain at least one event.</li>
     * <li>The specified {@link Predicate} must match at least one event in this
     * {@link Builder}.</li>
     * <li>All events that satisfy the {@link Predicate} must be equal.</li>
     * </ul>
     * <p>
     * <b>Postconditions:</b>
     * <ul>
     * <li>The number of events that match the {@link Predicate} is equal to
     * <code>frequency</code>.</li>
     * </ul>
     * @param filter The filter that determines for which events the frequency
     *          will be ensured.
     * @param frequency The target frequency, may not be negative.
     * @return This, as per the builder pattern.
     */
    public Builder ensureFrequency(Predicate<? super TimedEvent> filter,
        int frequency) {
      checkArgument(frequency >= 0, "Frequency must be >= 0.");
      checkState(!eventList.isEmpty(), "Event list is empty.");
      final FluentIterable<TimedEvent> filtered = FluentIterable
          .from(eventList)
          .filter(filter);
      checkArgument(
          !filtered.isEmpty(),
          "The specified filter did not match any event in the event list (size %s), filter: %s.",
          eventList.size(), filter);
      final Set<TimedEvent> set = filtered.toSet();
      checkArgument(
          set.size() == 1,
          "The specified filter matches multiple non-equal events, all matches must be equal. Events: %s. Filter: %s.",
          set, filter);

      if (filtered.size() > frequency) {
        // limit
        final List<TimedEvent> toAddBack = filtered.limit(frequency).toList();
        eventList.removeAll(set);
        eventList.addAll(toAddBack);
      } else if (filtered.size() < frequency) {
        // grow
        eventList.addAll(Collections.nCopies(frequency - filtered.size(), set
            .iterator().next()));
      }
      return self();
    }

    /**
     * Removes all events.
     * @return This, as per the builder pattern.
     */
    public Builder clearEvents() {
      eventList.clear();
      return self();
    }

    /**
     * Build a new {@link Scenario} instance.
     * @return The new instance.
     */
    public Scenario build() {
      final List<TimedEvent> list = newArrayList(eventList);
      Collections.sort(list, TimeComparator.INSTANCE);
      eventTypeSet.addAll(collectEventTypes(list));
      return new DefaultScenario(this, ImmutableList.copyOf(list),
          ImmutableSet.copyOf(eventTypeSet));
    }

    @Override
    protected Builder self() {
      return this;
    }

    ImmutableList<Supplier<? extends Model<?>>> getModelSuppliers() {
      return modelSuppliers.build();
    }
  }

  /**
   * Abstract builder of {@link Scenario} instances. Provides methods for
   * setting the basic properties of a scenario.
   * 
   * @param <T> The type of concrete builder.
   * @author Rinde van Lon
   */
  public abstract static class AbstractBuilder<T extends AbstractBuilder<T>> {
    static final Unit<Length> DEFAULT_DISTANCE_UNIT = SI.KILOMETER;
    static final Unit<Velocity> DEFAULT_SPEED_UNIT = NonSI.KILOMETERS_PER_HOUR;
    static final Unit<Duration> DEFAULT_TIME_UNIT = SI.MILLI(SI.SECOND);
    static final long DEFAULT_TICK_SIZE = 1000L;
    static final TimeWindow DEFAULT_TIME_WINDOW = new TimeWindow(0,
        8 * 60 * 60 * 1000);
    static final Predicate<Simulator> DEFAULT_STOP_CONDITION = Predicates
        .alwaysFalse();

    /**
     * Defines {@link Scenario#getDistanceUnit()}.
     */
    protected Unit<Length> distanceUnit;

    /**
     * Defines {@link Scenario#getSpeedUnit()}.
     */
    protected Unit<Velocity> speedUnit;

    /**
     * Defines {@link Scenario#getTimeUnit()}.
     */
    protected Unit<Duration> timeUnit;

    /**
     * Defines {@link Scenario#getTickSize()}.
     */
    protected long tickSize;

    /**
     * Defines {@link Scenario#getTimeWindow()}.
     */
    protected TimeWindow timeWindow;

    /**
     * Defines {@link Scenario#getStopCondition()}.
     */
    protected Predicate<Simulator> stopCondition;

    /**
     * Copying constructor. Copies all settings from the specified builder into
     * this instance. If no builder is specified default values will be used.
     * @param copy An existing builder or {@link Optional#absent()}.
     */
    protected AbstractBuilder(Optional<AbstractBuilder<?>> copy) {
      if (copy.isPresent()) {
        distanceUnit = copy.get().distanceUnit;
        speedUnit = copy.get().speedUnit;
        timeUnit = copy.get().timeUnit;
        tickSize = copy.get().tickSize;
        timeWindow = copy.get().timeWindow;
        stopCondition = copy.get().stopCondition;
      }
      else {
        distanceUnit = DEFAULT_DISTANCE_UNIT;
        speedUnit = DEFAULT_SPEED_UNIT;
        timeUnit = DEFAULT_TIME_UNIT;
        tickSize = DEFAULT_TICK_SIZE;
        timeWindow = DEFAULT_TIME_WINDOW;
        stopCondition = DEFAULT_STOP_CONDITION;
      }
    }

    /**
     * Should return 'this', the builder.
     * @return 'this'.
     */
    protected abstract T self();

    /**
     * Set the time unit to use. Possible values include: {@link SI#SECOND},
     * {@link NonSI#HOUR}, etc.
     * @param tu The time unit.
     * @return This, as per the builder pattern.
     */
    public T timeUnit(Unit<Duration> tu) {
      timeUnit = tu;
      return self();
    }

    /**
     * Set the tick size.
     * @param ts The tick size, expressed in the time unit as set by
     *          {@link #timeUnit(Unit)}.
     * @return This, as per the builder pattern.
     */
    public T tickSize(long ts) {
      tickSize = ts;
      return self();
    }

    /**
     * Set the speed unit. Possible values include: {@link SI#METERS_PER_SECOND}
     * , {@link NonSI#KILOMETERS_PER_HOUR}.
     * @param su The speed unit.
     * @return This, as per the builder pattern.
     */
    public T speedUnit(Unit<Velocity> su) {
      speedUnit = su;
      return self();
    }

    /**
     * Set the distance unit. Possible values include: {@link SI#METER},
     * {@link NonSI#MILE}.
     * @param du The distance unit.
     * @return This, as per the builder pattern.
     */
    public T distanceUnit(Unit<Length> du) {
      distanceUnit = du;
      return self();
    }

    /**
     * Set the length (duration) of the scenario. Note that the time at which
     * the simulation is stopped is defined by {@link #stopCondition(Predicate)}
     * .
     * @param length The length of the scenario, expressed in the time unit as
     *          set by {@link #timeUnit(Unit)}.
     * @return This, as per the builder pattern.
     */
    public T scenarioLength(long length) {
      timeWindow = new TimeWindow(0, length);
      return self();
    }

    /**
     * Set the condition when the scenario should stop. The default is to
     * continue indefinitely via {@link Predicates#alwaysFalse()}.
     * @param condition The stop condition to set.
     * @return This, as per the builder pattern.
     */
    public T stopCondition(Predicate<Simulator> condition) {
      stopCondition = condition;
      return self();
    }

    /**
     * Copies properties of the specified scenario into this builder.
     * @param scenario The scenario to copy the properties from.
     * @return This, as per the builder pattern.
     */
    protected T copyProperties(Scenario scenario) {
      distanceUnit = scenario.getDistanceUnit();
      speedUnit = scenario.getSpeedUnit();
      timeUnit = scenario.getTimeUnit();
      tickSize = scenario.getTickSize();
      timeWindow = scenario.getTimeWindow();
      stopCondition = scenario.getStopCondition();
      return self();
    }

    /**
     * @return {@link Scenario#getDistanceUnit()}.
     */
    public Unit<Length> getDistanceUnit() {
      return distanceUnit;
    }

    /**
     * @return {@link Scenario#getSpeedUnit()}.
     */
    public Unit<Velocity> getSpeedUnit() {
      return speedUnit;
    }

    /**
     * @return {@link Scenario#getTimeUnit()}.
     */
    public Unit<Duration> getTimeUnit() {
      return timeUnit;
    }

    /**
     * @return {@link Scenario#getTickSize()}.
     */
    public long getTickSize() {
      return tickSize;
    }

    /**
     * @return {@link Scenario#getTimeWindow()}.
     */
    public TimeWindow getTimeWindow() {
      return timeWindow;
    }

    /**
     * @return {@link Scenario#getStopCondition()}.
     */
    public Predicate<Simulator> getStopCondition() {
      return stopCondition;
    }
  }

  static class DefaultScenario extends Scenario {
    final ImmutableList<? extends Supplier<? extends Model<?>>> modelSuppliers;
    private final Unit<Velocity> speedUnit;
    private final Unit<Length> distanceUnit;
    private final Unit<Duration> timeUnit;
    private final TimeWindow timeWindow;
    private final long tickSize;
    private final Predicate<Simulator> stopCondition;
    private final ProblemClass problemClass;
    private final String instanceId;

    DefaultScenario(Builder b, ImmutableList<TimedEvent> events,
        ImmutableSet<Enum<?>> eventTypes) {
      super(events, eventTypes.isEmpty() ? collectEventTypes(events)
          : eventTypes);
      modelSuppliers = b.getModelSuppliers();
      speedUnit = b.speedUnit;
      distanceUnit = b.distanceUnit;
      timeUnit = b.timeUnit;
      timeWindow = b.timeWindow;
      tickSize = b.tickSize;
      stopCondition = b.stopCondition;
      problemClass = b.problemClass;
      instanceId = b.instanceId;
    }

    @Override
    public Unit<Duration> getTimeUnit() {
      return timeUnit;
    }

    @Override
    public TimeWindow getTimeWindow() {
      return timeWindow;
    }

    @Override
    public long getTickSize() {
      return tickSize;
    }

    @Override
    public Unit<Velocity> getSpeedUnit() {
      return speedUnit;
    }

    @Override
    public Unit<Length> getDistanceUnit() {
      return distanceUnit;
    }

    @Override
    public Predicate<Simulator> getStopCondition() {
      return stopCondition;
    }

    @Override
    public ImmutableList<? extends Supplier<? extends Model<?>>> getModelSuppliers() {
      return modelSuppliers;
    }

    @Override
    public ProblemClass getProblemClass() {
      return problemClass;
    }

    @Override
    public String getProblemInstanceId() {
      return instanceId;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof DefaultScenario)) {
        return false;
      }
      final DefaultScenario o = (DefaultScenario) other;
      return super.equals(o)
          && Objects.equal(o.modelSuppliers, modelSuppliers)
          && Objects.equal(o.speedUnit, speedUnit)
          && Objects.equal(o.distanceUnit, distanceUnit)
          && Objects.equal(o.timeUnit, timeUnit)
          && Objects.equal(o.timeWindow, timeWindow)
          && Objects.equal(o.tickSize, tickSize)
          && Objects.equal(o.stopCondition, stopCondition)
          && Objects.equal(o.problemClass, problemClass)
          && Objects.equal(o.instanceId, instanceId);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(super.hashCode(), modelSuppliers, speedUnit,
          distanceUnit, timeUnit, timeWindow, tickSize, stopCondition,
          problemClass, instanceId);
    }
  }
}
