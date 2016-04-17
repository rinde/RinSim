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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.measure.unit.SI;

import org.junit.Test;

import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.Scenario.ProblemClass;
import com.github.rinde.rinsim.scenario.Scenario.SimpleProblemClass;
import com.github.rinde.rinsim.scenario.ScenarioControllerTest.EventA;
import com.github.rinde.rinsim.scenario.ScenarioControllerTest.EventB;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.auto.value.AutoValue;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

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
    final Scenario.Builder builder = Scenario
      .builder(Scenario.DEFAULT_PROBLEM_CLASS);

    assertThat(builder.getTimeWindow())
      .isEqualTo(TimeWindow.create(0, 8 * 60 * 60 * 1000));
    assertThat(builder.getStopCondition())
      .isEqualTo(StopConditions.alwaysFalse());

    final Scenario scenario = builder.build();

    assertTrue(scenario.getModelBuilders().isEmpty());
    assertSame(Scenario.DEFAULT_PROBLEM_CLASS, scenario.getProblemClass());
    assertEquals("", scenario.getProblemInstanceId());
    assertThat(scenario.getStopCondition()).isEqualTo(
      StopConditions.alwaysFalse());
    assertEquals(TimeWindow.create(0, 8 * 60 * 60 * 1000),
      scenario.getTimeWindow());
  }

  /**
   * Test correct ordering of events.
   */
  @Test
  public void testAddEvents() {
    final TimedEvent ev0 = EventA.create(0);
    final TimedEvent ev1 = EventB.create(205);
    final TimedEvent ev2 = EventB.create(7);
    final TimedEvent ev3 = EventB.create(203);
    final Scenario scenario = Scenario
      .builder(Scenario.DEFAULT_PROBLEM_CLASS)
      .addEvent(ev0)
      .addEvent(ev1)
      .addEvents(asList(ev2, ev3))
      .build();
    assertEquals(asList(ev0, ev2, ev3, ev1), scenario.getEvents());
  }

  /**
   * Test all modifying methods.
   */
  @Test
  public void testCustomProperties() {
    final ProblemClass problemClass = new FakeProblemClass();
    final Scenario scenario = Scenario
      .builder(problemClass)
      .instanceId("crazyfast")
      .scenarioLength(7L)
      .setStopCondition(StopConditions.alwaysTrue())
      .addModel(
        RoadModelBuilders.plane()
          .withMinPoint(new Point(6, 6))
          .withMaxPoint(new Point(1034, 32))
          .withDistanceUnit(SI.METER)
          .withSpeedUnit(SI.METERS_PER_SECOND)
          .withMaxSpeed(1d))
      .build();

    assertThat(scenario.asQueue()).isEqualTo(scenario.getEvents());

    assertEquals("crazyfast", scenario.getProblemInstanceId());
    assertEquals(TimeWindow.create(0L, 7L), scenario.getTimeWindow());

    assertThat(scenario.getStopCondition()).isEqualTo(
      StopConditions.alwaysTrue());
    assertEquals(1, scenario.getModelBuilders().size());
    assertThat(scenario.getModelBuilders().iterator().next()
      .getAssociatedType()).isSameAs(RoadUser.class);

    final Scenario.Builder builder = Scenario
      .builder(Scenario.DEFAULT_PROBLEM_CLASS)
      .copyProperties(scenario);

    final Scenario copiedScen = builder.build();
    assertEquals(scenario, copiedScen);

    final Scenario builderCopyScen = Scenario.builder(builder,
      Scenario.DEFAULT_PROBLEM_CLASS)
      .build();

    assertNotEquals(copiedScen, builderCopyScen);
  }

  /**
   * Test no arg constructor.
   */
  @Test
  public void testNoArgConstructor() {
    final Scenario scenario = new EmptyScenario();
    assertTrue(scenario.getEvents().isEmpty());
  }

  /**
   * Test for correct behavior of
   * {@link Scenario.Builder#ensureFrequency(Predicate, int)}.
   */
  @Test
  public void testModifyEventsMethods() {
    final TimedEvent ev1 = TimeOutEvent.create(10000L);
    final TimedEvent ev2 = EventA.create(7);
    final TimedEvent ev3 = EventA.create(3);
    final TimedEvent ev3b = EventA.create(3);
    final TimedEvent ev4 = EventB.create(3);
    final TimedEvent ev5 = EventB.create(367);
    final List<TimedEvent> events = asList(ev1, ev2, ev3, ev3b, ev4, ev5);

    final Scenario.Builder b = Scenario
      .builder(Scenario.DEFAULT_PROBLEM_CLASS)
      .addEvents(events)
      .clearEvents();
    assertTrue(b.eventList.isEmpty());
    b.addEvents(events)
      .ensureFrequency(Predicates.equalTo(ev3), 1);
    assertEquals(asList(ev1, ev2, ev4, ev5, ev3), b.eventList);

    // should add two instances of ev1
    b.ensureFrequency(Predicates.equalTo(ev1), 3);
    assertEquals(asList(ev1, ev2, ev4, ev5, ev3, ev1, ev1), b.eventList);

    // frequency already achieved, nothing should change
    b.ensureFrequency(Predicates.equalTo(ev1), 3);
    assertEquals(asList(ev1, ev2, ev4, ev5, ev3, ev1, ev1), b.eventList);

    // only event b instances remain
    b.filterEvents(Predicates.instanceOf(EventB.class));
    assertEquals(asList(ev4, ev5), b.eventList);
  }

  /**
   * Negative frequency is not allowed.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEnsureFrequencyFailFrequency() {
    Scenario.builder(Scenario.DEFAULT_PROBLEM_CLASS)
      .ensureFrequency(Predicates.<TimedEvent>alwaysTrue(), -1);
  }

  /**
   * Empty events list is not allowed.
   */
  @Test(expected = IllegalStateException.class)
  public void testEnsureFrequencyFailEmptyEventsList() {
    Scenario.builder(Scenario.DEFAULT_PROBLEM_CLASS)
      .ensureFrequency(Predicates.<TimedEvent>alwaysTrue(), 1);
  }

  /**
   * Filter must match at least one event.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEnsureFrequencyFailFilter1() {
    Scenario.builder(Scenario.DEFAULT_PROBLEM_CLASS)
      .addEvent(EventA.create(0))
      .ensureFrequency(Predicates.<TimedEvent>alwaysFalse(), 1);
  }

  /**
   * Filter matches must be equal.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEnsureFrequencyFailFilter2() {
    Scenario
      .builder(Scenario.DEFAULT_PROBLEM_CLASS)
      .addEvent(EventA.create(0))
      .addEvent(EventA.create(1))
      .ensureFrequency(Predicates.instanceOf(EventA.class), 1);
  }

  /**
   * Tests for {@link SimpleProblemClass}.
   */
  @Test
  public void testSimpleProblemClass() {
    final ProblemClass pc = SimpleProblemClass.create("hello world");
    assertEquals("hello world", pc.getId());
    assertTrue(pc.toString().contains("hello world"));
  }

  static class EmptyScenario extends Scenario {

    @Override
    public ImmutableSet<ModelBuilder<?, ?>> getModelBuilders() {
      throw new UnsupportedOperationException();
    }

    @Override
    public TimeWindow getTimeWindow() {
      throw new UnsupportedOperationException();
    }

    @Override
    public StopCondition getStopCondition() {
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

    @Override
    public ImmutableList<TimedEvent> getEvents() {
      return ImmutableList.of();
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return other != null && other.getClass() == getClass();
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this);
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
    final List<EventA> events1 = asList(EventA.create(0));
    final List<EventA> events2 = asList(EventA.create(0));
    final List<EventA> events3 = asList(EventA.create(1));
    final List<EventA> events4 = asList(EventA.create(1), EventA.create(2));

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
    final AddObjectEvent A1 = AddObjectEvent.create(0, new Point(1, 0));
    final AddObjectEvent A2 = AddObjectEvent.create(0, new Point(2, 0));
    final AddObjectEvent B = AddObjectEvent.create(1, new Point(1, 1));
    final AddObjectEvent C = AddObjectEvent.create(2, new Point(1, 0));
    final AddObjectEvent D1 = AddObjectEvent.create(3, new Point(1, 2));
    final AddObjectEvent D2 = AddObjectEvent.create(3, new Point(1, 3));
    final AddObjectEvent E = AddObjectEvent.create(4, new Point(2, 0));
    final AddObjectEvent F = AddObjectEvent.create(5, new Point(4, 0));
    events.addAll(asList(A1, A2, B, C, D1, D2, E, F));
    Collections.reverse(events);

    final Scenario s = Scenario.builder().addEvents(events).build();
    final List<TimedEvent> res = newArrayList(s.getEvents());
    assertEquals(asList(A2, A1, B, C, D2, D1, E, F), res);
    assertFalse(res.equals(events));
    assertEquals(events.size(), res.size());
  }

  /**
   * Test copying by builder.
   */
  @Test
  public void testCreateScenarioByCopying() {
    final Scenario s = Scenario.builder()
      .addEvent(AddObjectEvent.create(100, new Point(0, 0)))
      .addEvent(AddObjectEvent.create(200, new Point(0, 0)))
      .addEvent(AddObjectEvent.create(300, new Point(0, 0)))
      .build();

    assertEquals(3, s.getEvents().size());

    final Scenario s2 = Scenario.builder(s).build();

    assertEquals(3, s.getEvents().size());
    assertEquals(3, s2.getEvents().size());
  }

  /**
   * Test equals of {@link TimedEvent}.
   */
  @Test
  public void timedEventEquals() {
    final TimedEvent a10 = EventA.create(10);
    final TimedEvent b10 = EventB.create(10);

    assertNotEquals(AddObjectEvent.create(10, new Point(10, 0)),
      a10);
    assertNotEquals(a10, null);
    assertNotEquals(a10, b10);
    assertEquals(b10, EventB.create(10));
  }

  /**
   * Tests the removal of model builders.
   */
  @Test
  public void testRemoveModelsOfType() {
    final Scenario.Builder builder = Scenario.builder();
    builder.addModel(TimeModel.builder())
      .addModel(TimeModel.builder().withRealTime())
      .addModel(RoadModelBuilders.plane())
      .addModel(CommModel.builder());
    assertThat(builder.modelBuilders).hasSize(4);

    builder.removeModelsOfType(RoadModelBuilders.PlaneRMB.class);
    assertThat(builder.modelBuilders).hasSize(3);
    assertThat(builder.modelBuilders).containsExactly(TimeModel.builder(),
      TimeModel.builder().withRealTime(), CommModel.builder());

    builder.removeModelsOfType(RoadModelBuilders.AbstractGraphRMB.class);
    builder.removeModelsOfType(TimeModel.AbstractBuilder.class);
    assertThat(builder.modelBuilders).hasSize(1);
    assertThat(builder.modelBuilders).containsExactly(CommModel.builder());

    builder.removeModelsOfType(CommModel.Builder.class);
    assertThat(builder.modelBuilders).isEmpty();
  }

  @AutoValue
  abstract static class AddObjectEvent implements TimedEvent {
    abstract Point getPoint();

    static AddObjectEvent create(long time, Point p) {
      return new AutoValue_ScenarioTest_AddObjectEvent(time, p);
    }
  }

  enum FakeEventType {
    A, B;
  }
}
