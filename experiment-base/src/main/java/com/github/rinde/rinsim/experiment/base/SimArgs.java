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
import javax.annotation.Nullable;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;

class SimArgs {
  final Scenario scenario;
  final Configuration masConfig;
  final long randomSeed;
  final Object objectiveFunction;
  final boolean showGui;
  final Optional<? extends PostProcessor<?, ?>> postProcessor;

  // final Optional<UICreator> uiCreator;

  SimArgs(Scenario s, Configuration m, long seed,
      Object obj, boolean gui, @Nullable PostProcessor<?, ?> pp
      /* , @Nullable UICreator uic */) {
    scenario = s;
    masConfig = m;
    randomSeed = seed;
    objectiveFunction = obj;
    showGui = gui;
    postProcessor = Optional.fromNullable(pp);
    // uiCreator = Optional.fromNullable(uic);
  }

  @Override
  public String toString() {
    return Joiner.on(",").join(scenario.getClass().getName(),
        scenario /* .getProblemClass() */,
        scenario/* .getProblemInstanceId() */, masConfig, randomSeed,
        objectiveFunction, showGui, postProcessor);
  }
}
