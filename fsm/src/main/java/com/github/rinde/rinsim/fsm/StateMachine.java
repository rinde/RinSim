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
package com.github.rinde.rinsim.fsm;

import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableTable;

/**
 * A simple state machine. The state machine is represented by a transition
 * table. A transition has the form <code>current state + trigger </code> &rarr;
 * <code>new state</code>. A transition can be recurrent, meaning that a trigger
 * will <i>not</i> initiate a transition. Triggers can be initiated from within
 * a state by means of the {@link State#handle(Object, Object)} method or from
 * outside the state machine by means of the {@link #handle(Object, Object)}. An
 * attempt to perform a transition not present in the transition table results
 * in a {@link RuntimeException}. Note that the transition table is immutable.
 * StateMachine instances can only be created using its builder via the
 * {@link #create(State)} method.
 *
 * @param <T> The trigger type. Concrete trigger objects that describe the same
 *          event should be <i>equal</i> (according to {@link #equals(Object)} )
 *          and their {@link #hashCode()} implementation should return the same
 *          value. If the events do not need to contain additional meta data,
 *          {@link Enum}s are the best choice.
 * @param <C> The context type. This is typically the object that contains the
 *          {@link StateMachine}, a {@link State} represents a state of this
 *          object.
 * @author Rinde van Lon
 */
public class StateMachine<T, C> {

  /**
   * The type of {@link Event}s that this {@link StateMachine} supports.
   */
  public enum StateMachineEvent {
    /**
     * This event is dispatched when the state machine changes its state. It is
     * called right after the actual state change, after the
     * {@link State#onEntry(Object, Object)} is called but before
     * {@link State#handle(Object, Object)} is called.
     */
    STATE_TRANSITION;
  }

  /**
   * The {@link EventDispatcher} used for dispatching events.
   */
  protected final EventDispatcher eventDispatcher;
  /**
   * The transition table which defines the allowed transitions.
   */
  protected final ImmutableTable<State<T, C>, T, State<T, C>> transitionTable;
  /**
   * The current state.
   */
  protected State<T, C> currentState;
  /**
   * The initial state.
   */
  protected final State<T, C> startState;

  /**
   * This indicates whether recursive transitions should be handled explicitly.
   * If true a recursive transition will be handled just like a normal
   * transition, {@link State#onExit(Object, Object)} and
   * {@link State#onEntry(Object, Object)} methods are called.
   */
  protected final boolean explicitRecursiveTransitions;

  StateMachine(State<T, C> start,
      ImmutableTable<State<T, C>, T, State<T, C>> table,
      boolean explRecurTrns) {
    eventDispatcher = new EventDispatcher(StateMachineEvent.values());
    startState = start;
    currentState = start;
    transitionTable = table;
    explicitRecursiveTransitions = explRecurTrns;
  }

  /**
   * Gives the current {@link State} time to update.
   * @param context Reference to the context.
   */
  public void handle(C context) {
    handle(null, context);
  }

  /**
   * Handle the specified trigger.
   * @param trigger The trigger that needs to be handled by the state machine.
   *          If this results in an attempt to perform a transition which is not
   *          allowed an {@link IllegalArgumentException} is thrown.
   * @param context Reference to the context.
   */
  public void handle(@Nullable T trigger, C context) {
    T ev = trigger;
    do {
      if (ev != null) {
        changeState(ev, context);
      }
      ev = currentState.handle(trigger, context);
    } while (ev != null);
  }

  /**
   * Perform a state change if possible.
   * @param trigger The trigger that may initiate a state change.
   * @param context Reference to the context.
   */
  protected void changeState(T trigger, C context) {
    checkArgument(transitionTable.contains(currentState, trigger),
        "The trigger %s is not supported when in state %s.", trigger,
        currentState);
    final State<T, C> newState = transitionTable.get(currentState, trigger);
    if (!newState.equals(currentState) || explicitRecursiveTransitions) {
      currentState.onExit(trigger, context);
      final State<T, C> oldState = currentState;
      currentState = newState;
      currentState.onEntry(trigger, context);
      eventDispatcher.dispatchEvent(new StateTransitionEvent<>(this,
          oldState, trigger, newState));
    }
  }

  /**
   * @return A reference to the current state of this {@link StateMachine}.
   */
  public State<T, C> getCurrentState() {
    return currentState;
  }

  /**
   * Convenience method for checking whether the current state is the same as
   * the specified state.
   * @param s The state to be checked.
   * @return <code>true</code> when the states are the same object,
   *         <code>false</code> otherwise.
   */
  public boolean stateIs(State<T, C> s) {
    return currentState.equals(s);
  }

  /**
   * Convenience method for checking whether the current state is one of the
   * specified states.
   * @param states The states to be checked.
   * @return <code>true</code> when the current state is one of the specified
   *         states, <code>false</code> otherwise.
   */
  @SafeVarargs
  public final boolean stateIsOneOf(State<T, C>... states) {
    for (final State<T, C> s : states) {
      if (stateIs(s)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return An {@link ImmutableCollection} of all states in this state machine.
   */
  public ImmutableCollection<State<T, C>> getStates() {
    return transitionTable.values();
  }

  /**
   * Looks up a state of the specified (sub)type if it exists. If there exist
   * multiple the first encountered is returned.
   * @param type The (sub)type to look for.
   * @param <U> The type.
   * @return The state of the specified type.
   * @throws IllegalArgumentException if there is no state of the specified
   *           type.
   */
  public <U> U getStateOfType(Class<U> type) {
    for (final State<T, C> state : getStates()) {
      if (type.isInstance(state)) {
        return type.cast(state);
      }
    }
    throw new IllegalArgumentException("There is no instance of " + type
        + " in this state machine.");
  }

  /**
   * Returns true if the current state supports the trigger.
   * @param trigger The trigger to check.
   * @return <code>true</code> when the specified trigger is supported by the
   *         current state, <code>false</code> otherwise.
   */
  public boolean isSupported(T trigger) {
    return transitionTable.contains(currentState, trigger);
  }

  /**
   * @return The {@link EventAPI} which allows to add and remove
   *         {@link com.github.rinde.rinsim.event.Listener}s to this
   *         {@link StateMachine}.
   */
  public EventAPI getEventAPI() {
    return eventDispatcher.getPublicEventAPI();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(startState, transitionTable, currentState,
        explicitRecursiveTransitions);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == null) {
      return false;
    }
    if (this == other) {
      return true;
    }
    if (!(other instanceof StateMachine)) {
      return false;
    }
    @SuppressWarnings("unchecked")
    final StateMachine<T, C> fsm = (StateMachine<T, C>) other;
    return Objects.equal(startState, fsm.startState)
        && Objects.equal(transitionTable, fsm.transitionTable)
        && Objects.equal(currentState, fsm.currentState)
        && Objects.equal(explicitRecursiveTransitions,
            fsm.explicitRecursiveTransitions);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("startState", startState)
        .add("transitionTable", transitionTable)
        .add("currentState", currentState)
        .add("explicitRecursiveTransitions", explicitRecursiveTransitions)
        .toString();
  }

  /**
   * Create a new {@link StateMachine} instance with the specified initial
   * state. This method returns a reference to the {@link StateMachineBuilder}
   * which allows for adding of transitions to the state machine.
   * @param initialState The start state of the state machine.
   * @param <T> The trigger type.
   * @param <C> The context type.
   * @return A reference to the {@link StateMachineBuilder} which is used for
   *         creating the {@link StateMachine}.
   */
  @CheckReturnValue
  public static <T, C> StateMachineBuilder<T, C> create(
      State<T, C> initialState) {
    return new StateMachineBuilder<>(initialState);
  }

  /**
   * Facilitates the creation of a {@link StateMachine}.
   * @param <T> Trigger parameter of {@link StateMachine}.
   * @param <C> Context parameter of {@link StateMachine}.
   * @see StateMachine
   */
  public static final class StateMachineBuilder<T, C> {
    private final ImmutableTable.Builder<State<T, C>, T, State<T, C>> tableBuilder;
    private final State<T, C> start;
    private boolean explicitRecursiveTransitions;

    StateMachineBuilder(State<T, C> initialState) {
      tableBuilder = ImmutableTable.builder();
      start = initialState;
      explicitRecursiveTransitions = false;
    }

    /**
     * Add a transition: <code>state + trigger &rarr; new state</code>.
     * @param from The from state.
     * @param trigger The trigger which triggers the transition.
     * @param to The destination of the transition, the new state.
     * @return A reference to this for method chaining.
     */
    public StateMachineBuilder<T, C> addTransition(State<T, C> from, T trigger,
        State<T, C> to) {
      tableBuilder.put(from, trigger, to);
      return this;
    }

    /**
     * Adds all transitions in the specified {@link StateMachine} to this
     * builder. Duplicates are not allowed.
     * @param sm The {@link StateMachine} from which the transitions are copied.
     * @return The builder reference.
     */
    public StateMachineBuilder<T, C> addTransitionsFrom(StateMachine<T, C> sm) {
      tableBuilder.putAll(sm.transitionTable);
      return this;
    }

    /**
     * Enables explicit recursive transitions, this means that when an recursive
     * transition is attempted the {@link State#onExit(Object, Object)} and
     * {@link State#onEntry(Object, Object)} are called on that state and a
     * {@link StateMachine.StateTransitionEvent} is dispatched. By default
     * recursive transitions are ignored.
     * @return The builder reference.
     */
    public StateMachineBuilder<T, C> explicitRecursiveTransitions() {
      explicitRecursiveTransitions = true;
      return this;
    }

    /**
     * Builds the {@link StateMachine} as configured by this
     * {@link com.github.rinde.rinsim.fsm.StateMachine.StateMachineBuilder}.
     * @return The {@link StateMachine}.
     */
    @CheckReturnValue
    public StateMachine<T, C> build() {
      return new StateMachine<>(start, tableBuilder.build(),
          explicitRecursiveTransitions);
    }
  }

  /**
   * Event class used by {@link StateMachine}.
   * @param <T> Trigger parameter of {@link StateMachine}.
   * @param <C> Context parameter of {@link StateMachine}.
   * @see StateMachine
   * @see StateMachineEvent
   */
  public static class StateTransitionEvent<T, C> extends Event {
    /**
     * The previous state of the state machine prior to the current state.
     */
    public final State<T, C> previousState;
    /**
     * The new state which was activated just before this event was issued.
     */
    public final State<T, C> newState;
    /**
     * The trigger that caused the event transition.
     */
    public final T trigger;

    /**
     * Create new event instance.
     * @param issuer Issuer of the event.
     * @param prev {@link #previousState}.
     * @param trig {@link #trigger}.
     * @param next {@link #newState}.
     */
    protected StateTransitionEvent(StateMachine<T, C> issuer, State<T, C> prev,
        T trig, State<T, C> next) {
      super(StateMachineEvent.STATE_TRANSITION, issuer);
      previousState = prev;
      trigger = trig;
      newState = next;
    }

    /**
     * Convenience method for checking whether this event equals a transition as
     * specified by the parameters.
     * @param prev Previous state.
     * @param trig Trigger.
     * @param next New state.
     * @return <code>true</code> when all parameters are equal to the properties
     *         of this event, <code>false</code> otherwise.
     */
    public boolean equalTo(State<T, C> prev, T trig, State<T, C> next) {
      return previousState.equals(prev) && trigger.equals(trig)
          && newState.equals(next);
    }

    @Override
    public String toString() {
      return new StringBuilder("[Event ").append(getEventType()).append(" ")
          .append(previousState).append(" + ").append(trigger).append(" -> ")
          .append(newState).append("]").toString();
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(previousState, trigger, newState);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (other == null) {
        return false;
      }
      if (this == other) {
        return true;
      }
      if (!(other instanceof StateTransitionEvent<?, ?>)) {
        return false;
      }
      @SuppressWarnings("unchecked")
      final StateTransitionEvent<T, C> ev = (StateTransitionEvent<T, C>) other;
      return Objects.equal(previousState, ev.previousState)
          && Objects.equal(newState, ev.newState)
          && Objects.equal(trigger, ev.trigger)
          && Objects.equal(eventType, ev.eventType)
          && Objects.equal(getIssuer(), ev.getIssuer());
    }
  }
}
