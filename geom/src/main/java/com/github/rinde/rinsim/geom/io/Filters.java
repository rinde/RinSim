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
package com.github.rinde.rinsim.geom.io;

import static com.google.common.base.Verify.verifyNotNull;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Graph;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * Defines some {@link Predicate}s that can be useful for filtering
 * {@link Connection}s when parsing a {@link Graph} using {@link DotGraphIO}.
 * @author Rinde van Lon
 */
public final class Filters {

  private Filters() {}

  /**
   * @return A predicate that filters out self cycles, i.e. connection with the
   *         same start and end.
   */
  public static Predicate<Connection<?>> selfCycleFilter() {
    return SimpleFilters.SELF_CYCLE;
  }

  /**
   * @return No filter, everything is included.
   */
  public static Predicate<Connection<?>> noFilter() {
    return Predicates.alwaysTrue();
  }

  enum SimpleFilters implements Predicate<Connection<?>> {
    SELF_CYCLE {
      @Override
      public boolean apply(@Nullable Connection<?> input) {
        final Connection<?> in = verifyNotNull(input);
        return !in.from().equals(in.to());
      }
    }
  }
}
