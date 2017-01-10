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

import static java.util.Arrays.asList;

import java.io.Serializable;

import com.github.rinde.rinsim.core.model.time.Clock;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

/**
 * Utility class containing default {@link StopCondition} instances and methods
 * to create more complex conditions. The following methods accept one or more
 * {@link StopCondition}s:
 * <ul>
 * <li>{@link #and(StopCondition, StopCondition, StopCondition...)}</li>
 * <li>{@link #or(StopCondition, StopCondition, StopCondition...)}</li>
 * <li>{@link #not(StopCondition)}</li>
 * </ul>
 * @author Rinde van Lon
 */
public final class StopConditions {

  private StopConditions() {}

  /**
   * Returns a {@link StopCondition} that will stop the simulation at the first
   * possible moment after <code>endTime</code>. The simulation time after
   * stopping is <code>endTime+1 tick</code>.
   * @param endTime The end time.
   * @return A {@link StopCondition}.
   */
  public static StopCondition limitedTime(long endTime) {
    return LimitedTime.create(endTime);
  }

  /**
   * @return A {@link StopCondition} that will stop the simulation immediately.
   */
  public static StopCondition alwaysTrue() {
    return Default.ALWAYS_TRUE;
  }

  /**
   * @return A {@link StopCondition} that will never stop the simulation.
   */
  public static StopCondition alwaysFalse() {
    return Default.ALWAYS_FALSE;
  }

  /**
   * Combines the specified {@link StopCondition}s into a single
   * {@link StopCondition} using the logical AND operator. The returned
   * condition will return <code>true</code> if <i>all</i> specified conditions
   * return <code>true</code>.
   * @param condition1 The first condition.
   * @param condition2 The second condition.
   * @param more More conditions (optional).
   * @return A new {@link StopCondition}.
   */
  public static StopCondition and(StopCondition condition1,
      StopCondition condition2, StopCondition... more) {
    return And.create(
      ImmutableSet.<StopCondition>builder()
        .add(condition1)
        .add(condition2)
        .addAll(asList(more))
        .build());
  }

  /**
   * Combines the specified {@link StopCondition}s into a single
   * {@link StopCondition} using the logical OR operator. The returned condition
   * will return <code>true</code> if <i>any</i> of the specified conditions
   * return <code>true</code>.
   * @param condition1 The first condition.
   * @param condition2 The second condition.
   * @param more More conditions (optional).
   * @return A new {@link StopCondition}.
   */
  public static StopCondition or(StopCondition condition1,
      StopCondition condition2, StopCondition... more) {
    return Or.create(
      ImmutableSet.<StopCondition>builder()
        .add(condition1)
        .add(condition2)
        .addAll(asList(more))
        .build());
  }

  /**
   * Creates a new {@link StopCondition} that returns <code>true</code> when the
   * specified condition returns <code>false</code> and vice versa.
   * @param condition The condition.
   * @return The negated {@link StopCondition}.
   */
  public static StopCondition not(StopCondition condition) {
    return Not.create(condition);
  }

  abstract static class CompositeStopCondition implements StopCondition {

    abstract ImmutableSet<StopCondition> stopConditions();

    @Override
    public abstract boolean evaluate(TypeProvider provider);

    static ImmutableSet<Class<?>> getTypes(Iterable<StopCondition> cnds) {
      final ImmutableSet.Builder<Class<?>> types = ImmutableSet.builder();
      for (final StopCondition sc : cnds) {
        types.addAll(sc.getTypes());
      }
      return types.build();
    }
  }

  @AutoValue
  abstract static class And extends CompositeStopCondition {
    @Override
    public boolean evaluate(TypeProvider provider) {
      for (final StopCondition sc : stopConditions()) {
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
      for (final StopCondition sc : stopConditions()) {
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

  @AutoValue
  abstract static class Not implements StopCondition {
    abstract StopCondition delegate();

    @Override
    public boolean evaluate(TypeProvider provider) {
      return !delegate().evaluate(provider);
    }

    static Not create(StopCondition sc) {
      return new AutoValue_StopConditions_Not(sc.getTypes(), sc);
    }
  }

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
        ImmutableSet.<Class<?>>of(Clock.class), endTime);
    }
  }

  enum Default implements StopCondition {
    ALWAYS_TRUE {
      @Override
      public boolean evaluate(TypeProvider provider) {
        return true;
      }

      @Override
      public String toString() {
        return StopConditions.class.getSimpleName() + ".alwaysTrue()";
      }
    },
    ALWAYS_FALSE {
      @Override
      public boolean evaluate(TypeProvider provider) {
        return false;
      }

      @Override
      public String toString() {
        return StopConditions.class.getSimpleName() + ".alwaysFalse()";
      }
    };
    @Override
    public ImmutableSet<Class<?>> getTypes() {
      return ImmutableSet.of();
    }
  }
}
