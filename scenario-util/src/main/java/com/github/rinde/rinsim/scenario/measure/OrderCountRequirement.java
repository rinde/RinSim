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
package com.github.rinde.rinsim.scenario.measure;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collections;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.scenario.Scenario;
import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * A {@link Predicate} that evaluates to <code>true</code> if the input scenario
 * has a number of orders in the range as specified by this instance. An order
 * is specified by a {@link AddParcelEvent}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
@Beta
class OrderCountRequirement implements Predicate<Scenario> {
  private final int min;
  private final int max;

  /**
   * Range constructor, only accepts scenarios that have a number of orders that
   * is in the range <code>[minNumOrders,maxNumOrders]</code>.
   * @param minNumOrders Minimum number of orders.
   * @param maxNumOrders Maximum number of orders.
   */
  OrderCountRequirement(int minNumOrders, int maxNumOrders) {
    checkArgument(minNumOrders <= maxNumOrders);
    min = minNumOrders;
    max = maxNumOrders;
  }

  /**
   * Exact constructor, only accepts scenarios that have exactly the specified
   * number of orders.
   * @param orders The number of orders.
   */
  OrderCountRequirement(int orders) {
    this(orders, orders);
  }

  @Override
  public boolean apply(@Nullable Scenario input) {
    if (input == null) {
      return false;
    }
    final int numOrders = Collections.frequency(
        Collections2.transform(input.getEvents(),
            new ToClassFunction()), AddParcelEvent.class);
    return numOrders >= min && numOrders <= max;
  }

  private static final class ToClassFunction implements
      Function<Object, Class<?>> {
    @Override
    @Nullable
    public Class<?> apply(@Nullable Object obj) {
      if (obj == null) {
        return null;
      }
      return obj.getClass();
    }
  }
}
