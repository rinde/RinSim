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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.Scenario.ProblemClass;
import com.github.rinde.rinsim.scenario.Scenario.SimpleProblemClass;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

/**
 * Tests for {@link Scenario} and its builder.
 * @author Rinde van Lon 
 */
public class ScenarioTest {

  /**
   * Test the default settings of a scenario.
   */
  @Test
  public void testDefaults() {
    final Scenario scenario = Scenario
        .builder(Scenario.DEFAULT_PROBLEM_CLASS)
        .addEventType(FakeEventType.A)
        .build();

    assertEquals(SI.KILOMETER, scenario.getDistanceUnit());
    assertTrue(scenario.getModelSuppliers().isEmpty());
    assertEquals(newHashSet(FakeEventType.A), scenario.getPossibleEventTypes());
    assertSame(Scenario.DEFAULT_PROBLEM_CLASS, scenario.getProblemClass());
    assertEquals("", scenario.getProblemInstanceId());
    assertEquals(NonSI.KILOMETERS_PER_HOUR, scenario.getSpeedUnit());
    assertEquals(Predicates.alwaysFalse(), scenario.getStopCondition());
    assertEquals(1000L, scenario.getTickSize());
    assertEquals(SI.MILLI(SI.SECOND), scenario.getTimeUnit());
    assertEquals(new TimeWindow(0, 8 * 60 * 60 * 1000),
        scenario.getTimeWindow());
  }

  /**
   * Test correct ordering of events.
   */
  @Test
  public void testAddEvents() {
    final TimedEvent ev0 = new TimedEvent(FakeEventType.A, 0);
    final TimedEvent ev1 = new TimedEvent(FakeEventType.B, 205);
    final TimedEvent ev2 = new TimedEvent(FakeEventType.B, 7);
    final TimedEvent ev3 = new TimedEvent(FakeEventType.B, 203);
    final Scenario scenario = Scenario
        .builder(Scenario.DEFAULT_PROBLEM_CLASS)
        .addEvent(ev0)
        .addEvent(ev1)
        .addEvents(asList(ev2, ev3))
        // redundant adding of event types, should not make any difference
        .addEventTypes(asList(FakeEventType.A, FakeEventType.B))
        .build();
    assertEquals(asList(ev0, ev2, ev3, ev1), scenario.asList());
    assertEquals(newHashSet(FakeEventType.A, FakeEventType.B),
        scenario.getPossibleEventTypes());
  }

  /**
   * Test all modifying methods.
   */
  @Test
  public void testCustomProperties() {
    final ProblemClass problemClass = new FakeProblemClass();
    final Scenario scenario = Scenario
        .builder(problemClass)
        .distanceUnit(SI.PICO(SI.METER))
        .speedUnit(NonSI.MACH)
        .timeUnit(NonSI.DAY_SIDEREAL)
        .instanceId("crazyfast")
        .scenarioLength(7L)
        .tickSize(1)
        .addEventType(PDPScenarioEvent.TIME_OUT)
        .stopCondition(Predicates.<Simulator> alwaysTrue())
        .addModel(PlaneRoadModel.supplier(new Point(6, 6), new Point(1034,
            32), SI.METER, Measure.valueOf(1d, SI.METERS_PER_SECOND)))
        .build();

    assertEquals(SI.PICO(SI.METER), scenario.getDistanceUnit());
    assertEquals(NonSI.MACH, scenario.getSpeedUnit());
    assertEquals(NonSI.DAY_SIDEREAL, scenario.getTimeUnit());
    assertEquals("crazyfast", scenario.getProblemInstanceId());
    assertEquals(new TimeWindow(0L, 7L), scenario.getTimeWindow());
    assertEquals(1L, scenario.getTickSize());
    assertEquals(newHashSet(PDPScenarioEvent.TIME_OUT),
        scenario.getPossibleEventTypes());
    assertEquals(Predicates.alwaysTrue(), scenario.getStopCondition());
    assertEquals(1, scenario.getModelSuppliers().size());
    assertTrue(scenario.getModelSuppliers().get(0).get() instanceof PlaneRoadModel);

    final Scenario.Builder builder = Scenario
        .builder(Scenario.DEFAULT_PROBLEM_CLASS)
        .copyProperties(scenario);

    final Scenario copiedScen = builder.build();
    assertEquals(scenario, copiedScen);

    final Scenario builderCopyScen = Scenario.builder(builder,
        Scenario.DEFAULT_PROBLEM_CLASS)
        .addEventType(PDPScenarioEvent.TIME_OUT)
        .build();

    assertNotEquals(copiedScen, builderCopyScen);
  }

  /**
   * Test no arg constructor.
   */
  @Test
  public void testNoArgConstructor() {
    final Scenario scenario = new EmptyScenario();
    assertTrue(scenario.asList().isEmpty());
  }

  /**
   * Test for correct behavior of
   * {@link Scenario.Builder#ensureFrequency(Predicate, int)}.
   */
  @Test
  public void testModifyEventsMethods() {
    final TimedEvent ev1 = new TimedEvent(PDPScenarioEvent.TIME_OUT, 10000L);
    final TimedEvent ev2 = new CustomEvent(FakeEventType.A, 7);
    final TimedEvent ev3 = new CustomEvent(FakeEventType.A, 3);
    final TimedEvent ev3b = new CustomEvent(FakeEventType.A, 3);
    final TimedEvent ev4 = new CustomEvent(FakeEventType.B, 3);
    final TimedEvent ev5 = new CustomEvent(FakeEventType.B, 367);
    final List<TimedEvent> events = asList(ev1, ev2, ev3, ev3b, ev4, ev5);

    final Scenario.Builder b = Scenario
        .builder(Scenario.DEFAULT_PROBLEM_CLASS)
        .addEvents(events)
        .clearEvents();
    assertTrue(b.eventList.isEmpty());
    assertTrue(b.eventTypeSet.isEmpty());
    b.addEvents(events)
        .ensureFrequency(Predicates.equalTo(ev3), 1);
    assertEquals(asList(ev1, ev2, ev4, ev5, ev3), b.eventList);

    // should add two instances of ev1
    b.ensureFrequency(Predicates.equalTo(ev1), 3);
    assertEquals(asList(ev1, ev2, ev4, ev5, ev3, ev1, ev1), b.eventList);

    // frequency already achieved, nothing should change
    b.ensureFrequency(Predicates.equalTo(ev1), 3);
    assertEquals(asList(ev1, ev2, ev4, ev5, ev3, ev1, ev1), b.eventList);

    // only custom event instances remain
    b.filterEvents(Predicates.instanceOf(CustomEvent.class));
    assertEquals(asList(ev2, ev4, ev5, ev3), b.eventList);
  }

  /**
   * Negative frequency is not allowed.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEnsureFrequencyFailFrequency() {
    Scenario.builder(Scenario.DEFAULT_PROBLEM_CLASS)
        .ensureFrequency(Predicates.<TimedEvent> alwaysTrue(), -1);
  }

  /**
   * Empty events list is not allowed.
   */
  @Test(expected = IllegalStateException.class)
  public void testEnsureFrequencyFailEmptyEventsList() {
    Scenario.builder(Scenario.DEFAULT_PROBLEM_CLASS)
        .ensureFrequency(Predicates.<TimedEvent> alwaysTrue(), 1);
  }

  /**
   * Filter must match at least one event.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEnsureFrequencyFailFilter1() {
    Scenario.builder(Scenario.DEFAULT_PROBLEM_CLASS)
        .addEvent(new CustomEvent(FakeEventType.A, 0))
        .ensureFrequency(Predicates.<TimedEvent> alwaysFalse(), 1);
  }

  /**
   * Filter matches must be equal.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEnsureFrequencyFailFilter2() {
    Scenario
        .builder(Scenario.DEFAULT_PROBLEM_CLASS)
        .addEvent(new CustomEvent(FakeEventType.A, 0))
        .addEvent(new CustomEvent(FakeEventType.A, 1))
        .ensureFrequency(Predicates.instanceOf(CustomEvent.class), 1);
  }

  /**
   * Tests for {@link SimpleProblemClass}.
   */
  @Test
  public void testSimpleProblemClass() {
    final ProblemClass pc = new SimpleProblemClass("hello world");
    assertEquals("hello world", pc.getId());
    assertTrue(pc.toString().contains("hello world"));
  }

  static class CustomEvent extends TimedEvent {
    CustomEvent(Enum<?> type, long timestamp) {
      super(type, timestamp);
    }
  }

  static class EmptyScenario extends Scenario {

    @Override
    public ImmutableList<? extends Supplier<? extends Model<?>>> getModelSuppliers() {
      throw new UnsupportedOperationException();
    }

    @Override
    public TimeWindow getTimeWindow() {
      throw new UnsupportedOperationException();
    }

    @Override
    public long getTickSize() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Predicate<Simulator> getStopCondition() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Unit<Duration> getTimeUnit() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Unit<Velocity> getSpeedUnit() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Unit<Length> getDistanceUnit() {
      throw new UnsupportedOperationException();
    }

    @Override
    public ProblemClass getProblemClass() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getProblemInstanceId() {
      throw new UnsupportedOperationException();
    }
  }

  static class FakeProblemClass implements ProblemClass {
    @Override
    public String getId() {
      return "fake";
    }
  }

  /**
   * Test equals method with differing events.
   */
  @Test
  public void testEqualsEvents() {
    final List<TimedEvent> events1 = newArrayList(new TimedEvent(
        FakeEventType.A, 0));
    final List<TimedEvent> events2 = newArrayList(new TimedEvent(
        FakeEventType.A, 0));
    final List<TimedEvent> events3 = newArrayList(new TimedEvent(
        FakeEventType.A, 1));
    final List<TimedEvent> events4 = newArrayList(new TimedEvent(
        FakeEventType.A, 1),
        new TimedEvent(FakeEventType.A, 2));

    final Scenario s1 = Scenario.builder().addEvents(events1).build();
    final Scenario s2 = Scenario.builder().addEvents(events2).build();
    final Scenario s3 = Scenario.builder().addEvents(events3).build();
    final Scenario s4 = Scenario.builder().addEvents(events4).build();

    assertNotEquals(s1, new Object());
    assertEquals(s1, s2);
    assertNotEquals(s1, s3);
    assertNotEquals(s1, s4);
  }

  /**
   * Tests whether events are sorted by time.
   */
  @Test
  public void testSorting() {
    final List<TimedEvent> events = new ArrayList<>(10);
    final AddObjectEvent A1 = new AddObjectEvent(0, new Point(1, 0));
    final AddObjectEvent A2 = new AddObjectEvent(0, new Point(2, 0));
    final AddObjectEvent B = new AddObjectEvent(1, new Point(1, 1));
    final AddObjectEvent C = new AddObjectEvent(2, new Point(1, 0));
    final AddObjectEvent D1 = new AddObjectEvent(3, new Point(1, 2));
    final AddObjectEvent D2 = new AddObjectEvent(3, new Point(1, 3));
    final AddObjectEvent E = new AddObjectEvent(4, new Point(2, 0));
    final AddObjectEvent F = new AddObjectEvent(5, new Point(4, 0));
    events.addAll(asList(A1, A2, B, C, D1, D2, E, F));
    Collections.reverse(events);

    final Scenario s = Scenario.builder().addEvents(events).build();
    final List<TimedEvent> res = newArrayList(s.asList());

    assertEquals(asList(A2, A1, B, C, D2, D1, E, F), res);
    assertFalse(res.equals(events));
    assertEquals(events.size(), res.size());
    Collections.reverse(res);
    assertEquals(res, events);
  }

  /**
   * Test copying by builder.
   */
  @Test
  public void testCreateScenarioByCopying() {
    final Scenario s = Scenario.builder()
        .addEventType(FakeEventType.A)
        .addEvent(new AddObjectEvent(100, new Point(0, 0)))
        .addEvent(new AddObjectEvent(200, new Point(0, 0)))
        .addEvent(new AddObjectEvent(300, new Point(0, 0)))
        .build();

    assertEquals(3, s.asList().size());

    final Scenario s2 = Scenario.builder(s).build();

    assertEquals(3, s.asList().size());
    assertEquals(3, s2.asList().size());
  }

  /**
   * Test equals of {@link TimedEvent}.
   */
  @Test
  public void timedEventEquals() {
    final TimedEvent a10 = new TimedEvent(FakeEventType.A, 10);
    final TimedEvent b10 = new TimedEvent(FakeEventType.B, 10);

    assertNotEquals(new AddObjectEvent(10, new Point(10, 0)),
        a10);
    assertNotEquals(a10, null);
    assertNotEquals(a10, b10);
    assertEquals(b10, new TimedEvent(FakeEventType.B, 10));
  }

  static class AddObjectEvent extends TimedEvent {
    public final Point pos;

    public AddObjectEvent(String[] parts) {
      this(Long.parseLong(parts[1]), Point.parsePoint(parts[2]));
    }

    public AddObjectEvent(long pTime, Point pPos) {
      super(FakeEventType.A, pTime);
      pos = pPos;
      hashCode();
      toString();
    }

    @Override
    public String toString() {
      return super.toString() + "|" + pos;
    }
  }

  enum FakeEventType {
    A, B;
  }
}
