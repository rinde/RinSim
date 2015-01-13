package com.github.rinde.rinsim.ui.renderers;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.road.GridRoadModel;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;

public class GridRoadModelRendererTest {
  @Test
  public void test() {
    final Simulator sim = new Simulator(new MersenneTwister(123L),
        Measure.valueOf(1000L, SI.MILLI(SI.SECOND)));

    sim.register(GridRoadModel.builder()
        .setNumXCells(5)
        .build());
    sim.configure();

    sim.register(new Agent());

    // View.create(sim)
    // .with(new GridRoadModelRenderer())
    // .show();
  }

  static class Agent implements MovingRoadUser, TickListener {

    @Override
    public void tick(TimeLapse timeLapse) {
      // TODO Auto-generated method stub

    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
      // TODO Auto-generated method stub

    }

    @Override
    public void initRoadUser(RoadModel model) {
      model.addObjectAt(this, new Point(0, 0));
    }

    @Override
    public double getSpeed() {
      return 1;
    }

  }
}
