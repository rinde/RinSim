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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.unmodifiableList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.event.ListenerEventHistory;
import com.github.rinde.rinsim.fsm.AbstractState;
import com.github.rinde.rinsim.fsm.State;
import com.github.rinde.rinsim.fsm.StateMachine;
import com.github.rinde.rinsim.fsm.StateMachines;
import com.github.rinde.rinsim.fsm.StateMachine.StateMachineBuilder;
import com.github.rinde.rinsim.fsm.StateMachine.StateMachineEvent;
import com.github.rinde.rinsim.fsm.StateMachine.StateTransitionEvent;

/**
 * @author Rinde van Lon
 * 
 */
@RunWith(value = Parameterized.class)
public class StateMachineTest {
  final static Context CONTEXT = new Context();
  final static Context SPECIAL_CONTEXT = new Context();
  /**
   * The state machine under test.
   */
  protected StateMachine<Events, Context> fsm;
  final boolean explicitRecursiveTransitions;
  DefaultState startState, stopState, pauseState, specialState;

  /**
   * @param ert Indicates whether to create a state machine that enables
   *          explicit recursive transitions.
   */
  @SuppressWarnings("null")
  public StateMachineTest(boolean ert) {
    explicitRecursiveTransitions = ert;
  }

  /**
   * @return The parameters for the tests.
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] { { true }, { false } });
  }

  /**
   * Setup the state machine used in the tests.
   */
  @Before
  public void setUp() {
    startState = new StartState();
    stopState = new StopState();
    pauseState = new PauseState();
    specialState = new SpecialState();

    final StateMachineBuilder<Events, Context> smb = StateMachine
        .create(stopState)
        .addTransition(startState, Events.STOP, stopState)
        .addTransition(startState, Events.SPEZIAL, specialState)
        .addTransition(specialState, Events.START, startState)
        .addTransition(specialState, Events.STOP, specialState)
        .addTransition(stopState, Events.START, startState)
        .addTransition(stopState, Events.STOP, stopState)
        .addTransition(startState, Events.PAUSE, pauseState)
        .addTransition(pauseState, Events.STOP, stopState)
        .addTransition(pauseState, Events.START, startState)
        .addTransition(startState, Events.RECURSIVE, startState)
        .addTransition(pauseState, Events.RECURSIVE, pauseState)
        .addTransition(stopState, Events.RECURSIVE, stopState)
        .addTransition(specialState, Events.RECURSIVE, specialState);

    if (explicitRecursiveTransitions) {
      smb.explicitRecursiveTransitions();
    }

    fsm = smb.build();

    assertTrue(fsm
        .stateIsOneOf(startState, specialState, stopState, pauseState));
    assertFalse(fsm.stateIsOneOf(startState, specialState));

    StateMachines.toDot(fsm);
    StateMachineEvent.valueOf("STATE_TRANSITION");
  }

  /**
   * Tests the query by class method.
   */
  @Test
  public void testGetStateOfType() {
    final StateA a = new StateA();
    final StateB b = new StateB();
    final StateC c = new StateC();
    final StateMachineBuilder<Events, Context> smb = StateMachine.create(a)
        .addTransition(a, Events.START, b)
        .addTransition(b, Events.START, c)
        .addTransition(c, Events.START, a);

    if (explicitRecursiveTransitions) {
      smb.explicitRecursiveTransitions();
    }
    fsm = smb.build();

    final State<Events, Context> first = fsm.getStates().iterator().next();
    assertEquals(first, fsm.getStateOfType(Object.class));
    assertEquals(b, fsm.getStateOfType(StateB.class));
    assertEquals(c, fsm.getStateOfType(StateC.class));

    boolean fail = false;
    try {
      fsm.getStateOfType(Enum.class);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Tests transitions.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testTransition() {
    final ListenerEventHistory history = new ListenerEventHistory();
    fsm.getEventAPI()
        .addListener(history, StateMachine.StateMachineEvent.values());

    // start in STOPPED state
    assertEquals(stopState, fsm.getCurrentState());
    fsm.handle(Events.START, CONTEXT);
    assertEquals(startState, fsm.getCurrentState());
    // history.getHistory()

    // nothing should happen
    fsm.handle(CONTEXT);
    assertEquals(startState, fsm.getCurrentState());

    // should go to SPECIAL and back to STARTED immediately
    history.clear();
    fsm.handle(Events.SPEZIAL, SPECIAL_CONTEXT);
    assertEquals(startState, fsm.getCurrentState());
    assertEquals(2, history.getHistory().size());
    assertTrue(((StateTransitionEvent) history.getHistory().get(0))
        .equalTo(startState, Events.SPEZIAL, specialState));
    assertTrue(((StateTransitionEvent) history.getHistory().get(1))
        .equalTo(specialState, Events.START, startState));

    // testing the equalTo method
    assertFalse(((StateTransitionEvent) history.getHistory().get(1))
        .equalTo(startState, Events.START, startState));
    assertFalse(((StateTransitionEvent) history.getHistory().get(1))
        .equalTo(specialState, Events.PAUSE, startState));
    assertFalse(((StateTransitionEvent) history.getHistory().get(1))
        .equalTo(specialState, Events.START, specialState));

    // go to SPECIAL
    fsm.handle(Events.SPEZIAL, CONTEXT);
    assertEquals(specialState, fsm.getCurrentState());
    // should remain in SPECIAL
    fsm.handle(Events.STOP, CONTEXT);
    assertEquals(specialState, fsm.getCurrentState());

    fsm.handle(Events.START, CONTEXT);
    assertEquals(startState, fsm.getCurrentState());
  }

  /**
   * Tests recursive transitions.
   */
  @Test
  public void testRecursiveTransitions() {
    final ListenerEventHistory history = new ListenerEventHistory();
    fsm.getEventAPI()
        .addListener(history, StateMachine.StateMachineEvent.values());

    // stopped recursive
    assertEquals(stopState, fsm.getCurrentState());
    assertTrue(stopState.handleHistory().isEmpty());
    assertTrue(stopState.onEntryHistory().isEmpty());
    assertTrue(stopState.onExitHistory().isEmpty());
    assertTrue(history.getHistory().isEmpty());

    fsm.handle(Events.RECURSIVE, CONTEXT);
    assertEquals(stopState, fsm.getCurrentState());

    assertEquals(1, stopState.handleHistory().size());
    if (explicitRecursiveTransitions) {
      assertEquals(1, stopState.onEntryHistory().size());
      assertEquals(1, stopState.onExitHistory().size());
      assertEquals(1, history.getHistory().size());
      assertEquals(
          new StateTransitionEvent<>(fsm, stopState,
              Events.RECURSIVE, stopState),
          history.getHistory().get(0));
    } else {
      assertTrue(stopState.onEntryHistory().isEmpty());
      assertTrue(stopState.onExitHistory().isEmpty());
      assertTrue(history.getHistory().isEmpty());
    }
  }

  /**
   * Test transition that is not allowed.
   */
  @Test(expected = IllegalArgumentException.class)
  public void impossibleTransition() {
    fsm.handle(Events.START, CONTEXT);
    fsm.handle(Events.START, CONTEXT);
  }

  /**
   * Tests correct behavior for events which are not equal.
   */
  @SuppressWarnings("static-method")
  @Test
  public void eventNotEqualBehavior() {
    final TestState state1 = new TestState("state1");
    state1.name();
    final TestState state2 = new TestState("state2");
    final Object event1 = "event1";
    final Object event2 = new Object();

    final StateMachine<Object, Object> sm = StateMachine.create(state1)/* */
        .addTransition(state1, event1, state2)/* */
        .addTransition(state2, event2, state1)/* */
        .build();

    assertTrue(sm.isSupported(event1));
    assertTrue(sm.isSupported("event1"));
    assertTrue(sm.isSupported(new StringBuilder("event").append(1)
        .toString()));

    assertFalse(sm.isSupported(event2));

    sm.handle("event1", CONTEXT);
    assertTrue(sm.stateIs(state2));

    assertTrue(sm.isSupported(event2));
    assertFalse(sm.isSupported(new Object()));
  }

  static class TestState extends AbstractState<Object, Object> {
    private final String name;

    public TestState(String pName) {
      name = pName;
    }

    @Override
    public String name() {
      return super.name() + name;
    }

    @Nullable
    @Override
    public Object handle(@Nullable Object event, Object context) {
      return null;
    }
  }

  enum Events {
    START, STOP, PAUSE, SPEZIAL, RECURSIVE
  }

  static class DefaultState implements State<Events, Context> {
    private final List<Events> history;
    private final List<Events> onEntryHistory;
    private final List<Events> onExitHistory;

    DefaultState() {
      history = newArrayList();
      onEntryHistory = newArrayList();
      onExitHistory = newArrayList();
    }

    @Override
    public String name() {
      return getClass().getSimpleName();
    }

    @Override
    @Nullable
    public Events handle(@Nullable Events event, Context context) {
      history.add(event);
      return null;
    }

    @Override
    public void onEntry(Events event, Context context) {
      onEntryHistory.add(event);
    }

    @Override
    public void onExit(Events event, Context context) {
      onExitHistory.add(event);
    }

    List<Events> handleHistory() {
      return unmodifiableList(history);
    }

    List<Events> onEntryHistory() {
      return unmodifiableList(onEntryHistory);
    }

    List<Events> onExitHistory() {
      return unmodifiableList(onExitHistory);
    }
  }

  class StateA extends DefaultState {}

  class StateB extends DefaultState {}

  class StateC extends DefaultState {}

  static class StartState extends DefaultState {}

  static class StopState extends DefaultState {}

  static class PauseState extends DefaultState {}

  static class SpecialState extends DefaultState {
    @Nullable
    @Override
    public Events handle(@Nullable Events event, Context context) {
      super.handle(event, context);
      if (context == SPECIAL_CONTEXT) {
        return Events.START;
      }
      return null;
    }
  }

  static class Context {}
}
