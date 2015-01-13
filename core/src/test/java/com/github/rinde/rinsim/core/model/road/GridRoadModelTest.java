package com.github.rinde.rinsim.core.model.road;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;

public class GridRoadModelTest {
  @Test
  public void test() {
    final Simulator sim = new Simulator(new MersenneTwister(123L),
        Measure.valueOf(1000L, SI.MILLI(SI.SECOND)));

    sim.register(GridRoadModel.builder().build());

  }
}
