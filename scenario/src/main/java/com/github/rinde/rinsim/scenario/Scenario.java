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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.scenario.TimedEvent.TimeComparator;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * Scenario is an immutable list of events sorted by the time stamp. To obtain
 * an instance there are a number of builder methods available such as
 * {@link #builder()}.
 * @author Rinde van Lon
 * @author Bartosz Michalik
 */
@AutoValue
public abstract class Scenario {
  /**
   * The default {@link ProblemClass}.
   */
  public static final ProblemClass DEFAULT_PROBLEM_CLASS = SimpleProblemClass
      .create("DEFAULT");

  /**
   * Instantiate a scenario.
   */
  protected Scenario() {}

  /**
   * Return a scenario as a list of (time sorted) events.
   * @return the list of events.
   */
  public abstract ImmutableList<TimedEvent> getEvents();

  /**
   * @return A queue containing all events of this scenario.
   */
  public Queue<TimedEvent> asQueue() {
    return newLinkedList(getEvents());
  }

  /**
   * @return Should return a list of {@link ModelBuilder}s which will be used
   *         for creating the models for this scenario.
   */
  public abstract ImmutableSet<ModelBuilder<?, ?>> getModelBuilders();

  /**
   * @return The {@link TimeWindow} of the scenario indicates the start and end
   *         of the scenario.
   */
  public abstract TimeWindow getTimeWindow();

  /**
   * @return The stop condition indicating when a simulation should end.
   */
  public abstract StopCondition getStopCondition();

  /**
   * @return The 'class' to which this scenario belongs.
   */
  public abstract ProblemClass getProblemClass();

  /**
   * @return The instance id of this scenario.
   */
  public abstract String getProblemInstanceId();

  @Override
  public abstract boolean equals(@Nullable Object other);

  @Override
  public abstract int hashCode();

  static Scenario create(
      Iterable<? extends TimedEvent> events,
      Iterable<? extends ModelBuilder<?, ?>> builders,
      TimeWindow tw,
      StopCondition stopCondition,
      ProblemClass pc,
      String id) {
    return new AutoValue_Scenario(
        ImmutableList.<TimedEvent>copyOf(events),
        ImmutableSet.<ModelBuilder<?, ?>>copyOf(builders),
        tw,
        stopCondition,
        pc, id);
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
    return new Builder(Optional.<AbstractBuilder<?>>of(base), problemClass);
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
  @AutoValue
  public abstract static class SimpleProblemClass implements ProblemClass {

    @Override
    public abstract String getId();

    @Override
    public String toString() {
      return String.format("SimpleProblemClass(%s)", getId());
    }

    /**
     * Create a new instance.
     * @param id The name to use as id.
     * @return A new instance.
     */
    public static SimpleProblemClass create(String id) {
      return new AutoValue_Scenario_SimpleProblemClass(id);
    }
  }

  /**
   * A builder for constructing {@link Scenario} instances.
   * @author Rinde van Lon
   */
  public static class Builder extends AbstractBuilder<Builder> {
    final List<TimedEvent> eventList;
    final List<ModelBuilder<?, ?>> modelBuilders;
    ProblemClass problemClass;
    String instanceId;

    Builder(ProblemClass pc) {
      this(Optional.<AbstractBuilder<?>>absent(), pc);
    }

    Builder(Optional<AbstractBuilder<?>> base, ProblemClass pc) {
      super(base);
      problemClass = pc;
      instanceId = "";
      eventList = new ArrayList<>();
      modelBuilders = new ArrayList<>();
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
     * Adds the model builder. The builders will be used to instantiate
     * {@link Model}s needed for the scenario.
     * @param modelBuilder The model builder to add.
     * @return This, as per the builder pattern.
     */
    public Builder addModel(ModelBuilder<?, ?> modelBuilder) {
      modelBuilders.add(modelBuilder);
      return self();
    }

    /**
     * Adds the model builders. The builders will be used to instantiate
     * {@link Model}s needed for the scenario.
     * @param builders The model builders to add.
     * @return This, as per the builder pattern.
     */
    public Builder addModels(Iterable<? extends ModelBuilder<?, ?>> builders) {
      Iterables.addAll(modelBuilders, builders);
      return self();
    }

    /**
     * Removes all previously added model builders that are an instance of the
     * specified type.
     * @param type All model builders that are instance of this type are
     *          removed.
     * @param <T> The type.
     * @return This, as per the builder pattern.
     */
    public <T extends ModelBuilder<?, ?>> Builder removeModelsOfType(
        Class<T> type) {
      final List<ModelBuilder<?, ?>> toRemove = new ArrayList<>();
      for (final ModelBuilder<?, ?> mb : modelBuilders) {
        if (type.isInstance(mb)) {
          toRemove.add(mb);
        }
      }
      modelBuilders.removeAll(toRemove);
      return self();
    }

    @Override
    public Builder copyProperties(Scenario scenario) {
      return super.copyProperties(scenario)
          .addEvents(scenario.getEvents())
          .problemClass(scenario.getProblemClass())
          .instanceId(scenario.getProblemInstanceId())
          .addModels(scenario.getModelBuilders());
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
        "The specified filter did not match any event in the event list "
            + "(size %s), filter: %s.",
        eventList.size(), filter);
      final Set<TimedEvent> set = filtered.toSet();
      checkArgument(
        set.size() == 1,
        "The specified filter matches multiple non-equal events, all matches "
            + "must be equal. Events: %s. Filter: %s.",
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

      return Scenario.create(list, modelBuilders, timeWindow, stopCondition,
        problemClass, instanceId);
    }

    @Override
    protected Builder self() {
      return this;
    }
  }

  /**
   * Abstract builder of {@link Scenario} instances. Provides methods for
   * setting the basic properties of a scenario.
   * @author Rinde van Lon
   * @param <T> The type of concrete builder.
   */
  public abstract static class AbstractBuilder<T extends AbstractBuilder<T>> {
    static final TimeWindow DEFAULT_TIME_WINDOW = TimeWindow.create(0,
      8 * 60 * 60 * 1000);
    static final StopCondition DEFAULT_STOP_CONDITION = StopConditions
        .alwaysFalse();

    /**
     * Defines {@link Scenario#getTimeWindow()}.
     */
    protected TimeWindow timeWindow;

    /**
     * Defines {@link Scenario#getStopCondition()}.
     */
    protected StopCondition stopCondition;

    /**
     * Copying constructor. Copies all settings from the specified builder into
     * this instance. If no builder is specified default values will be used.
     * @param copy An existing builder or {@link Optional#absent()}.
     */
    protected AbstractBuilder(Optional<AbstractBuilder<?>> copy) {
      if (copy.isPresent()) {
        timeWindow = copy.get().timeWindow;
        stopCondition = copy.get().stopCondition;
      } else {
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
     * Set the length (duration) of the scenario. Note that the time at which
     * the simulation is stopped is defined by
     * {@link #setStopCondition(StopCondition)} .
     * @param length The length of the scenario, expressed in the time unit as
     *          defined by the {@link TimeModel}.
     * @return This, as per the builder pattern.
     */
    public T scenarioLength(long length) {
      timeWindow = TimeWindow.create(0, length);
      return self();
    }

    /**
     * Set the condition when the scenario should stop. The default is to
     * continue indefinitely via {@link Predicates#alwaysFalse()}.
     * @param condition The stop condition to set.
     * @return This, as per the builder pattern.
     */
    public T setStopCondition(StopCondition condition) {
      stopCondition = condition;
      return self();
    }

    /**
     * Copies properties of the specified scenario into this builder.
     * @param scenario The scenario to copy the properties from.
     * @return This, as per the builder pattern.
     */
    protected T copyProperties(Scenario scenario) {
      timeWindow = scenario.getTimeWindow();
      stopCondition = scenario.getStopCondition();
      return self();
    }

    /**
     * @return {@link Scenario#getTimeWindow()}.
     */
    public TimeWindow getTimeWindow() {
      return timeWindow;
    }

    // move into separate model or scenario controller?
    /**
     * @return {@link Scenario#getStopCondition()}.
     */
    public StopCondition getStopCondition() {
      return stopCondition;
    }
  }
}
