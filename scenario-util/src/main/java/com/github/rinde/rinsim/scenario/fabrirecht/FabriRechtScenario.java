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
package com.github.rinde.rinsim.scenario.fabrirecht;

import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.StatsStopConditions;
import com.github.rinde.rinsim.pdptw.common.StatsTracker;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.StopCondition;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * A scenario for Fabri {@literal &} Recht problems.
 * @author Rinde van Lon
 */
@AutoValue
public abstract class FabriRechtScenario extends Scenario {
  static final double MAX_SPEED = 100d;

  /**
   * @return Minimum position.
   */
  public abstract Point getMin();

  /**
   * @return Maximum position.
   */
  public abstract Point getMax();

  /**
   * @return The default vehicle.
   */
  public abstract VehicleDTO getDefaultVehicle();

  @Override
  public StopCondition getStopCondition() {
    return StatsStopConditions.timeOutEvent();
  }

  @Override
  public ImmutableSet<ModelBuilder<?, ?>> getModelBuilders() {
    return ImmutableSet.<ModelBuilder<?, ?>>builder()
      .add(
        TimeModel.builder()
          .withTickLength(1L)
          .withTimeUnit(NonSI.MINUTE))
      .add(
        RoadModelBuilders.plane()
          .withMinPoint(getMin())
          .withMaxPoint(getMax())
          .withDistanceUnit(SI.KILOMETER)
          .withMaxSpeed(MAX_SPEED)
          .withSpeedUnit(
            SI.KILOMETRE.divide(NonSI.MINUTE).asType(Velocity.class)))
      .add(
        DefaultPDPModel.builder()
          .withTimeWindowPolicy(TimeWindowPolicies.TARDY_ALLOWED))
      .add(
        StatsTracker.builder())
      .build();
  }

  @Override
  public ProblemClass getProblemClass() {
    return FabriRechtProblemClass.SINGLETON;
  }

  @Override
  public String getProblemInstanceId() {
    return "1";
  }

  /**
   * Create a new scenario.
   * @param pEvents The event list.
   * @param pMin {@link #getMin()}.
   * @param pMax {@link #getMax()}.
   * @param pTimeWindow {@link #getTimeWindow()}.
   * @param pDefaultVehicle {@link #getDefaultVehicle()}.
   * @return a new instance.
   */
  public static FabriRechtScenario create(
      Iterable<? extends TimedEvent> pEvents, Point pMin, Point pMax,
      TimeWindow pTimeWindow, VehicleDTO pDefaultVehicle) {

    return new AutoValue_FabriRechtScenario(
      ImmutableList.<TimedEvent>copyOf(pEvents),
      pTimeWindow,
      pMin,
      pMax,
      pDefaultVehicle);
  }

  enum FabriRechtProblemClass implements ProblemClass {
    SINGLETON;

    @Override
    public String getId() {
      return "fabrirecht";
    }
  }
}
