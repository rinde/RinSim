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

import static com.google.common.base.Verify.verifyNotNull;

import java.io.Serializable;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.scenario.StopCondition.StopConditionBuilder;
import com.google.auto.value.AutoValue;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;

/**
 * Utility class for creating {@link StopCondition} instances.
 * @author Rinde van Lon
 */
public final class StopConditions {

  private StopConditions() {}

  /**
   * Returns a {@link ModelBuilder} that constructs a {@link StopCondition} that
   * will stop the simulation at the first possible moment after
   * <code>endTime</code>. The simulation time after stopping is
   * <code>endTime+1</code>.
   * @param endTime The end time.
   * @return A {@link ModelBuilder}.
   */
  public static StopConditionBuilder limitedTime(long endTime) {
    return adapt(Clock.class, LimitedTime.create(endTime));
  }

  public static StopConditionBuilder alwaysTrue() {
    return adapt(Clock.class, Predicates.<Clock> alwaysTrue());
  }

  public static StopConditionBuilder alwaysFalse() {
    return adapt(Clock.class, Predicates.<Clock> alwaysFalse());
  }

  /**
   * Adapts the specified {@link Predicate} such that it can be used as a stop
   * condition.
   * @param type The type the {@link Predicate} accepts.
   * @param pred The predicate that defines the stop condition.
   * @return The adapted stop condition.
   */
  public static <T> StopConditionBuilder adapt(Class<T> type, Predicate<T> pred) {
    return SingleBuilder.create(type, pred);
  }

  @AutoValue
  abstract static class LimitedTime implements Predicate<Clock>, Serializable {
    private static final long serialVersionUID = 8074891223968917862L;

    abstract long endTime();

    @Override
    public boolean apply(@Nullable Clock input) {
      return verifyNotNull(input).getCurrentTime() >= endTime();
    }

    static LimitedTime create(long endTime) {
      return new AutoValue_StopConditions_LimitedTime(endTime);
    }
  }

  @AutoValue
  abstract static class SingleBuilder<T> implements StopConditionBuilder {

    final Class<PredicateStopCondition<T>> modelType;

    @SuppressWarnings({ "serial", "unchecked" })
    SingleBuilder() {
      modelType = (Class<PredicateStopCondition<T>>)
        new TypeToken<PredicateStopCondition<T>>(getClass()) {}.getRawType();
    }

    abstract Class<T> dependencyType();

    abstract Predicate<T> predicate();

    @Override
    public PredicateStopCondition<T> build(DependencyProvider dependencyProvider) {
      T dependency = dependencyProvider.get(dependencyType());
      return new PredicateStopCondition<>(dependency, predicate());
    }

    @Override
    public Class<Void> getAssociatedType() {
      return Void.class;
    }

    @Override
    public Class<StopCondition> getModelType() {
      return StopCondition.class;
    }

    @Override
    public ImmutableSet<Class<?>> getProvidingTypes() {
      return ImmutableSet.of();
    }

    @Override
    public ImmutableSet<Class<?>> getDependencies() {
      return ImmutableSet.<Class<?>> of(dependencyType());
    }

    static <D> SingleBuilder<D> create(Class<D> type, Predicate<D> p) {
      return new AutoValue_StopConditions_SingleBuilder<>(type, p);
    }
  }

  static class PredicateStopCondition<T> extends AbstractModelVoid implements
    StopCondition {
    private final T subject;
    private final Predicate<T> predicate;

    PredicateStopCondition(T s, Predicate<T> p) {
      subject = s;
      predicate = p;
    }

    @Override
    public boolean evaluate() {
      return predicate.apply(subject);
    }
  }
}
