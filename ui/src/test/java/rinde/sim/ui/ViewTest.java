package rinde.sim.ui;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Test;

import rinde.sim.core.Simulator;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.ui.renderers.PlaneRoadModelRenderer;
import rinde.sim.ui.renderers.TestRenderer;

public class ViewTest {

  /**
   * Simple GUI test.
   */
  @Test
  public void testRenderer() {
    final Simulator sim = new Simulator(new MersenneTwister(123),
        Measure.valueOf(1000L, SI.MILLI(SI.SECOND)));
    sim.register(new PlaneRoadModel(new Point(0, 0), new Point(10, 10), 50));
    sim.configure();
    sim.addTickListener(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        if (timeLapse.getTime() >= 10000) {
          sim.stop();
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });

    View.create(sim)
        .setTitleAppendix("ViewTest")
        .enableAutoClose()
        .enableAutoPlay()
        .with(new PlaneRoadModelRenderer())
        .with(new TestRenderer())
        .show();

  }

  /**
   * Tests a view with a simulator that is not configured.
   */
  @Test(expected = IllegalStateException.class)
  public void failNotConfiguredSim() {
    final Simulator sim = new Simulator(new MersenneTwister(123),
        Measure.valueOf(1000L, SI.MILLI(SI.SECOND)));
    View.create(sim).show();
  }
}
