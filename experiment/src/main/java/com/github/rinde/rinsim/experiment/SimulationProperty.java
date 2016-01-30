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
