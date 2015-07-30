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
package com.github.rinde.rinsim.core.model.time;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

/**
 *
 * @author Rinde van Lon
 */
public final class TimeUtil {

  TimeUtil() {}

  public static List<Double> interArrivalTimes(Iterable<Long> timeStamps) {
    final PeekingIterator<Long> it =
        Iterators.peekingIterator(timeStamps.iterator());
    final List<Double> interArrivalTimes = new ArrayList<>();
    for (long l1 = it.next(); it.hasNext(); l1 = it.next()) {
      final long l2 = it.peek();
      interArrivalTimes.add((l2 - l1) / 1000000d);
    }
    return interArrivalTimes;
  }

}
