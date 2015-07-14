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
package com.github.rinde.rinsim.scenario;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verifyNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.core.model.time.TimeModel.AbstractBuilder;
import com.github.rinde.rinsim.core.model.time.TimeModel.RealtimeBuilder;
import com.google.common.base.Function;

/**
 * Provides utilities for converting {@link Scenario} instances.
 * @author Rinde van Lon
 */
public final class ScenarioConverters {
  private ScenarioConverters() {}

  /**
   * Returns a function that converts {@link Scenario} instances to real-time
   * versions. If a builder for {@link TimeModel} is present in the scenario, it
   * is replaced by a real-time version. If no builder for {@link TimeModel} is
   * present a real-time builder is added.
   * @return A scenario converter function.
   */
  public static Function<Scenario, Scenario> toRealtime() {
    return ToRealTimeConverter.INSTANCE;
  }

  enum ToRealTimeConverter implements Function<Scenario, Scenario> {
    INSTANCE {
      @Override
      @Nullable
      public Scenario apply(@Nullable Scenario input) {
        final Scenario in = verifyNotNull(input);
        final List<TimeModel.AbstractBuilder<?>> timeModels = new ArrayList<>();
        for (final ModelBuilder<?, ?> mb : in.getModelBuilders()) {
          if (mb instanceof TimeModel.AbstractBuilder) {
            timeModels.add((AbstractBuilder<?>) mb);
          }
        }
        RealtimeBuilder rtb = TimeModel.builder()
            .withRealTime()
            .withStartInClockMode(ClockMode.SIMULATED);
        if (timeModels.size() == 1) {
          // copy properties from existing time model
          rtb = rtb.withTickLength(timeModels.get(0).getTickLength())
              .withTimeUnit(timeModels.get(0).getTimeUnit());
        } else {
          // in this case we don't copy properties, we use the defaults
          checkArgument(timeModels.isEmpty(),
              "More than one time model is not supported.");
        }
        return Scenario.builder(in)
            .removeModelsOfType(TimeModel.AbstractBuilder.class)
            .addModel(rtb)
            .build();
      }

      @Override
      public String toString() {
        return String.format("%s.toRealtime()",
            ScenarioConverters.class.getSimpleName());
      }
    }
  }
}
