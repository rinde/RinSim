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
package com.github.rinde.rinsim.scenario.gendreau06;

import java.util.List;
import java.util.Set;

import javax.measure.Measure;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.pdptw.common.StatsStopConditions;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.StopCondition;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 *
 * The length of the scenario is a soft constraint. There is a pre defined
 * length of the day (either 4 hours or 7.5 hours), vehicles are allowed to
 * continue driving after the end of the day.
 * <p>
 * Once a vehicle is moving towards a Parcel it is obliged to service it. This
 * means that diversion is not allowed.
 * <p>
 * Distance is expressed in km, time is expressed in ms (the original format is
 * in seconds, however it allows fractions as such it was translated to ms),
 * speed is expressed as km/h.
 * @author Rinde van Lon
 */
@AutoValue
public abstract class Gendreau06Scenario extends Scenario {
  static final Point MIN = new Point(0, 0);
  static final Point MAX = new Point(5, 5);
  static final Measure<Double, Velocity> MAX_SPEED = Measure.valueOf(
    30d, NonSI.KILOMETERS_PER_HOUR);

  static Gendreau06Scenario create(List<? extends TimedEvent> pEvents,
    Set<Enum<?>> pSupportedTypes, long ts, GendreauProblemClass problemClass,
    int instanceNumber, boolean diversion) {

    return new AutoValue_Gendreau06Scenario(
      ImmutableList.<TimedEvent> copyOf(pEvents),
      ImmutableSet.<Enum<?>> copyOf(pSupportedTypes),
      problemClass, Integer.toString(instanceNumber), ts, diversion);
  }

  abstract long getTickSize();

  abstract boolean getAllowDiversion();

  @Override
  public TimeWindow getTimeWindow() {
    return TimeWindow.ALWAYS;
  }

  @Override
  public StopCondition getStopCondition() {
    return StopConditions.and(
      StatsStopConditions.vehiclesDoneAndBackAtDepot(),
      StatsStopConditions.timeOutEvent()
      );
  }

  @Override
  public ImmutableSet<ModelBuilder<?, ?>> getModelBuilders() {
    return ImmutableSet.<ModelBuilder<?, ?>> builder()
      .add(
        TimeModel.builder()
          .withTickLength(getTickSize())
          .withTimeUnit(SI.MILLI(SI.SECOND))
      )
      .add(
        PDPRoadModel.builder(
          RoadModelBuilders.plane()
            .withMinPoint(MIN)
            .withMaxPoint(MAX)
            .withDistanceUnit(SI.KILOMETER)
            .withSpeedUnit(MAX_SPEED.getUnit())
            .withMaxSpeed(MAX_SPEED.getValue())
          )
          .withAllowVehicleDiversion(getAllowDiversion())
      )
      .add(
        DefaultPDPModel.builder()
          .withTimeWindowPolicy(TimeWindowPolicies.TARDY_ALLOWED)
      )
      .build();
  }
}
