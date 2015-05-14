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

import javax.annotation.Nullable;

import com.github.rinde.rinsim.scenario.StopCondition.StopConditionBuilder;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * This class contains default stop conditions that require a
 * {@link StatisticsProvider} in the simulator.
 * @author Rinde van Lon
 */
public enum StatsStopConditions implements Predicate<StatisticsProvider> {

  /**
   * The simulation is terminated once the
   * {@link com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent#TIME_OUT}
   * event is dispatched.
   */
  TIME_OUT_EVENT {
    @Override
    public boolean apply(@Nullable StatisticsProvider context) {
      assert context != null;
      return context.getStatistics().simFinish;
    }
  },

  /**
   * The simulation is terminated as soon as all the vehicles are back at the
   * depot, note that this can be before or after the
   * {@link com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent#TIME_OUT}
   * event is dispatched.
   */
  VEHICLES_DONE_AND_BACK_AT_DEPOT {
    @Override
    public boolean apply(@Nullable StatisticsProvider context) {
      assert context != null;
      final StatisticsDTO stats = context.getStatistics();

      return stats.totalVehicles == stats.vehiclesAtDepot
        && stats.movedVehicles > 0
        && stats.totalParcels == stats.totalDeliveries;
    }
  },

  /**
   * The simulation is terminated as soon as any tardiness occurs.
   */
  ANY_TARDINESS {
    @Override
    public boolean apply(@Nullable StatisticsProvider context) {
      assert context != null;
      final StatisticsDTO stats = context.getStatistics();
      return stats.pickupTardiness > 0
        || stats.deliveryTardiness > 0;
    }
  };

  @SafeVarargs
  public static StopConditionBuilder and(
    Predicate<StatisticsProvider>... conditions) {
    return adapt(Predicates.and(conditions));
  }

  public static StopConditionBuilder adapt(
    Predicate<StatisticsProvider> condition) {
    return StopConditions.adapt(StatisticsProvider.class, condition);
  }
}
