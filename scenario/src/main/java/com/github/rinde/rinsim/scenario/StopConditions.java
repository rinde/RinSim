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

import java.io.Serializable;

import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.google.auto.value.AutoValue;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

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
  public static StopCondition limitedTime(long endTime) {
    return LimitedTime.create(endTime);
  }

  public static StopCondition alwaysTrue() {
    return Default.ALWAYS_TRUE;
  }

  public static StopCondition alwaysFalse() {
    return Default.ALWAYS_FALSE;
  }

  public static StopCondition and(StopCondition... conditions) {
    return And.create(ImmutableSet.copyOf(conditions));
  }

  public static StopCondition or(StopCondition... conditions) {
    return Or.create(ImmutableSet.copyOf(conditions));
  }

  abstract static class CompositeStopCondition implements StopCondition {

    abstract ImmutableSet<StopCondition> stopConditions();

    @Override
    public abstract boolean evaluate(TypeProvider provider);

    static ImmutableSet<Class<?>> getTypes(Iterable<StopCondition> cnds) {
      ImmutableSet.Builder<Class<?>> types = ImmutableSet.builder();
      for (StopCondition sc : cnds) {
        types.addAll(sc.getTypes());
      }
      return types.build();
    }
  }

  @AutoValue
  abstract static class And extends CompositeStopCondition {
    @Override
    public boolean evaluate(TypeProvider provider) {
      for (StopCondition sc : stopConditions()) {
        if (!sc.evaluate(provider)) {
          return false;
        }
      }
      return true;
    }

    static And create(ImmutableSet<StopCondition> scs) {
      return new AutoValue_StopConditions_And(getTypes(scs), scs);
    }
  }

  @AutoValue
  abstract static class Or extends CompositeStopCondition {
    @Override
    public boolean evaluate(TypeProvider provider) {
      for (StopCondition sc : stopConditions()) {
        if (sc.evaluate(provider)) {
          return true;
        }
      }
      return false;
    }

    static Or create(ImmutableSet<StopCondition> scs) {
      return new AutoValue_StopConditions_Or(getTypes(scs), scs);
    }
  }

  /**
   * Adapts the specified {@link Predicate} such that it can be used as a stop
   * condition.
   * @param type The type the {@link Predicate} accepts.
   * @param pred The predicate that defines the stop condition.
   * @return The adapted stop condition.
   */
  // public static <T> StopConditionBuilder adapt(Class<T> type, Predicate<T>
  // pred) {
  // return SingleBuilder.create(type, pred);
  // }

  @AutoValue
  abstract static class LimitedTime implements StopCondition, Serializable {
    private static final long serialVersionUID = 8074891223968917862L;

    abstract long endTime();

    @Override
    public boolean evaluate(TypeProvider provider) {
      return provider.get(Clock.class).getCurrentTime() >= endTime();
    }

    static LimitedTime create(long endTime) {
      return new AutoValue_StopConditions_LimitedTime(
        ImmutableSet.<Class<?>> of(Clock.class), endTime);
    }
  }

  enum Default implements StopCondition {
    ALWAYS_TRUE {
      @Override
      public boolean evaluate(TypeProvider provider) {
        return true;
      }
    },
    ALWAYS_FALSE {
      @Override
      public boolean evaluate(TypeProvider provider) {
        return false;
      }
    };
    @Override
    public ImmutableSet<Class<?>> getTypes() {
      return ImmutableSet.of();
    }
  }

  // @AutoValue
  // abstract static class SingleBuilder<T> implements StopConditionBuilder {
  //
  // final Class<PredicateStopCondition<T>> modelType;
  //
  // @SuppressWarnings({ "serial", "unchecked" })
  // SingleBuilder() {
  // modelType = (Class<PredicateStopCondition<T>>)
  // new TypeToken<PredicateStopCondition<T>>(getClass()) {}.getRawType();
  // }
  //
  // abstract Class<T> dependencyType();
  //
  // abstract Predicate<T> predicate();
  //
  // @Override
  // public PredicateStopCondition<T> build(DependencyProvider
  // dependencyProvider) {
  // T dependency = dependencyProvider.get(dependencyType());
  // return new PredicateStopCondition<>(dependency, predicate());
  // }
  //
  // @Override
  // public Class<Void> getAssociatedType() {
  // return Void.class;
  // }
  //
  // @Override
  // public Class<StopCondition> getModelType() {
  // return StopCondition.class;
  // }
  //
  // @Override
  // public ImmutableSet<Class<?>> getProvidingTypes() {
  // return ImmutableSet.of();
  // }
  //
  // @Override
  // public ImmutableSet<Class<?>> getDependencies() {
  // return ImmutableSet.<Class<?>> of(dependencyType());
  // }
  //
  // static <D> SingleBuilder<D> create(Class<D> type, Predicate<D> p) {
  // return new AutoValue_StopConditions_SingleBuilder<>(type, p);
  // }
  // }
  //
  // static class PredicateStopCondition<T> extends AbstractModelVoid implements
  // StopCondition {
  // private final T subject;
  // private final Predicate<T> predicate;
  //
  // PredicateStopCondition(T s, Predicate<T> p) {
  // subject = s;
  // predicate = p;
  // }
  //
  // @Override
  // public boolean evaluate() {
  // return predicate.apply(subject);
  // }
  // }
}
