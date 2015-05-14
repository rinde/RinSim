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

import static com.github.rinde.rinsim.scenario.ScenarioController.EventType.SCENARIO_FINISHED;
import static com.github.rinde.rinsim.scenario.ScenarioController.EventType.SCENARIO_STARTED;
import static com.github.rinde.rinsim.scenario.ScenarioControllerTest.TestEvents.EVENT_A;
import static com.github.rinde.rinsim.scenario.ScenarioControllerTest.TestEvents.EVENT_B;
import static com.github.rinde.rinsim.scenario.ScenarioControllerTest.TestEvents.EVENT_C;
import static com.github.rinde.rinsim.scenario.ScenarioControllerTest.TestEvents.EVENT_D;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import com.github.rinde.rinsim.testutil.TestUtil;
import com.google.common.base.Predicates;

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

  enum TestEvents {
    EVENT_A, EVENT_B, EVENT_C, EVENT_D;
  }

  /**
   * Sets up a scenario.
   */
  @Before
  public void setUp() {
    scenario = Scenario.builder()
      .addEventTypes(asList(EVENT_A, EVENT_B, EVENT_C, EVENT_D))
      .addEvent(new TimedEvent(EVENT_A, 0))
      .addEvent(new TimedEvent(EVENT_B, 0))
      .addEvent(new TimedEvent(EVENT_B, 0))
      .addEvent(new TimedEvent(EVENT_A, 1))
      .addEvent(new TimedEvent(EVENT_C, 5))
      .addEvent(new TimedEvent(EVENT_C, 100))
      .build();
    assertThat(scenario).isNotNull();

    TestUtil.testEnum(ScenarioController.EventType.class);

    ClockController clock = mock(ClockController.class);
    when(clock.getEventAPI()).thenReturn(mock(EventAPI.class));
    SimulatorAPI sim = mock(SimulatorAPI.class);

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
    controller = ScenarioController.builder(scenario)
      .withNumberOfTicks(3)
      .build(dependencyProvider);
    boolean fail = false;
    try {
      controller.tick(TimeLapseFactory.create(0, 1));
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).containsMatch("The event .* is not handled");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  /**
   * Tests a scenario with a limited number of ticks.
   */
  @Test
  public void finiteSimulation() {
    Simulator sim = Simulator
      .builder()
      .setTickLength(1L)
      .setTimeUnit(SI.SECOND)
      .addModel(
        ScenarioController.builder(scenario)
          .withEventHandler(AddParcelEvent.class,
            new NopHandler<AddParcelEvent>())
          .withEventHandler(TimedEvent.class, new NopHandler<>())
          .withNumberOfTicks(101)
      )
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

    ScenarioController sc = sim.getModelProvider().getModel(
      ScenarioController.class);

    final ListenerEventHistory leh = new ListenerEventHistory();
    sc.getEventAPI().addListener(leh);
    assertThat(sc.isScenarioFinished()).isFalse();
    sim.start();
    assertThat(leh.getEventTypeHistory())
      .containsExactly(SCENARIO_STARTED, EVENT_A, EVENT_B, EVENT_B, EVENT_A,
        EVENT_C, EVENT_C, SCENARIO_FINISHED).inOrder();

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
    Simulator sim = Simulator
      .builder()
      .setTickLength(1L)
      .addModel(
        ScenarioController
          .builder(scenario)
          .withEventHandler(AddParcelEvent.class,
            new NopHandler<AddParcelEvent>())
          .withEventHandler(TimedEvent.class, new NopHandler<>())
          .withStopCondition(
            StopConditions.adapt(Clock.class, Predicates.<Clock> alwaysTrue())))
      .build();

    sim.start();

    assertThat(sim.getCurrentTime()).isEqualTo(1L);

    Simulator sim2 = Simulator
      .builder()
      .setTickLength(1L)
      .addModel(
        ScenarioController
          .builder(scenario)
          .withEventHandler(AddParcelEvent.class,
            new NopHandler<AddParcelEvent>())
          .withEventHandler(TimedEvent.class, new NopHandler<>())
          .withStopCondition(StopConditions.limitedTime(100))
      )
      .build();

    sim2.start();

    assertThat(sim2.getCurrentTime()).isEqualTo(101L);
  }

  /**
   * Tests proper dispatching of setup events.
   */
  @Test
  public void testSetupEvents() {
    final Scenario s = Scenario.builder()
      .addEventTypes(asList(EVENT_A, EVENT_B, EVENT_C, EVENT_D))
      .addEvent(new TimedEvent(EVENT_A, 0))
      .addEvent(new TimedEvent(EVENT_B, -1))
      .addEvent(new TimedEvent(EVENT_B, 2))
      .addEvent(new TimedEvent(EVENT_A, 2))
      .addEvent(new TimedEvent(EVENT_C, -1))
      .addEvent(new TimedEvent(EVENT_C, 100))
      .build();

    Simulator sim = Simulator.builder()
      .setTickLength(1L)
      .setTimeUnit(SI.SECOND)
      .addModel(
        ScenarioController.builder(s)
          .withNumberOfTicks(1)
          .withEventHandler(TimedEvent.class, new NopHandler<>())
      )
      .build();

    final ListenerEventHistory leh = new ListenerEventHistory();
    ScenarioController sc = sim.getModelProvider().getModel(
      ScenarioController.class);
    sc.getEventAPI().addListener(leh);
    sim.start();
    assertThat(leh.getEventTypeHistory())
      .containsExactly(EVENT_B, EVENT_C, SCENARIO_STARTED, EVENT_A).inOrder();

  }

  /**
   * check whether the start event was generated. following scenario is
   * interrupted after 3rd step so there are some events left
   */
  @Test
  public void testStartEventGenerated() {

    Simulator sim = Simulator.builder()
      .setTickLength(1L)
      .setTimeUnit(SI.SECOND)
      .addModel(
        ScenarioController.builder(scenario)
          .withNumberOfTicks(3)
          .withEventHandler(TimedEvent.class, new NopHandler<>())
      )
      .build();

    final boolean[] r = new boolean[1];
    final int[] i = new int[1];

    controller = sim.getModelProvider().getModel(ScenarioController.class);

    controller.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        if (e.getEventType() == ScenarioController.EventType.SCENARIO_STARTED) {
          r[0] = true;
        } else if (!r[0]) {

          fail();
        } else {
          i[0] += 1;
        }
      }
    });

    controller.clock.tick();
    assertThat(r[0]).isTrue();
    assertThat(i[0]).isEqualTo(3);
  }

  /**
   * Test run of whole scenario.
   */
  @Test
  public void runningWholeScenario() {
    final Simulator sim = Simulator.builder()
      .setTickLength(1L)
      .setTimeUnit(SI.SECOND)
      .addModel(
        ScenarioController.builder(scenario)
          .withNumberOfTicks(-1)
          .withEventHandler(TimedEvent.class, new NopHandler<>())
      )
      .build();

    final boolean[] r = new boolean[1];
    final int[] i = new int[1];
    controller = sim.getModelProvider().getModel(ScenarioController.class);
    controller.getEventAPI().addListener(new Listener() {

      @Override
      public void handleEvent(Event e) {
        if (e.getEventType() == ScenarioController.EventType.SCENARIO_FINISHED) {
          synchronized (controller) {
            r[0] = true;
            sim.stop();
          }
        } else {
          i[0] += 1;
        }
      }
    });
    sim.start();

    assertThat(r[0]).isTrue();
    assertThat(i[0]).isEqualTo(scenario.getEvents().size() + 1);
    assertThat(controller.isScenarioFinished()).isTrue();

    sim.stop();
  }

  static class NopHandler<T extends TimedEvent> implements TimedEventHandler<T> {
    @Override
    public void handleTimedEvent(T event, SimulatorAPI simulator) {}
  }
}
