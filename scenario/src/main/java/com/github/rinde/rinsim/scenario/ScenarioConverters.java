/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.core.model.time.TimeModel.AbstractBuilder;
import com.github.rinde.rinsim.core.model.time.TimeModel.RealtimeBuilder;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

/**
 * Provides utilities for converting {@link Scenario} instances.
 * @author Rinde van Lon
 */
public final class ScenarioConverters {
  private ScenarioConverters() {}

  /**
   * Returns a function that converts {@link Scenario} instances by removing any
   * existing {@link TimeModel} instances and replaces it with the specified
   * instance.
   * @param timeModel The time model to use.
   * @return A new converter function.
   */
  public static Function<Scenario, Scenario> useTimeModel(
      final TimeModel.AbstractBuilder<?> timeModel) {
    return new Function<Scenario, Scenario>() {
      @Override
      @Nullable
      public Scenario apply(@Nullable Scenario input) {
        return Scenario.builder(verifyNotNull(input))
          .removeModelsOfType(TimeModel.AbstractBuilder.class)
          .addModel(timeModel)
          .build();
      }
    };
  }

  /**
   * Returns a function that converts {@link Scenario} instances to real-time
   * versions. If a builder for {@link TimeModel} is present in the scenario, it
   * is replaced by a real-time version. If no builder for {@link TimeModel} is
   * present a real-time builder is added.
   * @return A scenario converter function.
   */
  public static Function<Scenario, Scenario> toRealtime() {
    return TimeModelConverter.TO_RT;
  }

  /**
   * Returns a function that converts {@link Scenario} instances to simulated
   * time versions. If a builder for {@link TimeModel} is present in the
   * scenario, it is replaced by a simulated time version. If no builder for
   * {@link TimeModel} is present a simulated time builder is added.
   * @return A scenario converter function.
   */
  public static Function<Scenario, Scenario> toSimulatedtime() {
    return TimeModelConverter.TO_ST;
  }

  /**
   * Adapts a function that converts {@link TimedEvent} instances to a function
   * that converts entire {@link Scenario} instances.
   * @param converter The converter to adapt.
   * @return The adapted converter.
   */
  public static Function<Scenario, Scenario> eventConverter(
      final Function<TimedEvent, TimedEvent> converter) {
    return new Function<Scenario, Scenario>() {
      @Nonnull
      @Override
      public Scenario apply(@Nullable Scenario input) {
        final Scenario in = verifyNotNull(input);
        return Scenario.builder(in)
          .clearEvents()
          .addEvents(
            FluentIterable.from(in.getEvents()).transform(converter))
          .build();
      }
    };
  }

  enum TimeModelConverter implements Function<Scenario, Scenario> {
    TO_RT {
      @Override
      @Nullable
      public Scenario apply(@Nullable Scenario input) {
        final Scenario in = verifyNotNull(input);

        final Optional<TimeModel.AbstractBuilder<?>> timeModel =
          getTimeModel(in);

        RealtimeBuilder rtb = TimeModel.builder()
          .withRealTime()
          .withStartInClockMode(ClockMode.SIMULATED);
        if (timeModel.isPresent()) {
          // copy properties from existing time model
          rtb = rtb.withTickLength(timeModel.get().getTickLength())
            .withTimeUnit(timeModel.get().getTimeUnit());
        }
        // else: in this case we don't copy properties, we use the defaults
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
    },

    TO_ST {
      @Nullable
      @Override
      public Scenario apply(@Nullable Scenario input) {
        final Scenario in = verifyNotNull(input);
        final Optional<TimeModel.AbstractBuilder<?>> timeModel =
          getTimeModel(in);

        final TimeModel.Builder rtb = TimeModel.builder();
        if (timeModel.isPresent()) {
          rtb.withTickLength(timeModel.get().getTickLength())
            .withTimeUnit(timeModel.get().getTimeUnit());
        }

        return Scenario.builder(in)
          .removeModelsOfType(TimeModel.AbstractBuilder.class)
          .addModel(rtb)
          .build();
      }

      @Override
      public String toString() {
        return String.format("%s.toSimulatedtime()",
          ScenarioConverters.class.getSimpleName());
      }

    }
  }

  static Optional<TimeModel.AbstractBuilder<?>> getTimeModel(Scenario scen) {
    final List<TimeModel.AbstractBuilder<?>> timeModels = new ArrayList<>();
    for (final ModelBuilder<?, ?> mb : scen.getModelBuilders()) {
      if (mb instanceof TimeModel.AbstractBuilder) {
        timeModels.add((AbstractBuilder<?>) mb);
      }
    }
    if (timeModels.isEmpty()) {
      return Optional.absent();
    }
    checkArgument(timeModels.size() == 1,
      "More than one time model is not supported.");
    return Optional.<TimeModel.AbstractBuilder<?>>of(timeModels.get(0));
  }
}
