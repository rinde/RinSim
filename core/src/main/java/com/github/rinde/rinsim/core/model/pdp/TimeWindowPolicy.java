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
package com.github.rinde.rinsim.core.model.pdp;

import com.github.rinde.rinsim.util.TimeWindow;

/**
 * Implementations of this interface can define a policy that says when pickups
 * and deliveries are allowed based on the specified time windows.
 * @author Rinde van Lon 
 */
public interface TimeWindowPolicy {

  /**
   * @param tw The {@link TimeWindow} to assess.
   * @param time The time.
   * @param duration The pickup duration.
   * @return <code>true</code> if with the current combination of time window,
   *         time and duration a pickup is allowed, <code>false</code>
   *         otherwise.
   */
  boolean canPickup(TimeWindow tw, long time, long duration);

  /**
   * @param tw The {@link TimeWindow} to assess.
   * @param time The time.
   * @param duration The delivery duration.
   * @return <code>true</code> if with the current combination of time window,
   *         time and duration a delivery is allowed, <code>false</code>
   *         otherwise.
   */
  boolean canDeliver(TimeWindow tw, long time, long duration);

  /**
   * Defines several default time window policies.
   * @author Rinde van Lon 
   */
  public enum TimeWindowPolicies implements TimeWindowPolicy {
    /**
     * Everything is fine, everything is going to be all right. Treats time
     * windows as a soft constraints.
     */
    LIBERAL {
      @Override
      public boolean canPickup(TimeWindow tw, long time, long duration) {
        return true;
      }

      @Override
      public boolean canDeliver(TimeWindow tw, long time, long duration) {
        return true;
      }
    },

    /**
     * Only allows pickups and deliveries which fit in the time windows, treats
     * the time windows as a hard constraint.
     */
    STRICT {
      @Override
      public boolean canPickup(TimeWindow tw, long time, long duration) {
        return tw.isIn(time);
      }

      @Override
      public boolean canDeliver(TimeWindow tw, long time, long duration) {
        return tw.isIn(time);
      }
    },

    /**
     * Being tardy (late) is allowed, being early is NOT! Earliness is a hard
     * constraint, tardiness is a soft constraint.
     */
    TARDY_ALLOWED {
      @Override
      public boolean canPickup(TimeWindow tw, long time, long duration) {
        return tw.isAfterStart(time);
      }

      @Override
      public boolean canDeliver(TimeWindow tw, long time, long duration) {
        return tw.isAfterStart(time);
      }
    };
  }
}
