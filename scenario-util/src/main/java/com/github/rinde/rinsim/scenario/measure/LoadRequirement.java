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
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.math3.stat.StatUtils;

import com.github.rinde.rinsim.scenario.Scenario;
import com.google.common.annotations.Beta;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;

/**
 * A {@link Predicate} that evaluates to <code>true</code> if the load graph of
 * a scenario is within the range as described by this predicate.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
@Beta
class LoadRequirement implements Predicate<Scenario> {
  private final ImmutableList<Double> desiredLoadList;

  private final double maxMean;
  private final double maxMax;
  private final boolean relative;

  /**
   * Creates a new instance.
   * @param desiredLoad The desired load graph.
   * @param maxMeanDeviation The maximum mean deviation to the desired load
   *          graph that is allowed for the input scenario.
   * @param maxMaxDeviation The maximum max deviation to the desired load graph
   *          that
   * @param relativeLoad If <code>true</code>
   *          {@link Metrics#measureRelativeLoad(Scenario)} is used, otherwise
   *          {@link Metrics#measureLoad(Scenario)} is used.
   */
  LoadRequirement(ImmutableList<Double> desiredLoad,
      double maxMeanDeviation, double maxMaxDeviation, boolean relativeLoad) {
    desiredLoadList = desiredLoad;
    maxMean = maxMeanDeviation;
    maxMax = maxMaxDeviation;
    relative = relativeLoad;
  }

  @Override
  public boolean apply(@Nullable Scenario scenario) {
    assert scenario != null;
    final List<Double> loads = newArrayList(relative ? Metrics
        .measureRelativeLoad(scenario) : Metrics.measureLoad(scenario));
    final int toAdd = desiredLoadList.size() - loads.size();
    for (int j = 0; j < toAdd; j++) {
      loads.add(0d);
    }

    final double[] deviations = abs(subtract(
        Doubles.toArray(desiredLoadList),
        Doubles.toArray(loads)));
    final double mean = StatUtils.mean(deviations);
    final double max = Doubles.max(deviations);
    return max <= maxMax && mean <= maxMean;
  }

  static double[] subtract(double[] arr1, double[] arr2) {
    checkArgument(arr1.length == arr2.length);
    final double[] res = new double[arr1.length];
    for (int i = 0; i < arr1.length; i++) {
      res[i] = arr1[i] - arr2[i];
    }
    return res;
  }

  static double[] abs(double[] arr) {
    final double[] res = new double[arr.length];
    for (int i = 0; i < arr.length; i++) {
      res[i] = Math.abs(arr[i]);
    }
    return res;
  }
}
