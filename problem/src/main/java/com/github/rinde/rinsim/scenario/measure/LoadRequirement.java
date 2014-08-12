package com.github.rinde.rinsim.scenario.measure;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.math3.stat.StatUtils;

import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.generator.Metrics;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;

public class LoadRequirement implements Predicate<Scenario> {
  private final ImmutableList<Double> desiredLoadList;

  private final double maxMean;
  private final double maxMax;
  private final boolean relative;

  public LoadRequirement(ImmutableList<Double> desiredLoad,
      double maxMeanDeviation, double maxMaxDeviation, boolean relativeLoad) {
    desiredLoadList = desiredLoad;
    maxMean = maxMeanDeviation;
    maxMax = maxMaxDeviation;
    relative = relativeLoad;
  }

  @Override
  public boolean apply(@Nullable Scenario scenario) {
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
