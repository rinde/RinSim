/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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

import com.github.rinde.rinsim.scenario.AddParcelEvent;
import com.github.rinde.rinsim.scenario.Scenario;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

public class OrderCountRequirement implements Predicate<Scenario> {
  private final int min;
  private final int max;

  public OrderCountRequirement(int minNumOrders, int maxNumOrders) {
    checkArgument(minNumOrders <= maxNumOrders);
    min = minNumOrders;
    max = maxNumOrders;
  }

  public OrderCountRequirement(int orders) {
    this(orders, orders);
  }

  @Override
  public boolean apply(@Nullable Scenario input) {
    if (input == null) {
      return false;
    }
    final int numOrders = Collections.frequency(
        Collections2.transform(input.asList(),
            new ToClassFunction()), AddParcelEvent.class);
    System.out.println(min + " " + max + " " + numOrders);
    return numOrders >= min && numOrders <= max;
  }

  public static final class ToClassFunction implements
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
