/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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
package com.github.rinde.rinsim.experiment;

import java.util.Set;

import com.github.rinde.rinsim.scenario.Scenario;

public enum SimulationProperty {
  CONFIG {
    @Override
    Set<? extends Object> select(
        Set<MASConfiguration> c,
        Set<Scenario> scen,
        Set<Long> seeds,
        Set<Integer> reps) {
      return c;
    }
  },
  SCENARIO {
    @Override
    Set<? extends Object> select(
        Set<MASConfiguration> c,
        Set<Scenario> scen,
        Set<Long> seeds,
        Set<Integer> reps) {
      return scen;
    }
  },
  REPS {
    @Override
    Set<? extends Object> select(
        Set<MASConfiguration> c,
        Set<Scenario> scen,
        Set<Long> seeds,
        Set<Integer> reps) {
      return seeds;
    }
  },
  SEED_REPS {
    @Override
    Set<? extends Object> select(
        Set<MASConfiguration> c,
        Set<Scenario> scen,
        Set<Long> seeds,
        Set<Integer> reps) {
      return reps;
    }
  };

  abstract Set<? extends Object> select(
      Set<MASConfiguration> c,
      Set<Scenario> scen,
      Set<Long> seeds,
      Set<Integer> reps);
}
