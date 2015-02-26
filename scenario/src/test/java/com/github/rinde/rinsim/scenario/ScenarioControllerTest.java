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

import static com.github.rinde.rinsim.scenario.ScenarioController.EventType.SCENARIO_FINISHED;
import static com.github.rinde.rinsim.scenario.ScenarioController.EventType.SCENARIO_STARTED;
import static com.github.rinde.rinsim.scenario.ScenarioControllerTest.TestEvents.EVENT_A;
import static com.github.rinde.rinsim.scenario.ScenarioControllerTest.TestEvents.EVENT_B;
import static com.github.rinde.rinsim.scenario.ScenarioControllerTest.TestEvents.EVENT_C;
import static com.github.rinde.rinsim.scenario.ScenarioControllerTest.TestEvents.EVENT_D;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Set;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.TimeLapseFactory;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.event.ListenerEventHistory;
import com.github.rinde.rinsim.scenario.ScenarioController.UICreator;

public class ScenarioControllerTest {

  @SuppressWarnings("null")
  protected ScenarioController controller;
  @SuppressWarnings("null")
  protected Scenario scenario;
  @SuppressWarnings("null")
  protected Simulator simulator;

  public enum TestEvents {
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
    ScenarioController.EventType.valueOf("SCENARIO_STARTED");
    simulator = new Simulator(new MersenneTwister(123),
        Measure.valueOf(1L, SI.SECOND));
  }

  @Test(expected = IllegalStateException.class)
  public void testEmptyController() {
    controller = new ScenarioController(scenario, simulator, new TestHandler(),
        3);
    controller.tick(TimeLapseFactory.create(0, 1));
  }

  // @Test(expected = ConfigurationException.class)
  // public void initializeFail() throws ConfigurationException {
  // final ScenarioController sc = new ScenarioController(scenario, 1) {
  // @Override
  // protected Simulator createSimulator() throws Exception {
  // throw new RuntimeException("this is what we want");
  // }
  //
  // @Override
  // protected boolean handleTimedEvent(TimedEvent event) {
  // return true;
  // }
  // };
  // sc.initialize();
  // }

  @Test
  public void handleTimedEvent() {
    final ScenarioController sc = new ScenarioController(scenario, simulator,
        new TestHandler(), 1);

    assertFalse(sc.timedEventHandler
        .handleTimedEvent(new TimedEvent(EVENT_A, 0)));
    assertFalse(sc.timedEventHandler
        .handleTimedEvent(new TimedEvent(EVENT_B, 0)));
    assertFalse(sc.timedEventHandler
        .handleTimedEvent(new TimedEvent(EVENT_C, 0)));
    assertFalse(sc.timedEventHandler
        .handleTimedEvent(new TimedEvent(EVENT_D, 0)));
  }

  @Test(expected = IllegalStateException.class)
  public void eventNotHandled() {
    final ScenarioController sc = new ScenarioController(scenario, simulator,
        new TestHandler(), 1);
    sc.disp.dispatchEvent(new TimedEvent(EVENT_A, 0));
  }

  @Test
  public void finiteSimulation() throws InterruptedException {
    final ScenarioController sc = new ScenarioController(scenario, simulator,
        new TestHandler(TestEvents.values()), 101);

    final ListenerEventHistory leh = new ListenerEventHistory();
    sc.getEventAPI().addListener(leh);
    assertFalse(sc.isScenarioFinished());
    sc.start();
    assertEquals(
        asList(SCENARIO_STARTED, EVENT_A, EVENT_B, EVENT_B, EVENT_A, EVENT_C,
            EVENT_C, SCENARIO_FINISHED), leh
            .getEventTypeHistory());

    assertTrue(sc.isScenarioFinished());
    sc.stop();
    final long before = sc.simulator.getCurrentTime();
    sc.start();// should have no effect

    assertEquals(before, sc.simulator.getCurrentTime());
    final TimeLapse emptyTime = TimeLapseFactory.create(0, 1);
    emptyTime.consumeAll();
    sc.tick(emptyTime);
  }

  @Test
  public void fakeUImode() {
    final ScenarioController sc = new ScenarioController(scenario, simulator,
        new TestHandler(TestEvents.values()), 3);
    sc.enableUI(new UICreator() {

      @Override
      public void createUI(Simulator sim) {
        // TODO Auto-generated method stub

      }
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
    final ScenarioController sc = new ScenarioController(s, simulator, th, 1);
    sc.start();
    assertEquals(asList(EVENT_B, EVENT_C, EVENT_A), th.eventTypes);

  }

  /**
   * check whether the start event was generated. following scenario is
   * interrupted after 3rd step so there are some events left
   */
  @Test
  public void testStartEventGenerated() {
    controller = new ScenarioController(scenario, simulator, new TestHandler(
        EVENT_A, EVENT_B), 3);

    // {
    //
    // @Override
    // protected boolean handleTimedEvent(TimedEvent event) {
    // if (event.getEventType() == EVENT_A
    // || event.getEventType() == EVENT_B) {
    // return true;
    // }
    //
    // return super.handleTimedEvent(event);
    // }
    //
    // };

    final boolean[] r = new boolean[1];
    final int[] i = new int[1];

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

    controller.simulator.tick();
    assertTrue("event generated", r[0]);
    assertEquals(3, i[0]);
  }

  @Test
  public void runningWholeScenario() throws InterruptedException {
    controller = new ScenarioController(scenario, simulator, new TestHandler(
        EVENT_A, EVENT_B, EVENT_C), -1);

    final boolean[] r = new boolean[1];
    final int[] i = new int[1];

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
