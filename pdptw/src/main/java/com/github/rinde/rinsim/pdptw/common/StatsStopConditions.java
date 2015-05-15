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

import com.github.rinde.rinsim.scenario.StopCondition;
import com.google.common.collect.ImmutableSet;

/**
 * This class contains default stop conditions that require a
 * {@link StatisticsProvider} in the simulator.
 * @author Rinde van Lon
 */
public final class StatsStopConditions {
  /**
   * The simulation is terminated once the
   * {@link com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent#TIME_OUT}
   * event is dispatched.
   * @return A {@link StopCondition}.
   */
  public static StopCondition timeOutEvent() {
    return Instances.TIME_OUT_EVENT;
  }

  /**
   * The simulation is terminated as soon as all the vehicles are back at the
   * depot, note that this can be before or after the
   * {@link com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent#TIME_OUT}
   * event is dispatched.
   * @return A {@link StopCondition}.
   */
  public static StopCondition vehiclesDoneAndBackAtDepot() {
    return Instances.VEHICLES_DONE_AND_BACK_AT_DEPOT;
  }

  /**
   * The simulation is terminated as soon as any tardiness occurs.
   * @return A {@link StopCondition}.
   */
  public static StopCondition anyTardiness() {
    return Instances.ANY_TARDINESS;
  }

  enum Instances implements StopCondition {
    TIME_OUT_EVENT {
      @Override
      public boolean evaluate(TypeProvider provider) {
        return provider.get(StatisticsProvider.class).getStatistics().simFinish;
      }
    },
    VEHICLES_DONE_AND_BACK_AT_DEPOT {
      @Override
      public boolean evaluate(TypeProvider provider) {
        final StatisticsDTO stats = provider.get(StatisticsProvider.class)
          .getStatistics();

        return stats.totalVehicles == stats.vehiclesAtDepot
          && stats.movedVehicles > 0
          && stats.totalParcels == stats.totalDeliveries;
      }
    },
    ANY_TARDINESS {
      @Override
      public boolean evaluate(TypeProvider provider) {
        final StatisticsDTO stats = provider.get(StatisticsProvider.class)
          .getStatistics();
        return stats.pickupTardiness > 0
          || stats.deliveryTardiness > 0;
      }
    };

    @Override
    public ImmutableSet<Class<?>> getTypes() {
      return ImmutableSet.<Class<?>> of(StatisticsProvider.class);
    }
  }
}
