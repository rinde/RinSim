/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import static com.github.rinde.rinsim.scenario.ScenarioController.EventType.SCENARIO_EVENT;
import static com.github.rinde.rinsim.scenario.ScenarioController.EventType.SCENARIO_FINISHED;
import static com.github.rinde.rinsim.scenario.ScenarioController.EventType.SCENARIO_STARTED;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.core.model.time.ClockController;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeLapseFactory;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.event.ListenerEventHistory;
import com.github.rinde.rinsim.scenario.ScenarioController.EventType;
import com.github.rinde.rinsim.testutil.TestUtil;
import com.google.auto.value.AutoValue;

/**
 * Tests for {@link ScenarioController}.
 * @author Rinde van Lon
 */
public class ScenarioControllerTest {

  @SuppressWarnings("null")
  ScenarioController controller;
  @SuppressWarnings("null")
  Scenario scenario;

  @SuppressWarnings("null")
  DependencyProvider dependencyProvider;

  /**
   * Sets up a scenario.
   */
  @Before
  public void setUp() {
    scenario = Scenario
        .builder()
        .addEvent(EventC.create(100))
        .addEvent(EventA.create(0))
        .addEvent(EventA.create(1))
        .addEvent(EventB.create(0))
        .addEvent(EventB.create(0))
        .addEvent(EventC.create(5))
        .build();
    assertThat(scenario).isNotNull();

    TestUtil.testEnum(ScenarioController.EventType.class);

    final ClockController clock = mock(ClockController.class);
    when(clock.getEventAPI()).thenReturn(mock(EventAPI.class));
    final SimulatorAPI sim = mock(SimulatorAPI.class);

    dependencyProvider = mock(DependencyProvider.class);
    when(dependencyProvider.get(ClockController.class)).thenReturn(clock);
    when(dependencyProvider.get(Clock.class)).thenReturn(clock);
    when(dependencyProvider.get(SimulatorAPI.class)).thenReturn(sim);
  }

  /**
   * Tests that a not handled event results in a {@link IllegalStateException}.
   */
  @Test
  public void testEventNotHandled() {
    final ScenarioController.Builder b = ScenarioController.builder(scenario)
        .withNumberOfTicks(3);

    boolean fail = false;
    try {
      b.build(dependencyProvider);
    } catch (final IllegalStateException e) {
      assertThat(e.getMessage()).containsMatch("No handler found for event");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  /**
   * Tests that handling an interface is rejected.
   */
  @Test
  public void testHandleInterface() {
    boolean fail = false;
    try {
      ScenarioController.builder(scenario)
          .withEventHandler(TimedEvent.class, new NopHandler<>()).toString();
    } catch (final IllegalArgumentException e) {
      fail = true;
      assertThat(e.getMessage()).containsMatch("Must handle a concrete class");
    }
    assertThat(fail).isTrue();
  }

  /**
   * Tests a scenario with a limited number of ticks.
   */
  @Test
  public void finiteSimulation() {
    final NopHandler<?> handler = new NopHandler<>();
    @SuppressWarnings("unchecked")
    final Simulator sim = Simulator
        .builder()
        .setTickLength(1L)
        .setTimeUnit(SI.SECOND)
        .addModel(
            ScenarioController.builder(scenario)
                .withEventHandler(EventA.class, (NopHandler<EventA>) handler)
                .withEventHandler(EventB.class, (NopHandler<EventB>) handler)
                .withEventHandler(EventC.class, (NopHandler<EventC>) handler)
                .withNumberOfTicks(101))
        .build();

    final List<Long> ticks = new ArrayList<>();
    sim.addTickListener(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        ticks.add(timeLapse.getStartTime());
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });

    final ScenarioController sc = sim.getModelProvider().getModel(
        ScenarioController.class);

    final ListenerEventHistory leh = new ListenerEventHistory();
    sc.getEventAPI().addListener(leh);
    assertThat(sc.isScenarioFinished()).isFalse();
    sim.start();

    assertThat(handler.getEvents()).containsExactly(
        EventA.create(0),
        EventB.create(0),
        EventB.create(0),
        EventA.create(1),
        EventC.create(5),
        EventC.create(100)).inOrder();

    assertThat(leh.getEventTypeHistory())
        .containsAllOf(SCENARIO_STARTED, SCENARIO_FINISHED)
        .inOrder();

    assertThat(sc.isScenarioFinished()).isTrue();
    sim.stop();
    final long before = sc.clock.getCurrentTime();
    sim.start();// should have no effect

    assertThat(ticks).hasSize(101);

    assertThat(before).isEqualTo(sc.clock.getCurrentTime());
    final TimeLapse emptyTime = TimeLapseFactory.create(0, 1);
    emptyTime.consumeAll();
    sc.tick(emptyTime);
  }

  /**
   * Test for stop condition.
   */
  @Test
  public void testStopCondition() {
    final Simulator sim = Simulator
        .builder()
        .setTickLength(1L)
        .addModel(
            ScenarioController
                .builder(scenario)
                .withEventHandler(EventA.class, new NopHandler<EventA>())
                .withEventHandler(EventB.class, new NopHandler<EventB>())
                .withEventHandler(EventC.class, new NopHandler<EventC>())
                .withAndStopCondition(StopConditions.alwaysTrue()))
        .build();

    sim.start();

    assertThat(sim.getCurrentTime()).isEqualTo(1L);

    final Simulator sim2 = Simulator
        .builder()
        .setTickLength(1L)
        .addModel(
            ScenarioController
                .builder(scenario)
                .withEventHandler(EventA.class, new NopHandler<EventA>())
                .withEventHandler(EventB.class, new NopHandler<EventB>())
                .withEventHandler(EventC.class, new NopHandler<EventC>())
                .withAndStopCondition(StopConditions.limitedTime(100)))
        .build();

    sim2.start();

    assertThat(sim2.getCurrentTime()).isEqualTo(101L);
  }

  /**
   * Tests proper dispatching of setup events.
   */
  @Test
  public void testSetupEvents() {
    final Scenario s = Scenario
        .builder()
        .addEvent(EventA.create(0))
        .addEvent(EventB.create(-1))
        .addEvent(EventB.create(2))
        .addEvent(EventA.create(2))
        .addEvent(EventC.create(-1))
        .addEvent(EventC.create(100))
        .build();

    final NopHandler<?> handler = new NopHandler<>();

    @SuppressWarnings("unchecked")
    final Simulator sim = Simulator.builder()
        .setTickLength(1L)
        .setTimeUnit(SI.SECOND)
        .addModel(
            ScenarioController.builder(s)
                .withNumberOfTicks(1)
                .withEventHandler(EventA.class, (NopHandler<EventA>) handler)
                .withEventHandler(EventB.class, (NopHandler<EventB>) handler)
                .withEventHandler(EventC.class, (NopHandler<EventC>) handler))
        .build();

    final ListenerEventHistory leh = new ListenerEventHistory();
    final ScenarioController sc = sim.getModelProvider().getModel(
        ScenarioController.class);
    sc.getEventAPI().addListener(leh);
    sim.start();

    assertThat(handler.getEvents()).containsExactly(
        EventB.create(-1),
        EventC.create(-1),
        EventA.create(0)).inOrder();

    assertThat(leh.getEventTypeHistory())
        .containsExactly(SCENARIO_EVENT, SCENARIO_EVENT, SCENARIO_STARTED,
            SCENARIO_EVENT)
        .inOrder();

  }

  /**
   * Checks whether the start events are generated.
   */
  @Test
  public void testStartEventGenerated() {
    final NopHandler<EventA> aHandler = new NopHandler<>();
    final NopHandler<EventB> bHandler = new NopHandler<>();
    final NopHandler<EventC> cHandler = new NopHandler<>();

    final Simulator sim = Simulator.builder()
        .setTickLength(1L)
        .setTimeUnit(SI.SECOND)
        .addModel(
            ScenarioController.builder(scenario)
                .withEventHandler(EventA.class, aHandler)
                .withEventHandler(EventB.class, bHandler)
                .withEventHandler(EventC.class, cHandler))
        .build();

    controller = sim.getModelProvider().getModel(ScenarioController.class);

    final ListenerEventHistory leh = new ListenerEventHistory();
    controller.getEventAPI().addListener(leh);

    controller.clock.tick();

    assertThat(aHandler.getEvents()).containsExactly(EventA.create(0L));
    assertThat(bHandler.getEvents()).containsExactly(EventB.create(0L),
        EventB.create(0L));
    assertThat(cHandler.getEvents()).isEmpty();
    assertThat(leh.getEventTypeHistory()).containsExactly(
        EventType.SCENARIO_STARTED,
        EventType.SCENARIO_EVENT,
        EventType.SCENARIO_EVENT,
        EventType.SCENARIO_EVENT);
  }

  /**
   * Test run of whole scenario.
   */
  @Test
  public void runningWholeScenario() {
    final NopHandler<?> handler = new NopHandler<>();
    @SuppressWarnings("unchecked")
    final Simulator sim = Simulator.builder()
        .setTickLength(1L)
        .setTimeUnit(SI.SECOND)
        .addModel(
            ScenarioController.builder(scenario)
                .withNumberOfTicks(-1)
                .withEventHandler(EventA.class, (NopHandler<EventA>) handler)
                .withEventHandler(EventB.class, (NopHandler<EventB>) handler)
                .withEventHandler(EventC.class, (NopHandler<EventC>) handler))
        .build();

    controller = sim.getModelProvider().getModel(ScenarioController.class);
    controller.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        if (e
            .getEventType() == ScenarioController.EventType.SCENARIO_FINISHED) {
          sim.stop();
        }
      }
    });
    sim.start();
    assertThat(handler.getEvents()).hasSize(scenario.getEvents().size());
    assertThat(controller.isScenarioFinished()).isTrue();
  }

  static class NopHandler<T extends TimedEvent>
      implements TimedEventHandler<T> {

    private final List<T> events;

    NopHandler() {
      events = new ArrayList<>();
    }

    @Override
    public void handleTimedEvent(T event, SimulatorAPI simulator) {
      events.add(event);
    }

    public List<T> getEvents() {
      return Collections.unmodifiableList(events);
    }
  }

  @AutoValue
  abstract static class EventA implements TimedEvent {
    static EventA create(long time) {
      return new AutoValue_ScenarioControllerTest_EventA(time);
    }
  }

  @AutoValue
  abstract static class EventB implements TimedEvent {
    static EventB create(long time) {
      return new AutoValue_ScenarioControllerTest_EventB(time);
    }
  }

  @AutoValue
  abstract static class EventC implements TimedEvent {
    static EventC create(long time) {
      return new AutoValue_ScenarioControllerTest_EventC(time);
    }
  }

  @AutoValue
  abstract static class EventD implements TimedEvent {
    static EventD create(long time) {
      return new AutoValue_ScenarioControllerTest_EventD(time);
    }
  }
}
