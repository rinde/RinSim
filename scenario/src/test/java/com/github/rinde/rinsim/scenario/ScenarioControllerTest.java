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
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.time.ClockController;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeLapseFactory;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.event.ListenerEventHistory;
import com.github.rinde.rinsim.scenario.ScenarioController.UICreator;
import com.github.rinde.rinsim.testutil.TestUtil;

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

  @Before
  public void setUp() throws Exception {
    scenario = Scenario.builder()
      .addEventTypes(asList(EVENT_A, EVENT_B, EVENT_C, EVENT_D))
      .addEvent(new TimedEvent(EVENT_A, 0))
      .addEvent(new TimedEvent(EVENT_B, 0))
      .addEvent(new TimedEvent(EVENT_B, 0))
      .addEvent(new TimedEvent(EVENT_A, 1))
      .addEvent(new TimedEvent(EVENT_C, 5))
      .addEvent(new TimedEvent(EVENT_C, 100))
      .build();
    assertNotNull(scenario);

    TestUtil.testEnum(ScenarioController.EventType.class);

    ClockController clock = mock(ClockController.class);
    when(clock.getEventAPI()).thenReturn(mock(EventAPI.class));
    SimulatorAPI sim = mock(SimulatorAPI.class);

    dependencyProvider = mock(DependencyProvider.class);
    when(dependencyProvider.get(ClockController.class)).thenReturn(clock);
    when(dependencyProvider.get(SimulatorAPI.class)).thenReturn(sim);
  }

  /**
   * Tests that a not handled event results in a {@link IllegalStateException}.
   */
  @Test
  public void testEventNotHandled() {
    controller = ScenarioController.builder()
      .setScenario(scenario)
      .setEventHandler(new TestHandler())
      .setNumberOfTicks(3)
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

  @Test
  public void handleTimedEvent() {
    final ScenarioController sc =
      ScenarioController.builder()
        .setScenario(scenario)
        .setNumberOfTicks(1)
        .setEventHandler(new TestHandler())
        .build(dependencyProvider);

    assertFalse(sc.timedEventHandler
      .handleTimedEvent(new TimedEvent(EVENT_A, 0)));
    assertFalse(sc.timedEventHandler
      .handleTimedEvent(new TimedEvent(EVENT_B, 0)));
    assertFalse(sc.timedEventHandler
      .handleTimedEvent(new TimedEvent(EVENT_C, 0)));
    assertFalse(sc.timedEventHandler
      .handleTimedEvent(new TimedEvent(EVENT_D, 0)));
  }

  @Test
  public void finiteSimulation() throws InterruptedException {
    final ScenarioController.Builder scb = ScenarioController.builder()
      .setScenario(scenario)
      .setEventHandler(new TestHandler(TestEvents.values()))
      .setNumberOfTicks(101);

    Simulator sim = Simulator.builder()
      .setTickLength(1L)
      .setTimeUnit(SI.SECOND)
      .addModel(scb)
      .build();

    ScenarioController sc = sim.getModelProvider().getModel(
      ScenarioController.class);

    final ListenerEventHistory leh = new ListenerEventHistory();
    sc.getEventAPI().addListener(leh);
    assertFalse(sc.isScenarioFinished());
    sc.start();
    assertEquals(
      asList(SCENARIO_STARTED, EVENT_A, EVENT_B, EVENT_B, EVENT_A, EVENT_C,
        EVENT_C, SCENARIO_FINISHED), leh.getEventTypeHistory());

    assertTrue(sc.isScenarioFinished());
    sc.stop();
    final long before = sc.clock.getCurrentTime();
    sc.start();// should have no effect

    assertEquals(before, sc.clock.getCurrentTime());
    final TimeLapse emptyTime = TimeLapseFactory.create(0, 1);
    emptyTime.consumeAll();
    sc.tick(emptyTime);
  }

  @Test
  public void fakeUImode() {
    Simulator sim = Simulator.builder()
      .setTickLength(1L)
      .setTimeUnit(SI.SECOND)
      .addModel(
        ScenarioController.builder()
          .setScenario(scenario)
          .setNumberOfTicks(3)
          .setEventHandler(new TestHandler(TestEvents.values()))
      )
      .build();

    ScenarioController sc = sim.getModelProvider().getModel(
      ScenarioController.class);

    sc.enableUI(new UICreator() {
      @Override
      public void createUI(Simulator simulator) {}
    });

    sc.start();
    sc.stop();
    final TimeLapse emptyTime = TimeLapseFactory.create(0, 1);
    emptyTime.consumeAll();
    sc.tick(emptyTime);
  }

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

    final EventHistory th = new EventHistory();
    Simulator sim = Simulator.builder()
      .setTickLength(1L)
      .setTimeUnit(SI.SECOND)
      .addModel(
        ScenarioController.builder()
          .setScenario(s)
          .setNumberOfTicks(1)
          .setEventHandler(th)
      )
      .build();

    ScenarioController sc = sim.getModelProvider().getModel(
      ScenarioController.class);
    sc.start();
    assertEquals(asList(EVENT_B, EVENT_C, EVENT_A), th.eventTypes);

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
        ScenarioController.builder()
          .setScenario(scenario)
          .setNumberOfTicks(3)
          .setEventHandler(new TestHandler(EVENT_A, EVENT_B))
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
    assertTrue("event generated", r[0]);
    assertEquals(3, i[0]);
  }

  @Test
  public void runningWholeScenario() throws InterruptedException {
    Simulator sim = Simulator.builder()
      .setTickLength(1L)
      .setTimeUnit(SI.SECOND)
      .addModel(
        ScenarioController.builder()
          .setScenario(scenario)
          .setNumberOfTicks(-1)
          .setEventHandler(new TestHandler(EVENT_A, EVENT_B, EVENT_C))
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
            controller.stop();
          }
        } else {
          i[0] += 1;
        }
      }
    });

    controller.start();

    assertTrue(r[0]);
    assertEquals(scenario.asList().size() + 1, i[0]);
    assertTrue(controller.isScenarioFinished());

    controller.stop();
  }

  class TestHandler implements TimedEventHandler {
    Set<Enum<?>> types;

    public TestHandler(Enum<?>... handledTypes) {
      types = newHashSet(handledTypes);
    }

    @Override
    public boolean handleTimedEvent(TimedEvent event) {
      return types.contains(event.getEventType());
    }
  }

  class EventHistory implements TimedEventHandler {

    protected final List<TimedEvent> eventList;
    protected final List<Enum<?>> eventTypes;

    public EventHistory() {
      eventList = newArrayList();
      eventTypes = newArrayList();
    }

    @Override
    public boolean handleTimedEvent(TimedEvent event) {
      eventList.add(event);
      eventTypes.add(event.getEventType());
      return true;
    }

  }

}
