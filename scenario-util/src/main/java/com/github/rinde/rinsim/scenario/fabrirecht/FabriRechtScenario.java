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
package com.github.rinde.rinsim.scenario.fabrirecht;

import java.util.List;
import java.util.Set;

import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.core.pdptw.VehicleDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem.StopConditions;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.collect.ImmutableList;

/**
 * A scenario for Fabri {@literal &} Recht problems.
 * @author Rinde van Lon
 */
public class FabriRechtScenario extends Scenario {
  /**
   * Minimum position.
   */
  public final Point min;
  /**
   * Maximum position.
   */
  public final Point max;
  /**
   * Time window of the scenario.
   */
  public final TimeWindow timeWindow;
  /**
   * The default vehicle.
   */
  public final VehicleDTO defaultVehicle;

  FabriRechtScenario(Point pMin, Point pMax, TimeWindow pTimeWindow,
    VehicleDTO pDefaultVehicle) {
    super();
    min = pMin;
    max = pMax;
    timeWindow = pTimeWindow;
    defaultVehicle = pDefaultVehicle;
  }

  /**
   * Create a new scenario.
   * @param pEvents The event list.
   * @param pSupportedTypes The event types.
   * @param pMin {@link #min}.
   * @param pMax {@link #max}.
   * @param pTimeWindow {@link #timeWindow}.
   * @param pDefaultVehicle {@link #defaultVehicle}.
   */
  public FabriRechtScenario(List<? extends TimedEvent> pEvents,
    Set<Enum<?>> pSupportedTypes, Point pMin, Point pMax,
    TimeWindow pTimeWindow, VehicleDTO pDefaultVehicle) {
    super(pEvents, pSupportedTypes);
    min = pMin;
    max = pMax;
    timeWindow = pTimeWindow;
    defaultVehicle = pDefaultVehicle;
  }

  @Override
  public TimeWindow getTimeWindow() {
    return timeWindow;
  }

  @Override
  public StopConditions getStopCondition() {
    return StopConditions.TIME_OUT_EVENT;
  }

  @Override
  public ImmutableList<? extends ModelBuilder<?, ?>> getModelBuilders() {
    return ImmutableList
      .<ModelBuilder<?, ?>> builder()
      .add(
        TimeModel.builder()
          .setTickLength(1L)
          .setTimeUnit(NonSI.MINUTE)
      )
      .add(
        RoadModelBuilders.plane()
          .setMinPoint(min)
          .setMaxPoint(max)
          .setDistanceUnit(SI.KILOMETER)
          .setMaxSpeed(100d)
          .setSpeedUnit(
            SI.KILOMETRE.divide(NonSI.MINUTE).asType(Velocity.class))
      )
      .add(
        DefaultPDPModel.builder()
          .setTimeWindowPolicy(TimeWindowPolicies.TARDY_ALLOWED)
      )
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

  enum FabriRechtProblemClass implements ProblemClass {
    SINGLETON;

    @Override
    public String getId() {
      return "fabrirecht";
    }
  }
}
