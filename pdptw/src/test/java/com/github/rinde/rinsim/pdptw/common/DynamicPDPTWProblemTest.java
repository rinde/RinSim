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
package com.github.rinde.rinsim.pdptw.common;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.List;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem.Creator;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem.StopConditions;
import com.github.rinde.rinsim.scenario.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon
 *
 */
public class DynamicPDPTWProblemTest {

  protected static final Creator<AddVehicleEvent> DUMMY_ADD_VEHICLE_EVENT_CREATOR = new Creator<AddVehicleEvent>() {
    @Override
    public boolean create(Simulator sim, AddVehicleEvent event) {
      return true;
    }
  };

  /**
   * Checks for the absence of a creator for AddVehicleEvent.
   */
  @Test(expected = IllegalStateException.class)
  public void noVehicleCreator() {
    final List<TimedEvent> events = asList(new TimedEvent(
      PDPScenarioEvent.ADD_DEPOT, 10));
    new DynamicPDPTWProblem(new DummyScenario(events), 123,
      ImmutableList.<ModelBuilder<?, ?>> of()).simulate();
  }

  @Test
  public void testStopCondition() {
    final List<TimedEvent> events = asList(new TimedEvent(
      PDPScenarioEvent.ADD_DEPOT, 10));
    final DynamicPDPTWProblem prob = new DynamicPDPTWProblem(new DummyScenario(
      events), 123, ImmutableList.<ModelBuilder<?, ?>> of());
    prob.addCreator(AddVehicleEvent.class, DUMMY_ADD_VEHICLE_EVENT_CREATOR);

    prob.addStopCondition(new TimeStopCondition(4));
    final StatisticsDTO stats = prob.simulate();

    assertEquals(5, stats.simulationTime);
  }

  class TimeStopCondition implements Predicate<Simulator> {
    protected final long time;

    public TimeStopCondition(long t) {
      time = t;
    }

    @Override
    public boolean apply(Simulator context) {
      return DynamicPDPTWProblem.getStats(context).simulationTime == time;
    }
  }

  class DummyScenario extends Scenario {

    public DummyScenario(List<TimedEvent> events) {
      super(events, new HashSet<Enum<?>>(
        java.util.Arrays.asList(PDPScenarioEvent.values())));
    }

    @Override
    public TimeWindow getTimeWindow() {
      return TimeWindow.ALWAYS;
    }

    @Override
    public StopConditions getStopCondition() {
      return StopConditions.TIME_OUT_EVENT;
    }

    @Override
    public ImmutableList<? extends ModelBuilder<?, ?>> getModelBuilders() {
      return ImmutableList.<ModelBuilder<?, ?>> builder()
        .add(
          TimeModel.builder()
            .setTickLength(1L)
            .setTimeUnit(SI.SECOND)
        )
        .add(
          RoadModelBuilders.plane()
            .setMinPoint(new Point(0, 0))
            .setMaxPoint(new Point(10, 10))
            .setDistanceUnit(SI.KILOMETER)
            .setMaxSpeed(1d)
            .setSpeedUnit(NonSI.KILOMETERS_PER_HOUR)
        )
        .add(
          DefaultPDPModel.builder()
            .setTimeWindowPolicy(TimeWindowPolicies.TARDY_ALLOWED)
        )
        .build();
    }

    @Override
    public ProblemClass getProblemClass() {
      throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public String getProblemInstanceId() {
      throw new UnsupportedOperationException("Not implemented.");
    }

  }

}
