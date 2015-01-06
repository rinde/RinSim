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
package com.github.rinde.rinsim.experiment.base;

import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;

public final class SimArgs implements Comparable<SimArgs> {
  final Scenario scenario;
  final Configuration configuration;
  final long randomSeed;
  final boolean showGui;
  final Optional<? extends PostProcessor<?, ?>> postProcessor;

  // final Optional<UICreator> uiCreator;

  SimArgs(Scenario s, Configuration m, long seed,
      boolean gui, @Nullable PostProcessor<?, ?> pp
  /* , @Nullable UICreator uic */) {
    scenario = s;
    configuration = m;
    randomSeed = seed;
    showGui = gui;
    postProcessor = Optional.fromNullable(pp);
    // uiCreator = Optional.fromNullable(uic);
  }

  @Override
  public String toString() {
    return Joiner.on(",").join(scenario.getClass().getName(),
        scenario.getProblemClass(),
        scenario.getProblemInstanceId(), configuration, randomSeed,
        showGui, postProcessor);
  }

  public Scenario getScenario() {
    return scenario;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public long getRandomSeed() {
    return randomSeed;
  }

  public boolean isShowGui() {
    return showGui;
  }

  public Optional<? extends PostProcessor<?, ?>> getPostProcessor() {
    return postProcessor;
  }

  @Override
  public int compareTo(@Nullable SimArgs o) {
    Objects.requireNonNull(o);
    return ComparisonChain.start()
        .compare(scenario, o.scenario)
        .compare(configuration, o.configuration)
        .compare(randomSeed, o.randomSeed)
        .result();
  }
}
